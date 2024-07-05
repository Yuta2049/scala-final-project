package com.finalproject.services

import cats.syntax.all.*

import scala.concurrent.duration.*
import cats.effect.IO
import com.finalproject.Config
import com.finalproject.services.QuotesLoader.callEffect
import fs2.Stream
import logstage.LogIO
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import skunk.{Codec, Command, Session}
import cats.effect.unsafe.implicits.global
import org.http4s.{MediaType, Method, Uri, client}
import org.http4s.implicits.uri
import org.http4s.client.dsl.io.*
import org.http4s.headers.*
import org.http4s.Method.*
import com.finalproject.dto._
import io.circe.{Decoder, jawn}
import io.circe.generic.semiauto.*
import skunk.codec.all.{numeric, timestamp, varchar}
import skunk.implicits.sql

trait QuotesLoader {

  def loadQuotes: IO[Unit]

  def loadQuotesSchedule: IO[Unit]
}

object QuotesLoader {

  def make(using config: Config, session: Session[IO], log: LogIO[IO]): IO[QuotesLoader] = {
    IO(new QuotesLoaderImpl)
  }

  def callEffect(client: Client[IO]): IO[String] = {
    val request = GET(uri"https://api.coindesk.com/v1/bpi/currentprice.json", Accept(MediaType.application.json))
    client.expect[String](request)
  }
}

class QuotesLoaderImpl(using config: Config, session: Session[IO], log: LogIO[IO]) extends QuotesLoader {

  private val codec: Codec[QuoteDb] =
    (varchar, timestamp, numeric).tupled.imap {
      case (currencyCode, quoteDatetime, rate) => QuoteDb(currencyCode, quoteDatetime, rate)
    } { quote => (quote.currencyCode, quote.quoteDatetime, quote.rate) }

  private val insert: Command[QuoteDb] =
    sql"""
        INSERT INTO bitcoin_quotes(currency_code, quote_time, rate)
        VALUES ($codec)
      """.command

  private def writeQuotes(response: BitcoinResponse) = IO[Unit] {
    println("writeQuotes")
    val v =
      for {
        command <- session.prepare(insert)
        rowCount <- command.execute(QuoteDb(response.bpi.USD.code,
          response.time.updatedISO.toLocalDateTime, response.bpi.USD.rate_float))
      } yield ()
    v.unsafeRunSync()
  }

  override def loadQuotes: IO[Unit] = {

    implicit val quoteDecoder: Decoder[Quote] = deriveDecoder[Quote]
    implicit val bpiDecoder: Decoder[BPI] = deriveDecoder[BPI]
    implicit val timeDecoder: Decoder[Time] = deriveDecoder[Time]
    implicit val jsonDecoder: Decoder[BitcoinResponse] = deriveDecoder[BitcoinResponse]

    for {
      response <- BlazeClientBuilder[IO].resource.use(client =>
        for {
          result <- callEffect(client).redeem(
            error => "could not get a result",
            something => something
          )
        } yield result
      )

      res <- jawn.decode[BitcoinResponse](response) match {
        case Left(error) => IO.println(s"error during parsing: $error")
        case Right(response) => writeQuotes(response)
      }
    } yield res
  }

  override def loadQuotesSchedule: IO[Unit] = {
    Stream
      .awakeEvery[IO](30.seconds)
      .evalMap(_ => loadQuotes)
      .compile
      .drain
  }
}
