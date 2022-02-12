package ee.mn8.castanet

import io.grpc._

object Constants :
  val AuthorizationMetadataKey: io.grpc.Metadata.Key[String] = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)

