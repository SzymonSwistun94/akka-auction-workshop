package io.scalac.auction.protocols

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

sealed trait LotProtocol extends GeneralProtocol

// query

final case class GetLotState(override val sender: ActorRef[GeneralProtocol]) extends LotProtocol

final case class AlterLot(override val sender: ActorRef[GeneralProtocol], maybeTitle: Option[String] = None, maybeDescription: Option[String] = None) extends LotProtocol
final case class SetLotState(override val sender: ActorRef[GeneralProtocol], state: LotState) extends LotProtocol

// response

final case class LotStateMessage(override val sender: ActorRef[GeneralProtocol], state: LotState) extends LotProtocol

final case class BidSuccess(override val sender: ActorRef[GeneralProtocol], lotName: String, bid: Bid) extends LotProtocol
final case class BidTooLow(override val sender: ActorRef[GeneralProtocol], lotName: String) extends LotProtocol

sealed abstract class LotState(val legalStates: Set[LotState]) extends Product with Serializable
object LotState {

  final case object Closed extends LotState(Set(InPreview, Open))
  final case object InPreview extends LotState(Set(Closed, Open))
  final case object Open extends LotState(Set(Finished))
  final case object Finished extends LotState(Set())

  val values = Set(Closed, InPreview, Open, Finished)
}