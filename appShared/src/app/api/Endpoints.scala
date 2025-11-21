package app.api

import framework.tapir.FrameworkTapir.*
import app.data.SharedData

object Endpoints {
  val hello = endpoint.get.in("hello").out(jsonBody[SharedData])
}
