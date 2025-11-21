package app.config

import cats.syntax.all.*
import ciris.ConfigValue
import framework.config.{EnvConfigPrefix, HttpConfig, IsProductionMode, PostgresqlConfig}

case class AppConfig(
  isProduction: IsProductionMode,
  postgresql: PostgresqlConfig,
  http: HttpConfig,
)
object AppConfig {
  given cirisConfig[F[_]](using
    isProduction: IsProductionMode,
    prefix: EnvConfigPrefix,
  ): ConfigValue[F, AppConfig] = (
    ciris.default(isProduction),
    PostgresqlConfig.cirisConfig,
    framework.config.HttpConfig.cirisConfig,
  ).parMapN(apply)
}
