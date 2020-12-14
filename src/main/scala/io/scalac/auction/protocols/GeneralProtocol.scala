package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

trait GeneralProtocol {
  val sender: ActorRef[GeneralProtocol]
}

sealed trait GeneralQuery extends GeneralProtocol with AccessControl

// query

final case class CreateAuction(override val sender: ActorRef[GeneralProtocol], override val userId: String, owner: String, title: String, startTime: Instant, endTime: Instant) extends GeneralQuery

final case class CreateLot(override val sender: ActorRef[GeneralProtocol], override val userId: String, title: String, description: String) extends GeneralQuery

final case class PlaceBid(override val sender: ActorRef[GeneralProtocol], override val userId: String, lotName: String, bid: Bid) extends GeneralQuery

final case class GetLotData(override val sender: ActorRef[GeneralProtocol], override val userId: String, name: String) extends GeneralQuery

// response

final case class LotData(override val sender: ActorRef[GeneralProtocol], name: String, description: String, maybeBid: Option[Bid] = None) extends GeneralProtocol

final case class MessageRejected(override val sender: ActorRef[GeneralProtocol], msg: String) extends GeneralProtocol