package io.scalac.auction.protocols

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

sealed trait LotProtocol extends GeneralProtocol

// query

final case class GetLotState(sender: ActorRef[GeneralProtocol])
    extends LotProtocol

final case class AlterLot(
    sender: ActorRef[GeneralProtocol],
    maybeTitle: Option[String] = None,
    maybeDescription: Option[String] = None
) extends LotProtocol

final case class SetLotState(sender: ActorRef[GeneralProtocol], state: LotState)
    extends LotProtocol

// response

final case class LotStateMessage(
    sender: ActorRef[GeneralProtocol],
    state: LotState
) extends LotProtocol

final case class BidSuccess(
    sender: ActorRef[GeneralProtocol],
    lotName: String,
    bid: Bid
) extends LotProtocol

final case class BidTooLow(sender: ActorRef[GeneralProtocol], lotName: String)
    extends LotProtocol

// states

sealed trait LotState extends Product with Serializable

object LotState {

  /** [[Closed]] lot state
    *
    * Legal transitions: [[InPreview]], [[Open]]
    */
  final case object Closed extends LotState

  /** [[InPreview]] lot state
    *
    * Legal transitions: [[Closed]], [[Open]]
    */
  final case object InPreview extends LotState {
    val allowedTransitions = Set(Closed, Open)
  }

  /** [[Open]] lot state
    *
    * Legal transitions: [[Finished]]
    */
  final case object Open extends LotState {
    val allowedTransitions = Set(Finished)
  }

  /** [[Finished]] lot state
    *
    * Legal transitions: ()
    */
  final case object Finished extends LotState
}
