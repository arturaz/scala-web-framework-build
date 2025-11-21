package app

import app.api.Endpoints
import app.context.ServerContext
import doobie.util.transactor.Transactor
import framework.http.middleware.GetCurrentRequest
import org.typelevel.otel4s.trace.Tracer

class ServerRouter(using Tracer[IO]) extends framework.utils.ServerRouter[ServerContext] {

  /** Creates routes that depend on the [[ServerContext]]. */
  override def routesVector(
    serverInterpreter: Http4sServerInterpreter[IO]
  )(using GetCurrentRequest[IO]): NonEmptyVector[ContextRoutes[ServerContext, IO]] = {
    // It's convenient to group the routes into logical groups
    val appRoutes = NonEmptyVector.of(
      serverInterpreter.toContextRoutes(
        Endpoints.hello
          .contextIn[ServerContext]()
          .serverLogicSuccess(app.endpoints.Hello.apply)
      )
    )

    NonEmptyVector
      .of(
        appRoutes
      )
      .flatten
  }
}
