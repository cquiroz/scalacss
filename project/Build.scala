import sbt._, Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin, ScalaJSPlugin.autoImport._
import Lib._

object ScalaCssBuild {

  private val ghProject = "scalacss"

  private val publicationSettings =
    Lib.publicationSettings(ghProject)

  object Ver {
    final val MTest         = "0.4.5"
    final val Nyaya         = "0.8.1"
    final val ReactJs       = "15.5.4"
    final val Scala211      = "2.11.11"
    final val Scala212      = "2.12.4"
    final val ScalaJsDom    = "0.9.1"
    final val ScalaJsReact  = "1.1.1"
    final val Scalatags     = "0.6.5"
    final val Scalaz        = "7.2.11"
    final val UnivEq        = "1.0.2"
  }

  def scalacFlags = Seq(
    "-deprecation",
    "-unchecked",
    // "-Ywarn-dead-code",
    // "-Ywarn-unused",
    // "-Ywarn-value-discard",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials")

  val commonSettings = ConfigureBoth(
    _.settings(
      organization              := "com.github.japgolly.scalacss",
      homepage                  := Some(url("https://github.com/japgolly/scalacss")),
      licenses                  += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),
      scalaVersion              := Ver.Scala212,
      crossScalaVersions        := Seq(Ver.Scala211, Ver.Scala212),
      scalacOptions            ++= scalacFlags,
      scalacOptions in Compile ++= byScalaVersion { case (2, 12) => Seq("-opt:l:method") }.value,
      scalacOptions in Test    --= Seq("-Ywarn-unused"),
      shellPrompt in ThisBuild  := ((s: State) => Project.extract(s).currentRef.project + "> "),
      triggeredMessage          := Watched.clearWhenTriggered,
      incOptions                := incOptions.value.withNameHashing(true).withLogRecompileOnMacro(false),
      updateOptions             := updateOptions.value.withCachedResolution(true))
    .configure(
      addCommandAliases(
        "/"   -> "project root",
        "L"   -> "root/publishLocal",
        "C"   -> "root/clean",
        "T"   -> ";root/clean;root/test",
        "TL"  -> ";T;L",
        "c"   -> "compile",
        "tc"  -> "test:compile",
        "t"   -> "test",
        "to"  -> "test-only",
        "tq"  -> "test-quick",
        "cc"  -> ";clean;compile",
        "ctc" -> ";clean;test:compile",
        "ct"  -> ";clean;test")))

  def byScalaVersion[A](f: PartialFunction[(Int, Int), Seq[A]]): Def.Initialize[Seq[A]] =
    Def.setting(CrossVersion.partialVersion(scalaVersion.value).flatMap(f.lift).getOrElse(Nil))

  def definesMacros = ConfigureBoth(
    _.settings(
      scalacOptions += "-language:experimental.macros",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        // "org.scala-lang" % "scala-library" % scalaVersion.value,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided")))

  def utestSettings = ConfigureBoth(
    _.settings(
      libraryDependencies += "com.lihaoyi" %%% "utest" % Ver.MTest % "test",
      testFrameworks      += new TestFramework("utest.runner.Framework")))
    .jsConfigure(
      // Not mandatory; just faster.
      _.settings(jsEnv in Test := new PhantomJS2Env(scalaJSPhantomJSClassLoader.value)))

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  lazy val root =
    Project("root", file("."))
      .configure(commonSettings.jvm, preventPublication)
      .aggregate(rootJVM, rootJS, BenchBuild.bench)

  lazy val rootJVM =
    Project("JVM", file(".rootJVM"))
      .configure(commonSettings.jvm, preventPublication)
      .aggregate(coreJVM, elisionTestJVM, extScalatagsJVM)

  lazy val rootJS =
    Project("JS", file(".rootJS"))
      .configure(commonSettings.jvm, preventPublication)
      .aggregate(coreJS, elisionTestJS, extScalatagsJS, extReact)

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  lazy val coreJVM = core.jvm
  lazy val coreJS  = core.js
  lazy val core = crossProject
    .configureCross(
      commonSettings,
      publicationSettings,
      definesMacros,
      //, Gen.attrAliases
      utestSettings)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.japgolly.univeq" %%% "univeq"      % Ver.UnivEq,
        "com.github.japgolly.nyaya"  %%% "nyaya-gen"   % Ver.Nyaya  % "test",
        "com.github.japgolly.nyaya"  %%% "nyaya-prop"  % Ver.Nyaya  % "test",
        "com.github.japgolly.nyaya"  %%% "nyaya-test"  % Ver.Nyaya  % "test",
        "org.scalaz"                 %%% "scalaz-core" % Ver.Scalaz % "test"))
    .jsSettings(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % Ver.ScalaJsDom)
    .jvmSettings(
      initialCommands := "import scalacss._")

  lazy val elisionTestJVM = elisionTest.jvm
  lazy val elisionTestJS  = elisionTest.js
  lazy val elisionTest = crossProject
    .in(file("elision-test"))
    .configureCross(commonSettings, utestSettings)
    .configureAll(preventPublication)
    .dependsOn(core)
    .settings(
      scalacOptions ++= Seq("-Xelide-below", "OFF")
    )

  lazy val extScalatagsJVM = extScalatags.jvm
  lazy val extScalatagsJS  = extScalatags.js
  lazy val extScalatags = crossProject
    .in(file("ext-scalatags"))
    .configureCross(commonSettings, publicationSettings)
    .dependsOn(core)
    .configureCross(utestSettings)
    .settings(
      moduleName := "ext-scalatags",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "scalatags"   % Ver.Scalatags,
        "org.scalaz"  %%% "scalaz-core" % Ver.Scalaz % "test"))

  lazy val extReact = project
    .in(file("ext-react"))
    .enablePlugins(ScalaJSPlugin)
    .configure(commonSettings.js, publicationSettings.js, utestSettings.js)
    .dependsOn(coreJS)
    .settings(
      moduleName := "ext-react",
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "core"        % Ver.ScalaJsReact,
        "com.github.japgolly.scalajs-react" %%% "test"        % Ver.ScalaJsReact % "test",
        "org.scalaz"                        %%% "scalaz-core" % Ver.Scalaz       % "test"),
      jsDependencies ++= Seq(
        "org.webjars.bower" % "react" % Ver.ReactJs % "test"
          /        "react-with-addons.js"
          minified "react-with-addons.min.js"
          commonJSName "React",
        "org.webjars.bower" % "react" % Ver.ReactJs % "test"
          /         "react-dom.js"
          minified  "react-dom.min.js"
          dependsOn "react-with-addons.js"
          commonJSName "ReactDOM",
        "org.webjars.bower" % "react" % Ver.ReactJs % "test"
          /         "react-dom-server.js"
          minified  "react-dom-server.min.js"
          dependsOn "react-dom.js"
          commonJSName "ReactDOMServer"),
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv())
}
