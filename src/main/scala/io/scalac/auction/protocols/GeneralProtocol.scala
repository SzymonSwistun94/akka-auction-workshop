package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

trait GeneralProtocol {
  def sender: ActorRef[GeneralProtocol]
}

sealed trait GeneralQuery extends GeneralProtocol with AccessControl

sealed trait AuctionQuery extends GeneralQuery {
  def auctionName: String
}

sealed trait LotQuery extends AuctionQuery {
  def lotName: String
}

// query

final case class CreateAuction(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    owner: String,
    auctionName: String,
    startTime: Instant,
    endTime: Instant
) extends AuctionQuery

final case class CreateLot(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    auctionName: String,
    title: String,
    lotName: String
) extends LotQuery

final case class PlaceBid(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    auctionName: String,
    lotName: String,
    bid: Bid
) extends LotQuery

final case class GetLotData(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    auctionName: String,
    lotName: String
) extends LotQuery

final case class GetAuctionData(
    override val sender: ActorRef[GeneralProtocol],
    override val userId: String,
    override val auctionName: String
) extends AuctionQuery

final case class GetAuctionState(
    override val sender: ActorRef[GeneralProtocol],
    override val userId: String,
    override val auctionName: String
) extends AuctionQuery

final case class AlterAuction(
    override val sender: ActorRef[GeneralProtocol],
    override val userId: String,
    override val auctionName: String,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
) extends AuctionQuery

final case class SetAuctionState(
    override val sender: ActorRef[GeneralProtocol],
    override val userId: String,
    override val auctionName: String,
    state: AuctionState
) extends AuctionQuery

// response

final case class LotData(
    sender: ActorRef[GeneralProtocol],
    name: String,
    description: String,
    maybeBid: Option[Bid] = None
) extends GeneralProtocol

final case class MessageRejected(sender: ActorRef[GeneralProtocol], msg: String)
    extends GeneralProtocol

final case class AuctionData(
    override val sender: ActorRef[GeneralProtocol],
    owner: String,
    title: String,
    startTime: Instant,
    endTime: Instant,
    lots: List[String]
) extends GeneralProtocol

final case class AuctionStateMessage(
    override val sender: ActorRef[GeneralProtocol],
    name: String,
    state: AuctionState
) extends GeneralProtocol
