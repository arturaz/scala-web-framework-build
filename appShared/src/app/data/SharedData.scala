package app.data

import sttp.tapir.Schema

final case class SharedData(name: String) derives io.circe.Codec, Schema
