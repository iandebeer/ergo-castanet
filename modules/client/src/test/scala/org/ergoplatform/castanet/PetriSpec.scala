package org.ergoplatform.client
import cats.data.State
import cats.syntax.functor.*
import io.circe.Decoder
import io.circe.Encoder
import io.circe.*
import io.circe.generic.auto.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.syntax.*
import munit.*
import scodec.bits.*

import scala.collection.immutable.ListSet
import scala.io.Source
import scala.quoted.*
import ee.mn8.castanet.*

import org.ergoplatform.flow.spec.flowspec.Wallet
import org.ergoplatform.flow.spec.flowspec.Wallet.Box.ErgCondition
import org.ergoplatform.flow.spec.flowspec.Transaction
import org.ergoplatform.flow.spec.flowspec.Transaction.InputArrow
import org.ergoplatform.flow.spec.flowspec.Transaction.SpendingPath
import org.ergoplatform.flow.spec.flowspec.PetrinetFs2Grpc
import org.ergoplatform.flow.spec.flowspec.FlowSpec
import org.ergoplatform.flow.spec.flowspec.FlowSpec.{Parameter => Param}
import org.ergoplatform.flow.spec.flowspec.Wallet.Box.TokenCondition
import org.ergoplatform.flow.spec.flowspec.Wallet.Box.Condition

class PetriSpec extends FunSuite {
  def defineSpec() =
    // The game contract is created by the second player using the funds from the createGameTransaction
    // The output can be spent by the second player after the end of the game, if the first player fails to provide its secret and answer for the withdrawal
    // At any time, the winner can withdraw the funds providing the answer and the secret of the first player
    val gameScript = s""" 
                        | { // Get inputs from the createGameTransaction, this is the last box in the input list
                        |   val p2Choice = INPUTS(INPUTS.size-1).R4[Byte].get
                        |   val p1AnswerHash = INPUTS(INPUTS.size-1).R5[Coll[Byte]].get
                        |   val player1Pk = INPUTS(INPUTS.size-1).R6[SigmaProp].get
                        |   val partyPrice = INPUTS(INPUTS.size-1).R7[Long].get
                        |   val game_end = INPUTS(INPUTS.size-1).R8[Int].get
                        |   
                        |   // Get the outputs register
                        |   val p1Choice = OUTPUTS(0).R4[Byte].get
                        |   val p1Secret = OUTPUTS(0).R5[Coll[Byte]].get
                        |   
                        |   // Compute the winner (the check of the correctness of the winner answer is done later)
                        |   val p1win = ( p2Choice != p1Choice )
                        |   
                        |   sigmaProp (
                        |      // After the end of the game the second player wins by default
                        |      // This prevents the first player to block to game by not relealing its answer and secret
                        |     (player2Pk && HEIGHT > game_end) || 
                        |       allOf(Coll(
                        |         // The hash of the first player answer must match
                        |         blake2b256(p1Secret ++ Coll(p1Choice)) == p1AnswerHash,
                        |         // The winner can withdraw
                        |         (player1Pk && p1win )|| (player2Pk && ( p1win == false ))
                        |       ))
                        |   )
                        |  }
 """.stripMargin

    // The create game contract is created by the first player to engage the game
    // It allows to cancel the game and get a refund after the end of the game
    // At any time the funds can be spent by the second player if:
    //   The funds are protected by the game script
    //   The output value is more than twice the party price
    //   The R5 register contains the hash the the answer of the first player
    //   The R6 register contains public key of the first player
    //   The party price and game_end are unchanged from the initial contract
    val createGameScript = s""" 
      {
        val gameScriptHash = SELF.R4[Coll[Byte]].get
        val p1AnswerHash = SELF.R5[Coll[Byte]].get

        sigmaProp (
          (player1Pk && HEIGHT > game_end) ||
              allOf(Coll(
                  player2Pk,
                  blake2b256(OUTPUTS(0).propositionBytes) == gameScriptHash,
                  OUTPUTS(0).value >= 2 * partyPrice,
                  OUTPUTS(0).R5[Coll[Byte]].get == p1AnswerHash,
                  OUTPUTS(0).R6[SigmaProp].get == player1Pk,
                  OUTPUTS(0).R7[Long].get == partyPrice,
                  OUTPUTS(0).R8[Int].get == game_end
                ))
        )
      }
    """.stripMargin

    val name: String = "Coin Flip Game"
    val parameters = Seq(
      Param("playPrice", "Long"),
      Param("minTxFee", "Long"),
      Param("playerFunds", "Long"),
      Param("gameDuration", "Long"),
      Param("playCount", "Integer"),
      Param("p1PK", "String"),
      Param("p2PK", "String")
    )
    val wallets: Seq[Wallet] = Seq(
      Wallet(
        "player1",
        "pk",
        Seq(Wallet.Box("funds", None))
      ),
      Wallet(
        "player2",
        "pk",
        Seq(Wallet.Box("funds", None))
      ),
      Wallet(
        "game",
        "pk",
        Seq(
          Wallet.Box(
            "game",
            Some(ErgCondition("targetBoxName", "erg_expression")),
            Seq(TokenCondition("tokeName", "targetBoxName", "expression")),
            Seq(Condition("targetBoxName", "expression"))
          )
        )
      ),
      Wallet(
        "state",
        "pk",
        Seq(Wallet.Box("init", None), Wallet.Box("end", None))
      )
    )
    val transactions = Seq(
      Transaction(
        name = "provide funds",
        inputs = Seq(InputArrow("state", "init", Some(SpendingPath("action", "Condition"))))
      ),
      Transaction(
        name = "play game",
        inputs = Seq(InputArrow("p2Choice", "fromBox", Some(SpendingPath("action", "Condition"))))
      ),
      Transaction(
        name = "create game",
        inputs = Seq(InputArrow("p2Choice", "fromBox", Some(SpendingPath("action", "Condition"))))
      ),
      Transaction(
        name = "withdraw",
        inputs = Seq(InputArrow("p2Choice", "fromBox", Some(SpendingPath("action", "Condition"))))
      )
    )

    //val boxes: Seq[Wallet.Box] = Seq(new Wallet.Box("",None))

    FlowSpec(name, parameters, wallets, transactions)

  test("build petri net") { 

    import Arc._
    val jp1 = """{"id":1,"name":"start","capacity":1}"""

    val p1: Place = decode[Place](jp1).getOrElse(Place(999, "Error", 0))
    val pp1       = p1.asJson.noSpaces

    println(s"\nPlace:\n$pp1\n")

    val p2: Place = Place(2, "left", 3)
    val p3: Place = Place(3, "right", 1)
    val p4: Place = Place(4, "joint", 3)
    val p5: Place = Place(5, "end", 1)

    // val rpc =

    val jt1 = """{"id":6,"name":"splitter","capacity":1}""""""
    val l   = (l: String) => println(l)
    val s1 = Service(
      "ee.mn8.castanet",
      "HelloFs2Grpc",
      List[RPC](RPC(name = "sayHello", input = "", output = ""))
    )
    val r1 = s1.rpcs.head
    // def func(serviceName: String, rpcName: String): Function1[String, Unit] = ???
    // '{(l: String) => println(l)}

    val t1: Transition = Transition(6, "splitter", s1, r1)
    /*
    val tt1       = t1.asJson.noSpaces
     */

    val t2: Transition = Transition(7, "joiner", s1, r1)
    val t3: Transition = Transition(8, "continuer", s1, r1)

    val n    = PetriNetBuilder().addAll(ListSet(p1, p2, p3, p4, p5))
    val json = List(p1, p2, p3, p4, p5).asJson.spaces2
    println(s"\n\nJSON LIST:\n $json")
    val ps = decode[List[Place]](json)
    println(s"\n\nJSON decoded: $ps")

    val json2 = List(t1, t2, t3).asJson.spaces2
    println(s"\n\nJSON LIST:\n $json2")
    val ts = decode[List[Transition]](json2)
    println(s"\n\nJSON decoded: $ts")

    //arcs = ListSet(Arc.Weighted(from = 1l,to = 2l,weight = 1)), places = ListSet[Place](p1), transitions = ListSet[Transition](Transition(id = 2l, name = "test", fn = t)))
    val n2 = n.addAll(ListSet(t1, t2, t3))
    val n3 = n2
      .add(Weighted(1, 6, 1))
      .add(Weighted(6, 2, 1))
      .add(Weighted(6, 3, 1))
      .add(Weighted(2, 7, 2))
      .add(Weighted(3, 7, 1))
      .add(Weighted(7, 4, 1))
      .add(Weighted(4, 8, 3))
      .add(Weighted(8, 5, 1))
    //val x = n3.ColouredPetriNet(Map[NodeId,ListSet[LinkableElement]]())
    println("_" * 10)
    println(s"Net 3: $n3")
    println("_" * 10)
    println(s"Linkables: ${n3.build()}")
    println("_" * 10)
    val pn = n3.build()
    val places = pn.elements.values.collect { case p: Place =>
      p
    }
    val dimensions = (places.size, places.maxBy(p => p.capacity).capacity)
    println(dimensions)

    val m1 = Markers(pn)
    println(s"${m1}\n${m1.toStateVector}")

    val m2 = m1.setMarker(Marker(1, bin"1"))
    println(s"${m2}\n${m2.toStateVector}")

    val m3 = m2.setMarker(Marker(2, bin"1")).setMarker(Marker(4, bin"11"))
    println(s"${m3}\n${m3.toStateVector}")

    val m4 = Markers(pn, m3.toStateVector)
    println(s"${m4}\n${m4.toStateVector} \n${m4.serialize}")

    val m5 = Markers(pn, m4.serialize)
    println(s"${m5}\n${m5.toStateVector} \n${m5.serialize}")
    PetriPrinter(fileName = "petrinet1", petriNet = pn).print(Option(m3))
    val steps: State[Step, Unit] =
      for
        p1 <- pn.step
        p2 <- pn.step
        p3 <- pn.step
      yield (
        PetriPrinter(fileName = "petrinet2", petriNet = pn).print(Option(p1)),
        PetriPrinter(fileName = "petrinet3", petriNet = pn).print(Option(p2)),
        PetriPrinter(fileName = "petrinet4", petriNet = pn).print(Option(p3))
      )
    steps.run(Step(m3, true, 1)).value

  }
}
