package io.scalac.auction.overseer

import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import io.scalac.auction.actors.OverseerActor
import io.scalac.auction.models.Bid
import io.scalac.auction.protocols.{AuctionState, BidSuccess, CreateAuction, CreateLot, GeneralProtocol, PlaceBid, SetAuctionState}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OverseerTest
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with Matchers {
  val testkit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = {
    testkit.shutdownTestKit()
    super.afterAll()
  }

  it should "create auction, lot and bid correctly" in {
    val probe = testkit.createTestProbe[GeneralProtocol]()
    val overseer = testkit.spawn(OverseerActor(), "overseer")

    val bid = Bid("1", 1)
    List(
      CreateAuction(probe.ref, "0", "0", "test", Instant.now, Instant.now.plus(1, ChronoUnit.DAYS)),
      CreateLot(probe.ref, "0", "test", "testLot", "s"),
      SetAuctionState(probe.ref, "0", "test", AuctionState.IN_PROGRESS),
      PlaceBid(probe.ref, "0", "test", "testLot", bid)
    ).foreach(overseer ! _)

    probe.receiveMessages(4)
    val response = probe.receiveMessage().asInstanceOf[BidSuccess]
    response.bid shouldEqual bid
    response.lotName shouldEqual "testLot"
  }

}
