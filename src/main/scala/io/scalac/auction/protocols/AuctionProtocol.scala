package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef

sealed trait AuctionProtocol extends GeneralProtocol

sealed trait AuctionQuery extends AuctionProtocol with AccessControl

// query

final case class GetAuctionData(override val sender: ActorRef[GeneralProtocol], override val userId: String) extends AuctionQuery

final case class GetAuctionState(override val sender: ActorRef[GeneralProtocol], override val userId: String) extends AuctionQuery

final case class AlterAuction(override val sender: ActorRef[GeneralProtocol], override val userId: String, title: Option[String] = None, startTime: Option[Instant] = None, endTime: Option[Instant] = None) extends AuctionQuery

final case class SetAuctionState(override val sender: ActorRef[GeneralProtocol], override val userId: String, state: AuctionState) extends AuctionQuery

// response

final case class AuctionData(override val sender: ActorRef[GeneralProtocol], owner: String, title: String, startTime: Instant, endTime: Instant, lots: List[String]) extends AuctionProtocol

final case class AuctionStateMessage(override val sender: ActorRef[GeneralProtocol], state: AuctionState) extends AuctionProtocol

final case class LotCreated(override val sender: ActorRef[GeneralProtocol], lotTitle: String) extends AuctionProtocol

sealed abstract class AuctionState(val legalStates: Set[AuctionState]) extends Product with Serializable

object AuctionState {

  final case object Unscheduled extends AuctionState(Set(Scheduled, InPreview, InProgress))

  final case object Scheduled extends AuctionState(Set(InPreview, InProgress))

  final case object InPreview extends AuctionState(Set(Scheduled, InProgress))

  final case object InProgress extends AuctionState(Set(NearEnd, Finished))

  final case object NearEnd extends AuctionState(Set(Finished))

  final case object Finished extends AuctionState(Set())

  val values = Set(Unscheduled, Scheduled, InPreview, InProgress, NearEnd, Finished)
}
