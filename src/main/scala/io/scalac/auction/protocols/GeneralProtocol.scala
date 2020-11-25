package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef

abstract class GeneralProtocol(val sender: ActorRef[GeneralProtocol])

case class MessageRejected(override val sender: ActorRef[GeneralProtocol], msg: String) extends GeneralProtocol(sender)
case class CreateAuction(override val sender: ActorRef[GeneralProtocol], name: String, startTime: Instant, endTime: Instant) extends GeneralProtocol(sender)
case class CreateLot(override val sender: ActorRef[GeneralProtocol], name: String, description: String) extends GeneralProtocol(sender)