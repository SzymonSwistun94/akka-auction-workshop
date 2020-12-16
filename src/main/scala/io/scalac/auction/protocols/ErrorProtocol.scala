package io.scalac.auction.protocols

import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.{StatusCode, StatusCodes}

sealed trait ErrorProtocol extends GeneralProtocol {
  def msg: String
  def code: StatusCode
}

final case class InvalidActorState(sender: ActorRef[GeneralProtocol])
    extends ErrorProtocol {
  val msg = "Invalid actor state"
  val code = StatusCodes.Conflict
}

final case class InvalidStateTransition(sender: ActorRef[GeneralProtocol])
    extends ErrorProtocol {
  val msg = "Invalid state transition"
  val code = StatusCodes.Conflict
}

final case class LotAlreadyExists(
    sender: ActorRef[GeneralProtocol],
    name: String
) extends ErrorProtocol {
  val msg = "Lot already exists"
  val code = StatusCodes.Conflict
}

final case class LotNotFound(sender: ActorRef[GeneralProtocol], name: String)
    extends ErrorProtocol {
  val msg = "Lot not found"
  val code = StatusCodes.NotFound
}

final case class AuctionAlreadyExists(
    override val sender: ActorRef[GeneralProtocol],
    name: String
) extends ErrorProtocol {
  val msg = "Auction already exists"
  val code = StatusCodes.Conflict
}

final case class AuctionNotFound(
    override val sender: ActorRef[GeneralProtocol],
    name: String
) extends ErrorProtocol {
  val msg = "Auction not found"
  val code = StatusCodes.NotFound
}

final case class AccessDenied(sender: ActorRef[GeneralProtocol])
    extends ErrorProtocol {
  val msg = "Access denied"
  val code = StatusCodes.Forbidden
}
