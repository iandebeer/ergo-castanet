package ee.mn8.client
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

class PetriSpec extends FunSuite {
  
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
