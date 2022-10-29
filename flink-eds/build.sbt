resolvers in ThisBuild ++= Seq("Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/", Resolver.mavenLocal)

name := "eds-sample"

version := "0.1-SNAPSHOT"

organization := "de.rwth.gross"

scalaVersion in ThisBuild := "2.11.7"

val flinkVersion = "1.3.2"
val scalaVer = "2.11.7"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-ml" % flinkVersion % "provided"
)

// resolvers in ThisBuild += "jitpack.io" at "https://jitpack.io"
// libraryDependencies += "com.github.sanity" % "quickml" % "10.16"

libraryDependencies += "com.github.haifengl" %% "smile-scala" % "1.4.0"
//libraryDependencies += "org.apache.flink" %% "smile-scala" % "1.4.0"


lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies
  )

mainClass in assembly := Some("de.rwth.gross.Job")

// make run command include the provided dependencies
run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// exclude Scala library from assembly
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
