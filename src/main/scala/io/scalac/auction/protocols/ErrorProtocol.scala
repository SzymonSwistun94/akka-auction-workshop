package io.scalac.auction.protocols

import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.{StatusCode, StatusCodes}

sealed class ErrorProtocol(override val sender: ActorRef[GeneralProtocol], val code: StatusCode, val msg: String) extends GeneralProtocol(sender)

case class InvalidActorState(override val sender: ActorRef[GeneralProtocol])
  extends ErrorProtocol(sender, StatusCodes.BadRequest, "Invalid request")

case class InvalidStateTransition(override val sender: ActorRef[GeneralProtocol])
  extends ErrorProtocol(sender, StatusCodes.BadRequest, "Invalid request")

case class LotAlreadyExists(override val sender: ActorRef[GeneralProtocol], name: String)
  extends ErrorProtocol(sender, StatusCodes.BadRequest, "Lot already exists")

case class LotNotFound(override val sender: ActorRef[GeneralProtocol], name: String)
  extends ErrorProtocol(sender, StatusCodes.NotFound, "Lot not found")

case class AuctionAlreadyExists(override val sender: ActorRef[GeneralProtocol], name: String)
  extends ErrorProtocol(sender, StatusCodes.BadRequest, "Auction already exists")

case class AuctionNotFound(override val sender: ActorRef[GeneralProtocol], name: String)
  extends ErrorProtocol(sender, StatusCodes.NotFound, "Auction not found")

case class AccessDenied(override val sender: ActorRef[GeneralProtocol])
  extends ErrorProtocol(sender, StatusCodes.Forbidden, "Access denied")