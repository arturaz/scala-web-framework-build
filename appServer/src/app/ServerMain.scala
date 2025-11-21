package app

import app.config.*
import app.context.ServerContext
import cats.effect.ExitCode
import ciris.ConfigValue
import ciris.Secret
import com.comcast.ip4s.*
import framework.config.*
import framework.prelude.{*, given}
import framework.utils.FrameworkHttpServer
import framework.utils.SecurityUtils
import framework.utils.ServerApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.trace.TracerProvider
import scribe.Level
import sttp.tapir.server.http4s.*

import java.nio.file.Path
import scala.annotation.nowarn

object ServerMain extends ServerApp {
  type ServerAppConfig = AppConfig

  override def developmentAppConfig: AppConfig = {
    extension (cfg: HttpConfig) {
      @nowarn // Disable potential "unused method" warning
      def withTls: HttpConfig =
        cfg
          .focus(_.server.useHttp2)
          .replace(true)
          // TLS is required for HTTP 2
          .focus(_.server.tls)
          .replace(
            Some(
              HttpServerTLSConfig(
                keyStoreFile = Path.of("certs/localhost.yourdomain.org.keystore.jks"),
                keyStorePassword = "change_it",
                keyPassword = "change_it",
              )
            )
          )
          .focus(_.frontendUri)
          .replace(FrontendUri(uri"https://localhost.yourdomain.org:5173"))
    }

    AppConfig(
      IsProductionMode(false),
      PostgresqlConfig(
        username = "postgres",
        password = Secret("zxcasdqwe123"),
        database = "my_app",
      ),
      HttpConfig(
        HttpServerConfig(
          host = host"0.0.0.0",
          port = port"3005",
          useHttp2 = false,
          tls = None,
        ),
        HttpServerLoggingConfig(logHeaders = true, logBody = true, logLevel = Level.Debug),
        ClientRequestTracingConfig(maxDriftFromCurrentTime = 30.seconds),
        frontendUri = FrontendUri(uri"http://localhost:5173"),
        corsAllowedDomains = Set(
          host"localhost",
          host"host.docker.internal",
        ),
      ) /* .withTls */,
    )
  }

  override def serverName: IO[String] = IO.pure("my_app")
  override def serverVersion: IO[String] = IO.pure("unversioned")

  override def productionAppConfig: ConfigValue[IO, AppConfig] =
    AppConfig.cirisConfig(using isProduction = IsProductionMode(true))

  override def postgresqlConfig(cfg: AppConfig): PostgresqlConfig = cfg.postgresql

  override def isProduction(cfg: AppConfig): IsProductionMode = cfg.isProduction

  given envConfigPrefix: EnvConfigPrefix = EnvConfigPrefix("MY_APP_")

  override def run(args: List[String]): IO[ExitCode] =
    SecurityUtils.ensureProvider[IO](new BouncyCastleProvider()) *> super.run(args)

  override def run(cfg: AppConfig, args: List[String])(using
    Tracer[IO],
    TracerProvider[IO],
    MeterProvider[IO],
  ): IO[ExitCode] = {
    args match {
      case Nil => runServer(cfg)
      case _ =>
        IO {
          println(s"Unknown command: ${args.mkString(" ")}")
          ExitCode.Error
        }
    }
  }

  def runServer(
    cfg: AppConfig
  )(using
    tracerProvider: TracerProvider[IO],
    tracer: Tracer[IO],
    meterProvider: MeterProvider[IO],
  ): IO[ExitCode] = {

    // val externalResources = {
    //   given Show[Throwable] = _.errorWithCausesAndStacktracesString

    //   tracer
    //     .span("startup")
    //     .resource
    //     .flatMap { _ =>
    //       (
    //         postgresqlResource(cfg).attemptNecAsString("postgresql"),
    //         EmberClientBuilder
    //           .default[IO]
    //           .build
    //           // Add gzip support to the HTTP client
    //           .map(org.http4s.client.middleware.GZip[IO]()(_))
    //           .attemptNecAsString("http4s-ember-client"),
    //       )
    //         // Run them in parallel to speed up startup and know if any of them fails without dealing with
    //         // failures sequentially
    //         .parMapN { (postgresql, httpClient) =>
    //           (postgresql, httpClient).mapN(Tuple2.apply) match {
    //             case Valid(tpl) => IO.pure(tpl)
    //             case Invalid(e) =>
    //               IO.raiseError(
    //                 new Exception(
    //                   s"Failed to initialize external resources:\n\n${e.iterator.mkString("\n\n")}"
    //                 )
    //               )
    //           }
    //         }
    //         .evalMap(identity)
    //     }
    // }

    // given PrettyPrintDuration.Strings = PrettyPrintDuration.Strings.English
    val serverResource = for {
      // (transactor, httpClient) <- externalResources

      router = ServerRouter(using
        // transactor,
        tracer,
      )
      _ <- FrameworkHttpServer.serverResource(
        cfg.http,
        FrameworkHttpServer.reqContextMiddleware(_ => IO.pure(ServerContext())),
        router.routes,
        FrameworkHttpServer.otelMiddleware,
        extraRoutes = serverInterpreter => {
          val _ = serverInterpreter // silence unused warning
          Router(
            "/api" -> FrameworkHttpServer.Routes.health
          )
        },
        // By default we assume a client that does not have support for client tracing
        withClientTracing = false
      )
    } yield ()

    val serverUrl = show"http://${cfg.http.server.host}:${cfg.http.server.port.value}"

    for {
      _ <- log.info(show"Starting server on $serverUrl...")
      _ <- serverResource.use(_ => log.info(show"Server started on $serverUrl.") *> IO.never)
    } yield ExitCode.Success
  }
}
