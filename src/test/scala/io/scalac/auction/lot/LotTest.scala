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

import scala.concurrent.duration.Duration

class LotTest
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with Matchers {
  val testkit = ActorTestKit()

  override def afterAll(): Unit = {
    testkit.shutdownTestKit()
    super.afterAll()
  }

  it should "accept only legal initial state transitions" in {
    val lotActor = testkit.spawn(LotActor())
    val probe = testkit.createTestProbe[GeneralProtocol]()

    val message = MessageRejected(lotActor, "Invalid actor state")
    val callAndResponse = LotState.values.toList.filter(_ == LotState.CLOSED).map(SetState(probe.ref, _) -> message)
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
      SetState(probe.ref, LotState.CLOSED) -> msg,
      SetState(probe.ref, LotState.FINISHED) -> msg
    ))

    lotActor1 ! SetState(probe.ref, LotState.IN_PREVIEW)
    probe.expectMessage(LotStateMessage(lotActor1, LotState.IN_PREVIEW))

    lotActor2 ! SetState(probe.ref, LotState.OPEN)
    probe.expectMessage(LotStateMessage(lotActor2, LotState.OPEN))
  }

  it should "transition through valid states correctly" in {
    val probe = testkit.createTestProbe[GeneralProtocol]()

    val lot1 = testkit.spawn(LotActor())
    val callchain1 = List(CreateLot(probe.ref, "test1", "test")) ++ List(LotState.OPEN, LotState.FINISHED).map(SetState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot1, callchain1, GetLotState(probe.ref), LotStateMessage(lot1, LotState.FINISHED))

    val lot2 = testkit.spawn(LotActor())
    val callchain2 = List(CreateLot(probe.ref, "test2", "test")) ++ List(LotState.IN_PREVIEW, LotState.OPEN, LotState.FINISHED).map(SetState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot2, callchain2, GetLotState(probe.ref), LotStateMessage(lot2, LotState.FINISHED))

    val lot3 = testkit.spawn(LotActor())
    val callchain3 = List(CreateLot(probe.ref, "test3", "test")) ++ List(LotState.IN_PREVIEW, LotState.CLOSED).map(SetState(probe.ref, _))
    testCallchain[GeneralProtocol, GeneralProtocol](probe, lot3, callchain3, GetLotState(probe.ref), LotStateMessage(lot3, LotState.CLOSED))
  }

  it should "handle bids correctly" in {
    val lotActor = testkit.spawn(LotActor())
    val probe = testkit.createTestProbe[GeneralProtocol]()

    List(CreateLot(probe.ref, "test1", "test"), SetState(probe.ref, LotState.OPEN)).foreach { msg =>
      lotActor ! msg
      probe.receiveMessage()
    }

    val bid1 = Bid("a", 10)
    val bid2 = Bid("c", 15)

    lotActor ! PlaceBid(probe.ref, bid1)
    probe.expectMessage(BidSuccess(lotActor, bid1))

    lotActor ! PlaceBid(probe.ref, Bid("b", 5))
    probe.expectMessage(BidFailure(lotActor))

    lotActor ! PlaceBid(probe.ref, bid2)
    probe.expectMessage(BidSuccess(lotActor, bid2))
  }

  private def testCallAndResponses[T1, T2](probe: TestProbe[T2], actorRef: ActorRef[T1], callAndResponseList: Seq[(T1, T2)]): Unit = {
      callAndResponseList.foreach {
        case (call, response) =>
          actorRef ! call
          probe.expectMessage(response)
      }
  }
  private def testCallchain[T1, T2](probe: TestProbe[T2], actorRef: ActorRef[T1], callchain: Seq[T1], lastMessage: T1, lastResponse: T2): Unit = {
    callchain.foreach { msg =>
      actorRef ! msg
      probe.receiveMessage()
    }
    actorRef ! lastMessage
    probe.expectMessage(lastResponse)
  }

}
