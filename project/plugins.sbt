val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.19")

addSbtPlugin("org.scala-js"     % "sbt-scalajs"              % scalaJSVersion)
addSbtPlugin("com.jsuereth"     % "sbt-pgp"                  % "1.0.1")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"              % "0.3.0")
