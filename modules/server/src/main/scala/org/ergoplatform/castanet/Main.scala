package org.ergoplatform.castanet

import fs2.*
import cats.effect.*
import _root_.io.grpc.*

import fs2.grpc.syntax.all._


import fs2.grpc.syntax.all._
import fs2.grpc.server.ServerOptions
import _root_.io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import Constants._
import org.ergoplatform.flow.spec.flowspec.PetrinetFs2Grpc
import org.ergoplatform.flow.spec.flowspec.Response

case class AuthInterceptor(msg: String = "hello") extends ServerInterceptor:
  override def interceptCall[Req,Res] (
      call: ServerCall[Req, Res],
      requestHeaders: Metadata,
      next: ServerCallHandler[Req, Res]) = 
        println(s"$msg: ${requestHeaders.get(Constants.AuthorizationMetadataKey)}")
        next.startCall(call,requestHeaders)

class FlowSpecImpl extends PetrinetFs2Grpc[IO, Metadata] :
  override def addFlow(request: org.ergoplatform.flow.spec.flowspec.FlowSpec, clientHeaders: Metadata): IO[org.ergoplatform.flow.spec.flowspec.Response] =
    IO(Response(true,"It works: " + request.name))
  

/* class GreeterImpl extends GreeterFs2Grpc[IO, Metadata] :
  override def sayHello(request: HelloRequest, clientHeaders: Metadata): IO[HelloReply] =
    IO(HelloReply("Request name is: " + request.name))


  override def sayHelloStream(request: Stream[IO, HelloRequest],clientHeaders: Metadata): Stream[IO, HelloReply] = 
    request.evalMap(req => sayHello(req, clientHeaders)) */

object Main extends IOApp.Simple:
  //extension(i:ServerInterceptor) def interceptWith: ServerServiceDefinition
  val helloService: Resource[IO, ServerServiceDefinition] =
    PetrinetFs2Grpc.bindServiceResource[IO](new FlowSpecImpl, ServerOptions.default )


  def run: IO[Unit] =
    val mySync: Async[IO] = Async[IO]
    val startup: IO[Any] = helloService.use{ service =>
      ServerBuilder
        .forPort(9999)
        .addService(service)
        .intercept(AuthInterceptor("hi there: "))
        .resource[IO](mySync)
        .evalMap(server => IO(server.start()))
        .useForever
    }
    startup >> IO.unit