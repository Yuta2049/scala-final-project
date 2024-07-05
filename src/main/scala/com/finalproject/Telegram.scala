package com.finalproject

import cats.effect.unsafe.implicits.global
import com.finalproject.services.{QuotesService, UserService}
import canoe.api.*
import canoe.methods.messages.SendMessage
import canoe.models.messages.TextMessage
import canoe.syntax.*
import cats.effect.IO
import fs2.Stream
import logstage.{Log, LogIO}

object Telegram {

  def run(using config: Config, log: LogIO[IO], userService: UserService, quotesService: QuotesService): IO[Unit] =
    Stream
      .resource(TelegramClient[IO](config.botConfig.apiKey))
      .flatMap { case given TelegramClient[IO] => Bot.polling[IO].follow(register, subscribe, unsubscribe) }
      .compile
      .drain

  private def subscribe(using client: TelegramClient[IO], log: LogIO[IO], userService: UserService,
                        quotesService: QuotesService): Scenario[IO, Unit] =
    for {
      _ <- Scenario.eval(LogIO[IO].info("Subscribe scenario started"))
      msg <- Scenario.expect(command("subscribe"))
      userId <- Scenario.eval(getUserId(msg))
      _ <- Scenario.eval(quotesService.updateSubscription(userId, true))
      _ <- Scenario.eval(msg.chat.send("You are subscribed to change bitcoin quote notifications"))
    } yield ()


  private def unsubscribe(using client: TelegramClient[IO], log: LogIO[IO], userService: UserService,
                          quotesService: QuotesService): Scenario[IO, Unit] =
    for {
      _ <- Scenario.eval(LogIO[IO].info("Unsubscribe scenario started"))
      msg <- Scenario.expect(command("unsubscribe"))
      userId <- Scenario.eval(getUserId(msg))
      _ <- Scenario.eval(quotesService.updateSubscription(userId, false))
      _ <- Scenario.eval(msg.chat.send("You are unsubscribed"))
    } yield ()


  private def register(using client: TelegramClient[IO], log: LogIO[IO], userService: UserService): Scenario[IO, Unit] =
    for {
      _ <- Scenario.eval(LogIO[IO].info("Register scenario started"))
      msg <- Scenario.expect(command("register"))
      userId <- Scenario.eval(getUserId(msg))
      name <- Scenario.eval(getUsername(msg))
      _ <- Scenario.eval(msg.chat.send("What's your name?"))
      name <- Scenario.expect(text)
      result <- Scenario.eval(maybeRegister(userId, name))
      _ <- Scenario.eval(msg.chat.send(result))
    } yield ()


  private def maybeRegister(id: Long, name: String)(using userService: UserService): IO[String] = {
    userService.getUser(id).map(_.isDefined).flatMap {
      case true => IO.pure("User already registered")
      case false => userService.createUser(id, name).map(_ => s"User $id registered")
    }
  }

  private def getUserId(msg: TextMessage): IO[Long] = {
    IO.fromOption(msg.from.map(_.id))(new Exception("No id found"))
  }

  private def getUsername(msg: TextMessage): IO[String] = {
    IO.fromOption(msg.from.flatMap(_.username))(new Exception("No username found"))
  }

  def notifyUsers(using client: TelegramClient[IO], log: LogIO[IO])(userIds: IO[List[Long]], message: String): IO[Unit] =
    userIds.map(ids => ids.foreach(id => SendMessage(id, message).call.unsafeRunSync()))
}
