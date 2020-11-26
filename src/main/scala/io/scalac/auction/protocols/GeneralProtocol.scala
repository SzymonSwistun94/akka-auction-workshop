package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

abstract class GeneralProtocol(val sender: ActorRef[GeneralProtocol])

sealed class GeneralQuery(override val sender: ActorRef[GeneralProtocol], override val userId: String) extends GeneralProtocol(sender) with AccessControl

sealed class AuctionQuery(override val sender: ActorRef[GeneralProtocol], override val userId: String, val auctionName: String) extends GeneralQuery(sender, userId)

sealed class LotQuery(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String, val lotName: String) extends AuctionQuery(sender, userId, auctionName)

// query

case class CreateAuction(override val sender: ActorRef[GeneralProtocol], override val userId: String, owner: String, override val auctionName: String, startTime: Instant, endTime: Instant) extends AuctionQuery(sender, userId, auctionName)

case class CreateLot(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String, override val lotName: String, description: String) extends LotQuery(sender, userId, auctionName, lotName)

case class PlaceBid(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String, override val lotName: String, bid: Bid) extends LotQuery(sender, userId, auctionName, lotName)

case class GetLotData(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String, override val lotName: String) extends LotQuery(sender, userId, auctionName, lotName)

case class GetAuctionData(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String) extends AuctionQuery(sender, userId, auctionName)

case class GetAuctionState(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String) extends AuctionQuery(sender, userId, auctionName)

case class AlterAuction(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String, startTime: Option[Instant] = None, endTime: Option[Instant] = None) extends AuctionQuery(sender, userId, auctionName)

case class SetAuctionState(override val sender: ActorRef[GeneralProtocol], override val userId: String, override val auctionName: String, state: AuctionState.Value) extends AuctionQuery(sender, userId, auctionName)

// response

case class LotData(override val sender: ActorRef[GeneralProtocol], name: String, description: String, maybeBid: Option[Bid] = None) extends GeneralProtocol(sender)

case class MessageRejected(override val sender: ActorRef[GeneralProtocol], msg: String) extends GeneralProtocol(sender)

case class AuctionData(override val sender: ActorRef[GeneralProtocol], owner: String, title: String, startTime: Instant, endTime: Instant, lots: List[String]) extends GeneralProtocol(sender)

case class AuctionStateMessage(override val sender: ActorRef[GeneralProtocol], name: String, state: AuctionState.Value) extends GeneralProtocol(sender)