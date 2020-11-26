package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

abstract class GeneralProtocol(val sender: ActorRef[GeneralProtocol])
sealed class GeneralQuery(override val sender: ActorRef[GeneralProtocol], override val userId: String) extends GeneralProtocol(sender) with AccessControl

// query

case class CreateAuction(override val sender: ActorRef[GeneralProtocol], override val userId: String, owner: String, title: String, startTime: Instant, endTime: Instant) extends GeneralQuery(sender, userId)

case class CreateLot(override val sender: ActorRef[GeneralProtocol], override val userId: String, title: String, description: String) extends GeneralQuery(sender, userId)

case class PlaceBid(override val sender: ActorRef[GeneralProtocol], override val userId: String, lotName: String, bid: Bid) extends GeneralQuery(sender, userId)

case class GetLotData(override val sender: ActorRef[GeneralProtocol], override val userId: String, name: String) extends GeneralQuery(sender, userId)

// response

case class LotData(override val sender: ActorRef[GeneralProtocol], name: String, description: String, maybeBid: Option[Bid] = None) extends GeneralProtocol(sender)

case class MessageRejected(override val sender: ActorRef[GeneralProtocol], msg: String) extends GeneralProtocol(sender)