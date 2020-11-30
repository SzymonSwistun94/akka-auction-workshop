package io.scalac.auction.protocols


object AuctionState extends Enumeration {
  type AuctionState = Value

  val UNSCHEDULED = Value("UNSCHEDULED") // -> SCHEDULED, IN_PREVIEW, IN_PROGRESS
  val SCHEDULED = Value("SCHEDULED")     // -> IN_PREVIEW, IN_PROGRESS
  val IN_PREVIEW = Value("IN_PREVIEW")   // -> SCHEDULED, IN_PROGRESS
  val IN_PROGRESS = Value("IN_PROGRESS") // -> NEAD_END, FINISHED
  val NEAR_END = Value("NEAR_END")       // -> FINISHED
  val FINISHED = Value("FINISHED")       // -> ()
}
