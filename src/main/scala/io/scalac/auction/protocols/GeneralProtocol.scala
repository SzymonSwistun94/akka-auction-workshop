package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef
import io.scalac.auction.models.Bid

trait GeneralProtocol {
  def sender: ActorRef[GeneralProtocol]
}

sealed trait GeneralQuery extends GeneralProtocol with AccessControl

// query

final case class CreateAuction(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    owner: String,
    title: String,
    startTime: Instant,
    endTime: Instant
) extends GeneralQuery

final case class CreateLot(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    title: String,
    description: String
) extends GeneralQuery

final case class PlaceBid(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    lotName: String,
    bid: Bid
) extends GeneralQuery

final case class GetLotData(
    sender: ActorRef[GeneralProtocol],
    userId: String,
    name: String
) extends GeneralQuery

// response

final case class LotData(
    sender: ActorRef[GeneralProtocol],
    name: String,
    description: String,
    maybeBid: Option[Bid] = None
) extends GeneralProtocol

final case class MessageRejected(sender: ActorRef[GeneralProtocol], msg: String)
    extends GeneralProtocol
