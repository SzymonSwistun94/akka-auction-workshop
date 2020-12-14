package io.scalac.auction.lot

import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.actors.LotActor
import io.scalac.auction.models.Bid
import io.scalac.auction.protocols._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec._
import io.scalac.auction.utils.TestUtils._

class LotTest
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with Matchers {
  val testkit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = {
    testkit.shutdownTestKit()
    super.afterAll()
  }

  it should "accept only legal initial state transitions" in {
    val lotActor = testkit.spawn(LotActor())
    val probe = testkit.createTestProbe[GeneralProtocol]()

    val expectedMessage = InvalidActorState(lotActor)
    val callAndResponse = LotState.values.toList.filter(_ == LotState.Closed).map(SetLotState(probe.ref, _) -> expectedMessage)
    testCallAndResponses(probe, lotActor, callAndResponse)
  }

  it should "transition from closed correctly" in {
    val lotActor1 = testkit.spawn(LotActor())
    val lotActor2 = testkit.spawn(LotActor())
    val probe = testkit.createTestProbe[GeneralProtocol]()

    lotActor1 ! CreateLot(probe.ref, "0", "test1", "test")
    probe.expectMessage(LotStateMessage(lotActor1, LotState.Closed))
    lotActor2 ! CreateLot(probe.ref, "0", "test2", "test")
    probe.expectMessage(LotStateMessage(lotActor2, LotState.Closed))

    val expectedMessage = InvalidStateTransition(lotActor1)

    testCallAndResponses(probe, lotActor1, List(
      SetLotState(probe.ref, LotState.Closed) -> expectedMessage,
      SetLotState(probe.ref, LotState.Finished) -> expectedMessage
    ))

    lotActor1 ! SetLotState(probe.ref, LotState.InPreview)
    probe.expectMessage(LotStateMessage(lotActor1, LotState.InPreview))

    lotActor2 ! SetLotState(probe.ref, LotState.Open)
    probe.expectMessage(LotStateMessage(lotActor2, LotState.Open))
  }

  it should "transition through valid states correctly" in {
    val probe = testkit.createTestProbe[GeneralProtocol]()

    val lot1 = testkit.spawn(LotActor())
    val callchain1 = List(CreateLot(probe.ref, "0", "test1", "test")) ++ List(LotState.Open, LotState.Finished).map(SetLotState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot1, callchain1, GetLotState(probe.ref), LotStateMessage(lot1, LotState.Finished))

    val lot2 = testkit.spawn(LotActor())
    val callchain2 = List(CreateLot(probe.ref, "0", "test2", "test")) ++ List(LotState.InPreview, LotState.Open, LotState.Finished).map(SetLotState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot2, callchain2, GetLotState(probe.ref), LotStateMessage(lot2, LotState.Finished))

    val lot3 = testkit.spawn(LotActor())
    val callchain3 = List(CreateLot(probe.ref, "0", "test3", "test")) ++ List(LotState.InPreview, LotState.Closed).map(SetLotState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot3, callchain3, GetLotState(probe.ref), LotStateMessage(lot3, LotState.Closed))
  }

  it should "handle bids correctly" in {
    val lotActor = testkit.spawn(LotActor())
    val probe = testkit.createTestProbe[GeneralProtocol]()

    List(CreateLot(probe.ref, "0", "test1", "test"), SetLotState(probe.ref, LotState.Open)).foreach { msg =>
      lotActor ! msg
      probe.receiveMessage()
    }

    val bid1 = Bid("a", 10)
    val bid2 = Bid("c", 15)

    lotActor ! PlaceBid(probe.ref, "0", "test1", bid1)
    probe.expectMessage(BidSuccess(lotActor, "test1", bid1))

    lotActor ! PlaceBid(probe.ref, "0", "test1", Bid("b", 5))
    probe.expectMessage(BidTooLow(lotActor, "test1"))

    lotActor ! PlaceBid(probe.ref, "0", "test1", bid2)
    probe.expectMessage(BidSuccess(lotActor, "test1", bid2))
  }

}
