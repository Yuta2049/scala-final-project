package com.finalproject

import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.*

case class BotConfig(
  apiKey: String
)

case class DbConfig(
  host: String,
  port: Int,
  database: String,
  user: String,
  password: String,
  driver: String,
  url: String,  
  migrationsTable: String,
  migrationsLocations: List[String],
  max: Int,
  debug: Boolean                 
)

case class Config(
  botConfig: BotConfig,
  database: DbConfig
) derives ConfigReader


object Config {
  def load: IO[Config] = IO {
    ConfigSource.default.loadOrThrow[Config]
  }
}
