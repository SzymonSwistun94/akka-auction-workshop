package io.scalac.auction.protocols

import java.time.Instant

import akka.actor.typed.ActorRef


object AuctionState extends Enumeration {
  type AuctionState = Value

  val UNSCHEDULED, // -> SCHEDULED, IN_PREVIEW, IN_PROGRESS
  SCHEDULED, // -> IN_PREVIEW, IN_PROGRESS
  IN_PREVIEW, // -> SCHEDULED, IN_PROGRESS
  IN_PROGRESS, // -> NEAD_END, FINISHED
  NEAR_END, // -> FINISHED
  FINISHED // -> ()
  = Value
}
