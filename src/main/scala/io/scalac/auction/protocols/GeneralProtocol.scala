package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

abstract class GeneralProtocol(val sender: ActorRef[GeneralProtocol])

case class MessageRejected(override val sender: ActorRef[GeneralProtocol], msg: String) extends GeneralProtocol(sender)
case class CreateAuction(override val sender: ActorRef[GeneralProtocol], owner: String, title: String, startTime: Instant, endTime: Instant) extends GeneralProtocol(sender)
case class CreateLot(override val sender: ActorRef[GeneralProtocol], title: String, description: String) extends GeneralProtocol(sender)
case class PlaceBid(override val sender: ActorRef[GeneralProtocol], lotName: String, bid: Bid) extends GeneralProtocol(sender)