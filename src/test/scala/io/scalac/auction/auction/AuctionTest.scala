package io.scalac.auction.auction

import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import io.scalac.auction.actors.AuctionActor
import io.scalac.auction.models.Bid
import io.scalac.auction.protocols._
import io.scalac.auction.utils.TestUtils.{testCallAndResponses, testCallchain}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps

class AuctionTest
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with Matchers {
  val testkit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = {
    testkit.shutdownTestKit()
    super.afterAll()
  }

  it should "accept only legal initial state transitions" in {
    val auctionActor = testkit.spawn(AuctionActor(), "auction")
    val probe = testkit.createTestProbe[GeneralProtocol]()

    val message = InvalidActorState(auctionActor)
    val callAndResponse = AuctionState.values.toList.map(SetAuctionState(probe.ref, "0", _) -> message)
    testCallAndResponses(probe, auctionActor, callAndResponse)
    testkit.stop(auctionActor)
  }

  it should "transition through valid states correctly" in {
    val probe = testkit.createTestProbe[GeneralProtocol]()

    val auctionActor1 = testkit.spawn(AuctionActor(), "auction1")
    val chain1 = {
      import AuctionState._
      CreateAuction(probe.ref, "root", "root", "test", Instant.now, Instant.now.plus(1, ChronoUnit.DAYS)) ::
        List(Scheduled, InPreview, InProgress, NearEnd, Finished).map(SetAuctionState(probe.ref, "root", _))
    }
    testCallchain(probe, auctionActor1, chain1, GetAuctionState(probe.ref, "root"), AuctionStateMessage(auctionActor1, AuctionState.Finished))
    testkit.stop(auctionActor1)

    val auctionActor2 = testkit.spawn(AuctionActor(), "auction2")
    val chain2 = {
      import AuctionState._
      CreateAuction(probe.ref, "root", "root", "test2", Instant.now, Instant.now.plus(1, ChronoUnit.DAYS)) ::
        List(InProgress, Finished).map(SetAuctionState(probe.ref, "root", _))
    }
    testCallchain(probe, auctionActor2, chain2, GetAuctionState(probe.ref, "root"), AuctionStateMessage(auctionActor2, AuctionState.Finished))
    testkit.stop(auctionActor2)
  }

  it should "create lot and bid correctly" in {
    val auctionActor = testkit.spawn(AuctionActor(), "auction")
    val probe = testkit.createTestProbe[GeneralProtocol]()

    auctionActor ! CreateAuction(probe.ref, "root", "root", "test", Instant.now, Instant.now.plus(1, ChronoUnit.DAYS))
    probe.expectMessage(AuctionStateMessage(auctionActor, AuctionState.Unscheduled))
    auctionActor ! CreateLot(probe.ref, "root", "testLot", "test")
    probe.receiveMessage().asInstanceOf[LotStateMessage].state shouldEqual LotState.Closed
    auctionActor ! SetAuctionState(probe.ref, "root", AuctionState.InProgress)
    probe.expectMessage(AuctionStateMessage(auctionActor, AuctionState.InProgress))
    probe.receiveMessage().asInstanceOf[LotStateMessage].state shouldEqual LotState.Open
    auctionActor ! PlaceBid(probe.ref, "root", "testLot", Bid("user", 1))
    probe.receiveMessage().asInstanceOf[BidSuccess].bid shouldEqual Bid("user", 1)

    testkit.stop(auctionActor)
  }

  it should "reply with LotNotFound when lot doesn't exist and LotAlreadyExists when attempting to create duplicate" in {
    val auctionActor = testkit.spawn(AuctionActor(), "auction")
    val probe = testkit.createTestProbe[GeneralProtocol]()

    auctionActor ! CreateAuction(probe.ref, "root", "root", "test", Instant.now, Instant.now.plus(1, ChronoUnit.DAYS))
    probe.expectMessage(AuctionStateMessage(auctionActor, AuctionState.Unscheduled))
    auctionActor ! GetLotData(probe.ref, "root", "testLot")
    probe.receiveMessage().asInstanceOf[LotNotFound].name shouldEqual "testLot"
    auctionActor ! CreateLot(probe.ref, "root", "testLot", "test")
    probe.receiveMessage().asInstanceOf[LotStateMessage].state shouldEqual LotState.Closed
    auctionActor ! CreateLot(probe.ref, "root", "testLot", "test")
    probe.receiveMessage().asInstanceOf[LotAlreadyExists].name shouldEqual "testLot"

    testkit.stop(auctionActor)
  }

  it should "reject unauthorized access" in {
    val auctionActor = testkit.spawn(AuctionActor(), "auction")
    val probe = testkit.createTestProbe[GeneralProtocol]()

    auctionActor ! CreateAuction(probe.ref, "root", "root", "test2", Instant.now, Instant.now.plus(1, ChronoUnit.DAYS))
    probe.expectMessage(AuctionStateMessage(auctionActor, AuctionState.Unscheduled))
    auctionActor ! SetAuctionState(probe.ref, "0", AuctionState.InProgress)
    probe.expectMessage(AccessDenied(auctionActor))

    testkit.stop(auctionActor)
  }
}
