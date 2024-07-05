package com.finalproject.services

import cats.syntax.all.*
import cats.effect.IO
import com.finalproject.dto.QuoteDb
import logstage.LogIO
import skunk.*
import skunk.implicits.*
import skunk.{Decoder, Query, Session}
import skunk.codec.all
import skunk.codec.all.*


trait QuotesService {
  def getLastQuotesPair(currencyCode: String): IO[List[QuoteDb]]

  def getSubscribers: IO[List[Long]]

  def updateSubscription(userId: Long, needSend: Boolean): IO[Unit]
}

class QuotesServiceImpl(using session: Session[IO], log: LogIO[IO]) extends QuotesService {

  private case class Subscription(userId: Long, needSend: Boolean)

  private val codec: Codec[Subscription] =
    (int8, bool).tupled.imap {
      case (userId, needSend) => Subscription(userId, needSend)
    } { subscription => (subscription.userId, subscription.needSend) }

  private val quotesDecoder: Decoder[QuoteDb] = (varchar(3) *: timestamp *: numeric(16, 8)).to[QuoteDb]

  private val selectLastQuotesPair: Query[String, QuoteDb] =
    sql"""
      select distinct currency_code, quote_time, rate
      from bitcoin_quotes bq
       where currency_code = $varchar
       order by quote_time desc limit 2
    """.query(quotesDecoder)


  private val selectSubscribers: Query[Void, Long] =
    sql"""
          select user_id from subscriptions where need_send = true
        """.query(int8)

  private val upsertSubscription: Command[Subscription] =
    sql"""
      INSERT INTO subscriptions(user_id, need_send)
      VALUES ($codec)
      ON CONFLICT(user_id) 
      DO UPDATE SET need_send = EXCLUDED.need_send;
     """.command

  override def updateSubscription(userId: Long, needSend: Boolean): IO[Unit] =
    for {
      _ <- log.info(s"Updating subscription in DB")
      command <- session.prepare(upsertSubscription)
      rowCount <- command.execute(Subscription(userId, needSend))
    } yield ()


  override def getLastQuotesPair(currencyCode: String): IO[List[QuoteDb]] =
    session.execute(selectLastQuotesPair)(currencyCode)


  override def getSubscribers: IO[List[Long]] =
    session.execute(selectSubscribers)
}

object QuotesService {

  def make(using session: Session[IO], log: LogIO[IO]): IO[QuotesService] =
    IO(new QuotesServiceImpl)
}
