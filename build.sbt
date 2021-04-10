name := "ekv"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "org.bouncycastle" % "bcprov-jdk15on" % "1.68",
  "org.scodec"      %% "scodec-bits"    % "1.1.25",
  "org.scodec"      %% "scodec-core"    % "1.11.7",
  "com.lihaoyi"     %% "os-lib"         % "0.7.4",
  "com.lihaoyi"     %% "utest"          % "0.7.8" % "test"
)

testFrameworks += new TestFramework("utest.runner.Framework")
