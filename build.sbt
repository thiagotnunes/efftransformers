name := "efftransformers"
scalaVersion := "2.11.11"
organization := "com.thiagotnunes"

scalacOptions ++= Seq(
  "-Ypartial-unification"
)

javacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies ++= Seq(
  "com.twitter" %% "util-core" % "17.10.0",
  "org.typelevel" %% "cats" % "0.7.2"
)

resolvers += Resolver.sonatypeRepo("releases")
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

