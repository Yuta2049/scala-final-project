package com.finalproject.services

import canoe.api.TelegramClient
import cats.effect.{IO, *}
import cats.syntax.all.*
import com.finalproject.{Config, Telegram}
import com.finalproject.dto.QuoteDb
import fs2.Stream

import scala.concurrent.duration.*
import logstage.LogIO


trait NotificationsSender {
  def sendNotifications: IO[Unit]

  def sendNotificationsSchedule: IO[Unit]

  /*
    Нам приходит 2 котировки - более поздняя и более ранняя.
    Более раннюю принимаем за базу, вычисляем от неё 0,01%.
    Если разница котировок больше, чем 0,01% от первой, то отправляем нотификацию об изменени
   */
  def checkDifference(quotes: List[QuoteDb]): IO[Boolean] =
    IO.apply(quotes.size == 2 && quotes(1).rate * 0.0001 < (quotes.head.rate - quotes(1).rate).abs)
}

object NotificationsSender {

  def make(using config: Config, log: LogIO[IO], quotesService: QuotesService, client: TelegramClient[IO]):
  IO[NotificationsSender] =
    IO(new NotificationsSenderImpl)
}

class NotificationsSenderImpl(using config: Config, log: LogIO[IO], quotesService: QuotesService,
                              client: TelegramClient[IO]) extends NotificationsSender {

  override def sendNotifications: IO[Unit] = {
    for {
      pair <- quotesService.getLastQuotesPair("USD")
      need_notify <- checkDifference(pair)
      _ <- log.info(s"need_notify: $need_notify")
      _ <- Telegram.notifyUsers(quotesService.getSubscribers, "Quotes increased by 0,1%")
    } yield ()
  }

  override def sendNotificationsSchedule: IO[Unit] = {
    Stream
      .awakeEvery[IO](30.seconds)
      .evalMap(_ => sendNotifications)
      .compile
      .drain
  }
}
