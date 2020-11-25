package io.scalac.auction.protocols

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

sealed class LotProtocol(sender: ActorRef[GeneralProtocol]) extends GeneralProtocol(sender)

// query

case class GetLotData(override val sender: ActorRef[GeneralProtocol]) extends LotProtocol(sender)
case class GetLotState(override val sender: ActorRef[GeneralProtocol]) extends LotProtocol(sender)

case class AlterLot(override val sender: ActorRef[GeneralProtocol], maybeTitle: Option[String] = None, maybeDescription: Option[String] = None) extends LotProtocol(sender)
case class SetState(override val sender: ActorRef[GeneralProtocol], state: LotState.Value) extends LotProtocol(sender)
case class PlaceBid(override val sender: ActorRef[GeneralProtocol], bid: Bid) extends LotProtocol(sender)

// response

case class LotData(override val sender: ActorRef[GeneralProtocol], name: String, description: String, maybeBid: Option[Bid] = None) extends LotProtocol(sender)
case class LotStateMessage(override val sender: ActorRef[GeneralProtocol], state: LotState.Value) extends LotProtocol(sender)

case class BidSuccess(override val sender: ActorRef[GeneralProtocol], bid: Bid) extends LotProtocol(sender)
case class BidFailure(override val sender: ActorRef[GeneralProtocol]) extends LotProtocol(sender)

object LotState extends Enumeration {
  type LotState = Value
  val CLOSED, IN_PREVIEW, OPEN, FINISHED = Value
}