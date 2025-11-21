package app.config

import cats.effect.kernel.Sync
import ciris.ConfigError
import framework.config.{HttpConfig, IsProductionMode, MultilineString, PostgresqlConfig}
import framework.data.FrameworkDateTime
import framework.utils.PEMLoader

import java.nio.file.{Files, Path}
import java.security.KeyPair
import ciris.ConfigValue
import framework.config.EnvConfigPrefix
import cats.syntax.all.*

/** @param buildTime
  *   the time when the application was built. If `None` this is a development build.
  * @param webPushPEMPrivateKey
  *   the PEM private key for WebPush notifications
  */
case class AppConfig(
  isProduction: IsProductionMode,
  postgresql: PostgresqlConfig,
  http: HttpConfig,
)
object AppConfig {
  given cirisConfig[F[_]: Sync](using
    isProduction: IsProductionMode,
    prefix: EnvConfigPrefix,
  ): ConfigValue[F, AppConfig] = (
    ciris.default(isProduction),
    PostgresqlConfig.cirisConfig,
    framework.config.HttpConfig.cirisConfig,
  ).parMapN(apply)
}
