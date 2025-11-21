package app.endpoints

import scala.annotation.unused
import app.context.ServerContext
import app.data.SharedData
import framework.data.FrameworkDateTime

object Hello {
  def apply(@unused ctx: ServerContext): IO[SharedData] = {
    for {
      now <- FrameworkDateTime.nowIO.to[IO]
    } yield SharedData(show"Hello at ${now.asString}")
  }
}
