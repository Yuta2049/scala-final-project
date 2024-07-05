package com.finalproject

import canoe.api.TelegramClient
import cats.effect.{ExitCode, IO, IOApp, Resource}
//import cats.implicits.*
import com.finalproject.repository.DbConnection
import com.finalproject.services.{NotificationsSender, QuotesLoader, QuotesService, UserService}
import izumi.logstage.api.IzLogger
import logstage.LogIO
import org.typelevel.otel4s.trace.Tracer
import fly4s.Fly4s
import fly4s.data.{Fly4sConfig, Locations, MigrateResult, ValidatePattern}
import fly4s.implicits.*


object Main extends IOApp {

  given Tracer[IO] = Tracer.noop[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    def migrateDb(dbConfig: DbConfig): Resource[IO, MigrateResult] =
      Fly4s.make[IO](
        url = dbConfig.url,
        user = Option(dbConfig.user),
        password = Option(dbConfig.password.toCharArray),
        config = Fly4sConfig(
          table = dbConfig.migrationsTable,
          locations = Locations(dbConfig.migrationsLocations),
          ignoreMigrationPatterns = List(ValidatePattern.ignorePendingMigrations),
          baselineOnMigrate = true
        )
      ).evalMap(_.validateAndMigrate.result)

    val createLogger = IO {
      val logger = IzLogger()
      LogIO.fromLogger[IO](logger)
    }

    val resources = for {
      config <- Config.load.toResource
      log <- createLogger.toResource
      _ <- migrateDb(config.database)

      pooledSession <- DbConnection.pooled[IO](config)

      telegramClient <- TelegramClient[IO](config.botConfig.apiKey)

      userService <- pooledSession.use { session =>
        for {
          userService <- UserService.make(using session, log)
        } yield userService
      }.toResource

      quotesService <- pooledSession.use { session =>
        for {
          quotesService <- QuotesService.make(using session, log)
        } yield quotesService
      }.toResource
      
      quotesLoader <- pooledSession.use { session =>
        for {
          quotesLoader <- QuotesLoader.make(using config, session, log)
        } yield quotesLoader
      }.toResource

      notificationsSender <- NotificationsSender.make(using config, log, quotesService, telegramClient).toResource

    } yield (config, log, userService, quotesLoader, notificationsSender, quotesService)

    resources.use {
      case (config, log, userService, quotesLoader, notificationsSender, quotesService) => {
        for {          
          telegramFiber <- Telegram.run(using config, log, userService, quotesService).start
          quotesLoaderFiber <- quotesLoader.loadQuotesSchedule.start
          notificationsSenderFiber <- notificationsSender.sendNotificationsSchedule.start
          _ <- telegramFiber.join
          _ <- quotesLoaderFiber.join
          _ <- notificationsSenderFiber.join
        } yield ()
      }.as(ExitCode.Success)
    }
  }
}
