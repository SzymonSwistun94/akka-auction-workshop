package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef

sealed trait AuctionProtocol extends GeneralProtocol

sealed trait AuctionQuery extends AuctionProtocol with AccessControl

// query

final case class GetAuctionData(
    sender: ActorRef[GeneralProtocol],
    userId: String
) extends AuctionQuery

final case class GetAuctionState(
    sender: ActorRef[GeneralProtocol],
    userId: String
) extends AuctionQuery

final case class AlterAuction(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    title: Option[String] = None,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
) extends AuctionQuery

final case class SetAuctionState(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    state: AuctionState
) extends AuctionQuery

// response

final case class AuctionData(
    sender: ActorRef[GeneralProtocol],
    owner: String,
    title: String,
    startTime: Instant,
    endTime: Instant,
    lots: List[String]
) extends AuctionProtocol

final case class AuctionStateMessage(
    sender: ActorRef[GeneralProtocol],
    state: AuctionState
) extends AuctionProtocol

final case class LotCreated(sender: ActorRef[GeneralProtocol], lotTitle: String)
    extends AuctionProtocol

// states

sealed trait AuctionState extends Product with Serializable
object AuctionState {

  /** [[Unscheduled]] ayction state
    *
    * Legal transitions: [[Scheduled]], [[InPreview]], [[InProgress]]
    */
  final case object Unscheduled extends AuctionState

  /** [[Scheduled]] ayction state
    *
    * Legal transitions: [[InPreview]], [[InProgress]]
    */
  final case object Scheduled extends AuctionState

  /** [[InPreview]] ayction state
    *
    * Legal transitions: [[Scheduled]], [[InProgress]]
    */
  final case object InPreview extends AuctionState

  /** [[InProgress]] ayction state
    *
    * Legal transitions: [[NearEnd]], [[Finished]]
    */
  final case object InProgress extends AuctionState

  /** [[NearEnd]] ayction state
    *
    * Legal transitions: [[Finished]]
    */
  final case object NearEnd extends AuctionState

  /** [[Finished]] ayction state
    *
    * Legal transitions: ()
    */
  final case object Finished extends AuctionState
}
