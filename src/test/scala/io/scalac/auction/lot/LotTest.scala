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

    val message = MessageRejected(lotActor, "Invalid actor state")
    val callAndResponse = LotState.values.toList.filter(_ == LotState.CLOSED).map(SetLotState(probe.ref, _) -> message)
    testCallAndResponses(probe, lotActor, callAndResponse)
  }

  it should "transition from closed correctly" in {
    val lotActor1 = testkit.spawn(LotActor())
    val lotActor2 = testkit.spawn(LotActor())
    val probe = testkit.createTestProbe[GeneralProtocol]()

    lotActor1 ! CreateLot(probe.ref, "test1", "test")
    probe.expectMessage(LotStateMessage(lotActor1, LotState.CLOSED))
    lotActor2 ! CreateLot(probe.ref, "test2", "test")
    probe.expectMessage(LotStateMessage(lotActor2, LotState.CLOSED))

    val msg = MessageRejected(lotActor1, "Invalid state transition")

    testCallAndResponses(probe, lotActor1, List(
      SetLotState(probe.ref, LotState.CLOSED) -> msg,
      SetLotState(probe.ref, LotState.FINISHED) -> msg
    ))

    lotActor1 ! SetLotState(probe.ref, LotState.IN_PREVIEW)
    probe.expectMessage(LotStateMessage(lotActor1, LotState.IN_PREVIEW))

    lotActor2 ! SetLotState(probe.ref, LotState.OPEN)
    probe.expectMessage(LotStateMessage(lotActor2, LotState.OPEN))
  }

  it should "transition through valid states correctly" in {
    val probe = testkit.createTestProbe[GeneralProtocol]()

    val lot1 = testkit.spawn(LotActor())
    val callchain1 = List(CreateLot(probe.ref, "test1", "test")) ++ List(LotState.OPEN, LotState.FINISHED).map(SetLotState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot1, callchain1, GetLotState(probe.ref), LotStateMessage(lot1, LotState.FINISHED))

    val lot2 = testkit.spawn(LotActor())
    val callchain2 = List(CreateLot(probe.ref, "test2", "test")) ++ List(LotState.IN_PREVIEW, LotState.OPEN, LotState.FINISHED).map(SetLotState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot2, callchain2, GetLotState(probe.ref), LotStateMessage(lot2, LotState.FINISHED))

    val lot3 = testkit.spawn(LotActor())
    val callchain3 = List(CreateLot(probe.ref, "test3", "test")) ++ List(LotState.IN_PREVIEW, LotState.CLOSED).map(SetLotState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot3, callchain3, GetLotState(probe.ref), LotStateMessage(lot3, LotState.CLOSED))
  }

  it should "handle bids correctly" in {
    val lotActor = testkit.spawn(LotActor())
    val probe = testkit.createTestProbe[GeneralProtocol]()

    List(CreateLot(probe.ref, "test1", "test"), SetLotState(probe.ref, LotState.OPEN)).foreach { msg =>
      lotActor ! msg
      probe.receiveMessage()
    }

    val bid1 = Bid("a", 10)
    val bid2 = Bid("c", 15)

    lotActor ! PlaceBid(probe.ref, "test1", bid1)
    probe.expectMessage(BidSuccess(lotActor, "test1", bid1))

    lotActor ! PlaceBid(probe.ref, "test1", Bid("b", 5))
    probe.expectMessage(BidFailure(lotActor, "test1"))

    lotActor ! PlaceBid(probe.ref, "test1", bid2)
    probe.expectMessage(BidSuccess(lotActor, "test1", bid2))
  }

}
