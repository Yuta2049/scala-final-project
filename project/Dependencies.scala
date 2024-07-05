import sbt.*

object Dependencies {
  object Versions {
    val catsEffect = "3.5.4"
    val cats = "2.12.0"
    val pureconfig = "0.17.7"
    val logstage = "1.2.5"
    val canoe = "0.6.0"
    val skunk = "1.1.0-M3"
    val fs2 = "3.10.2"
    val circe = "0.14.9"
    val postgresql = "42.7.3"
    val flywaydb = "10.14.0"
  }

  val distage: Seq[ModuleID] = Seq(
    "io.7mind.izumi" %% "logstage-core" % Versions.logstage,
  )

  val misc: Seq[ModuleID] = Seq(
    "org.augustjune"        %% "canoe"                     % Versions.canoe,
    "org.tpolecat"          %% "skunk-core"                % Versions.skunk,
    "org.typelevel"         %% "cats-effect"               % Versions.catsEffect,
    "org.typelevel"         %% "cats-core"                 % Versions.cats,
    "com.github.pureconfig" %% "pureconfig-core"           % Versions.pureconfig,
    "co.fs2"                %% "fs2-core"                  % Versions.fs2,
    "io.circe"              %% "circe-core"                % Versions.circe,
    "io.circe"              %% "circe-generic"             % Versions.circe,
    "io.circe"              %% "circe-parser"              % Versions.circe,
    "org.postgresql"        % "postgresql"                 % Versions.postgresql,
    "org.flywaydb"          % "flyway-database-postgresql" % Versions.flywaydb,
    "com.github.geirolz"    %% "fly4s"            % "1.0.5",
//    "org.tpolecat"          %% "natchez-core"     % "0.3.5",
//    "org.tpolecat"          %% "natchez-log"      % "0.3.5",
//    "org.typelevel"         %% "log4cats-core"    % "2.7.0",
//    "org.typelevel"         %% "log4cats-slf4j"   % "2.7.0"
  )

  val all: Seq[sbt.ModuleID] = misc ++ distage
}
