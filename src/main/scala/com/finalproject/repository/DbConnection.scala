package com.finalproject.repository

import cats.effect.std.Console
import cats.effect.{Resource, Temporal}
import com.finalproject.Config
import fs2.io.net.Network
import org.typelevel.otel4s.trace.Tracer
import skunk.Session

object DbConnection {

  def pooled[F[_] : Temporal : Tracer : Network : Console](config: Config): Resource[F, Resource[F, Session[F]]] =
    Session.pooled(
      host = config.database.host,
      port = config.database.port,
      user = config.database.user,
      password = Some(config.database.password),
      database = config.database.database,
      max = config.database.max,
      debug = config.database.debug
    )
}
