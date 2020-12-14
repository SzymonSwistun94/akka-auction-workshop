package io.scalac.auction.protocols

import akka.actor.typed.ActorRef

sealed trait ErrorProtocol extends GeneralProtocol

final case class InvalidActorState(override val sender: ActorRef[GeneralProtocol]) extends ErrorProtocol
final case class InvalidStateTransition(override val sender: ActorRef[GeneralProtocol]) extends ErrorProtocol
final case class LotAlreadyExists(override val sender: ActorRef[GeneralProtocol], name: String) extends ErrorProtocol
final case class LotNotFound(override val sender: ActorRef[GeneralProtocol], name: String) extends ErrorProtocol
final case class AccessDenied(override val sender: ActorRef[GeneralProtocol]) extends ErrorProtocol
