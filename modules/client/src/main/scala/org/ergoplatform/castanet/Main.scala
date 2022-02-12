package org.ergoplatform.castanet

import cats.effect.*
import cats.effect.std.Dispatcher
import fs2.*
import _root_.io.grpc.*
import fs2.grpc.syntax.all.*
import java.util.concurrent.Executor
import Constants.*

import scala.concurrent.ExecutionContext.Implicits.global
import fs2.grpc.client.ClientOptions
import _root_.io.grpc.ClientInterceptor

import cats.effect.IO
import org.ergoplatform.flow.spec.flowspec.FlowSpec.{Parameter => Param}

/* import org.ergoplatform.flow.spec.flowspec.PetrinetFs2Grpc
import org.ergoplatform.flow.spec.flowspec.Wallet.Box
import org.ergoplatform.flow.spec.flowspec.Wallet.Box.ErgCondition
import org.ergoplatform.flow.spec.flowspec.Transaction */
import org.ergoplatform.flow.spec.flowspec.Wallet
import org.ergoplatform.flow.spec.flowspec.Wallet.Box.ErgCondition
import org.ergoplatform.flow.spec.flowspec.Transaction
import org.ergoplatform.flow.spec.flowspec.Transaction.InputArrow
import org.ergoplatform.flow.spec.flowspec.Transaction.SpendingPath
import org.ergoplatform.flow.spec.flowspec.PetrinetFs2Grpc
import org.ergoplatform.flow.spec.flowspec.FlowSpec

object Main extends IOApp:

  case class JwtCredentials() extends CallCredentials:
    override def thisUsesUnstableApi(): Unit = {}
    override def applyRequestMetadata(
        requestInfo: CallCredentials.RequestInfo,
        appExecutor: Executor,
        applier: CallCredentials.MetadataApplier
    ): Unit =
      val headers = new Metadata()
      headers.put[String](AuthorizationMetadataKey, "test")
      applier.apply(headers)


  case class KeycloakInterceptor(s: String) extends ClientInterceptor:
    override def interceptCall[Req, Res](
        methodDescriptor: MethodDescriptor[Req, Res],
        callOptions: CallOptions,
        channel: Channel
    ) =
      println("hello from the client")
      channel.newCall[Req, Res](methodDescriptor, callOptions.withCallCredentials(JwtCredentials()))

  val managedChannelStream: Stream[IO, ManagedChannel] =
    ManagedChannelBuilder
      .forAddress("127.0.0.1", 9999)
      .usePlaintext()
      .intercept(KeycloakInterceptor("hi"))
      .stream[IO]

  
  //def getStub(fqn:String):fs2.grpc.GeneratedCompanion = ???
  //def func1(stub:GreeterFs2Grpc[cats.effect.IO, Metadata],s:String)(f: Function1[String,String]):String = f(s)
  override def run(args: List[String]): IO[ExitCode] = { 

    val name : String = "Coin Flip"
    val parameters = Seq(Param("param1","PK"))
    val wallets: Seq[Wallet] = Seq(new Wallet("player","ergo script", Seq(Wallet.Box("standard", Option(ErgCondition("this","script"))))))
    val transaction = Seq(Transaction(name = "play",inputs = Seq(InputArrow("fromWallet","fromBox",Some(SpendingPath("action", "Condition"))))))
    val flow : FlowSpec = FlowSpec(name,parameters,wallets,transaction)

    val boxes: Seq[Wallet.Box] = Seq(new Wallet.Box("",None))

    for {
      dispatcher     <- Stream.resource(Dispatcher[IO])
      managedChannel <- managedChannelStream
      flowStub = PetrinetFs2Grpc.stub[IO](dispatcher, managedChannel, ClientOptions.default)
      //helloStub: GreeterFs2Grpc[cats.effect.IO, Metadata] = GreeterFs2Grpc.stub[IO](dispatcher, managedChannel, ClientOptions.default)
      // _ <- Stream.eval(runProgram(helloStub))
      _ <- Stream.eval(
        for {
          response <- flowStub.addFlow(flow, new Metadata())
          message <- IO(response.message)
          _        <- IO.println(message)
        } yield ()
      )

    } yield ()
  }.compile.drain.as(ExitCode.Success)

