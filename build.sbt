val Scala3   = "3.1.1"
val Scala213 = "2.13.6"

val catsVersion          = "2.7.0"
val ceVersion            = "3.3.5"
val fs2Version           = "3.2.4"
val munitVersion         = "0.7.29"
val munitCEVersion       = "1.0.7"
val munitCheckEffVersion = "0.7.1"
val grpcVersion          = "1.44.0"
val googleProtoVersion   = "3.19.4"
val circeVersion         = "0.14.1"
val monocleVersion       = "3.1.0"
val scodecVersion        = "1.1.30"
val junitVersion         = "0.11"
val refinedVersion       = "0.9.27"
val dhallVersion         = "0.10.0-M2"
val castanetVersion      = "0.1.4"

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / scalaVersion         := Scala3
Global / organization         := "mn8.ee"
Global / version              := "0.1.0"

lazy val root = project
  .in(file("."))
  .aggregate(protocol, server, client)

lazy val protocol = project
  .in(file("modules/protocol"))
  .settings(
    name        := "ergo-castanet-protocol",
    description := "Protobuf definitions",
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java" % googleProtoVersion % "protobuf"
    )
  )
  .enablePlugins(Fs2Grpc)

lazy val client = project
  .in(file("modules/client"))
  .settings(
    name        := "ergo-castanet-client",
    description := "Protobuf Client",
    libraryDependencies ++= Seq(
      // "org.dhallj"          %% "dhall-scala"         % dhallVersion,
      "org.scala-lang" %% "scala3-staging" % Scala3,
      //"ch.qos.logback" % "logback-classic" % "1.2.6" % Test,
      "org.dhallj"     % "dhall-imports-mini"  % dhallVersion,
      "org.dhallj"     % "dhall-yaml"          % dhallVersion,
      "org.dhallj"    %% "dhall-circe"         % dhallVersion,
      "ee.mn8"        %% "castanet"       % castanetVersion,
      "org.scalameta" %% "munit"               % munitVersion   % Test,
      "org.scalameta" %% "munit-scalacheck"    % munitVersion   % Test,
      "org.typelevel" %% "munit-cats-effect-3" % munitCEVersion % Test,
      "org.typelevel" %% "cats-core"           % catsVersion,
      "co.fs2"        %% "fs2-core"            % fs2Version,
      "co.fs2"        %% "fs2-io"              % fs2Version,
      "org.typelevel" %% "cats-effect"         % ceVersion,
      "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
      "io.grpc" % "grpc-core"         % scalapb.compiler.Version.grpcJavaVersion,
      "io.grpc" % "grpc-protobuf"     % scalapb.compiler.Version.grpcJavaVersion,
      "io.grpc" % "grpc-stub"         % scalapb.compiler.Version.grpcJavaVersion,
      "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
    ),
    scalapbCodeGeneratorOptions += CodeGeneratorOption.FlatPackage
  )
  .enablePlugins(Fs2Grpc)
  .dependsOn(protocol)
  .dependsOn(protocol % "protobuf")

lazy val server = project
  .in(file("modules/server"))
  .settings(
    scalaVersion := Scala3,
    name         := "ergo-castanet-server",
    description  := "Protobuf Server",
    // nativeImageVersion := "21.2.0",
    Compile / mainClass := Some("org.ergoplatform.castanet.Main"),
    libraryDependencies ++= List(
      "ee.mn8"        %% "castanet" % castanetVersion,
      "org.typelevel" %% "cats-core"     % catsVersion,
      "co.fs2"        %% "fs2-core"      % fs2Version,
      "co.fs2"        %% "fs2-io"        % fs2Version,
      "org.typelevel" %% "cats-effect"   % ceVersion,
      "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
      "io.grpc" % "grpc-core"         % scalapb.compiler.Version.grpcJavaVersion,
      "io.grpc" % "grpc-services"     % scalapb.compiler.Version.grpcJavaVersion
    ),
    scalapbCodeGeneratorOptions += CodeGeneratorOption.FlatPackage
  )
  .enablePlugins(Fs2Grpc)
  .enablePlugins(NativeImagePlugin)
  .dependsOn(protocol)
  .dependsOn(protocol % "protobuf")
