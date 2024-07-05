package com.finalproject.services

import cats.syntax.all.*
import cats.effect.IO
import com.finalproject.services.UserService.User
import logstage.LogIO
import skunk.Session
import skunk.*
import skunk.codec.all
import skunk.implicits.*
import skunk.{Codec, Command, Query}
import skunk.codec.all.{int8, varchar}

trait UserService {
  def getUser(id: Long): IO[Option[User]]

  def createUser(id: Long, name: String): IO[Unit]
}

class UserServiceImpl(using session: Session[IO], log: LogIO[IO]) extends UserService {

  private val codec: Codec[User] =
    (int8, varchar).tupled.imap {
      case (id, name) => User(id, name)
    } { user => (user.id, user.name) }

  private val userDecoder: Decoder[User] = (int8 *: varchar).to[User]

  private val selectUser: Query[Long, User] =
    sql"""
        SELECT id, name
        FROM users
        WHERE id = $int8
      """.query(userDecoder)

  private val insertUser: Command[User] =
    sql"""
      INSERT INTO users (id, name)
      VALUES ($codec)
     """.command

  override def getUser(id: Long): IO[Option[User]] =
    for {
      query <- session.prepare(selectUser)
      res <- query.option(id)
      _ <- res match
        case Some(user) => IO(println(s"User found: $user"))
        case None => IO(println(s"No user found with id: $id"))
    } yield res

  override def createUser(id: Long, name: String): IO[Unit] =
    for {
      _ <- log.info(s"Creating user in DB")
      command <- session.prepare(insertUser)
      rowCount <- command.execute(User(id, name))
    } yield ()
}

object UserService {

  def make(using session: Session[IO], log: LogIO[IO]): IO[UserService] =
    IO(new UserServiceImpl)

  case class User(id: Long, name: String)
}
