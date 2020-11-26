package io.scalac.auction.protocols

import akka.actor.typed.ActorRef

sealed class ErrorProtocol(override val sender: ActorRef[GeneralProtocol]) extends GeneralProtocol(sender)

case class InvalidActorState(override val sender: ActorRef[GeneralProtocol]) extends ErrorProtocol(sender)
case class InvalidStateTransition(override val sender: ActorRef[GeneralProtocol]) extends ErrorProtocol(sender)
case class LotAlreadyExists(override val sender: ActorRef[GeneralProtocol], name: String) extends ErrorProtocol(sender)
case class LotNotFound(override val sender: ActorRef[GeneralProtocol], name: String) extends ErrorProtocol(sender)
case class AccessDenied(override val sender: ActorRef[GeneralProtocol]) extends ErrorProtocol(sender)