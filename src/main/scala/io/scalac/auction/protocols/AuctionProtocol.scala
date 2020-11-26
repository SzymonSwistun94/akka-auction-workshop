package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef

sealed class AuctionProtocol(sender: ActorRef[GeneralProtocol]) extends GeneralProtocol(sender)
sealed class AuctionQuery(sender: ActorRef[GeneralProtocol], override val userId: String) extends AuctionProtocol(sender) with AccessControl

// query

case class GetAuctionData(override val sender: ActorRef[GeneralProtocol], override val userId: String) extends AuctionQuery(sender, userId)
case class GetAuctionState(override val sender: ActorRef[GeneralProtocol], override val userId: String) extends AuctionQuery(sender, userId)
case class AlterAuction(override val sender: ActorRef[GeneralProtocol], override val userId: String, title: Option[String] = None, startTime: Option[Instant] = None, endTime: Option[Instant] = None) extends AuctionQuery(sender, userId)
case class SetAuctionState(override val sender: ActorRef[GeneralProtocol], override val userId: String, state: AuctionState.Value) extends AuctionQuery(sender, userId)

// response

case class AuctionData(override val sender: ActorRef[GeneralProtocol], owner: String, title: String, startTime: Instant, endTime: Instant, lots: List[String]) extends AuctionProtocol(sender)
case class AuctionStateMessage(override val sender: ActorRef[GeneralProtocol], state: AuctionState.Value) extends AuctionProtocol(sender)
case class LotCreated(override val sender: ActorRef[GeneralProtocol], lotTitle: String) extends AuctionProtocol(sender)

object AuctionState extends Enumeration {
  type AuctionState = Value

  val UNSCHEDULED, // -> SCHEDULED, IN_PREVIEW, IN_PROGRESS
  SCHEDULED, // -> IN_PREVIEW, IN_PROGRESS
  IN_PREVIEW, // -> SCHEDULED, IN_PROGRESS
  IN_PROGRESS, // -> NEAD_END, FINISHED
  NEAR_END, // -> FINISHED
  FINISHED // -> ()
  = Value
}
