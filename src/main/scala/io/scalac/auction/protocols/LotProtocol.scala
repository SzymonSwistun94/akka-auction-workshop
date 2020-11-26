package io.scalac.auction.protocols

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

sealed class LotProtocol(sender: ActorRef[GeneralProtocol]) extends GeneralProtocol(sender)

// query

case class GetLotState(override val sender: ActorRef[GeneralProtocol]) extends LotProtocol(sender)

case class AlterLot(override val sender: ActorRef[GeneralProtocol], maybeTitle: String, maybeDescription: Option[String] = None) extends LotProtocol(sender)
case class SetLotState(override val sender: ActorRef[GeneralProtocol], state: LotState.Value) extends LotProtocol(sender)

// response

case class LotStateMessage(override val sender: ActorRef[GeneralProtocol], name: String, state: LotState.Value) extends LotProtocol(sender)

case class BidSuccess(override val sender: ActorRef[GeneralProtocol], lotName: String, bid: Bid) extends LotProtocol(sender)
case class BidFailure(override val sender: ActorRef[GeneralProtocol], lotName: String) extends LotProtocol(sender)

object LotState extends Enumeration {
  type LotState = Value

  val CLOSED, // -> IN_PREVIEW, OPEN
  IN_PREVIEW, // -> CLOSED, OPEN
  OPEN,       // -> FINISHED
  FINISHED    // -> ()
  = Value
}