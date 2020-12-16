package io.scalac.auction.models

import java.time.Instant

case class Auction(
    name: String,
    startTime: Instant,
    endTime: Instant,
    lots: List[String] = Nil,
    state: String = "UNSCHEDULED"
)
case class Lot(
    auctionName: String,
    lotName: String,
    description: String,
    highestBid: Option[Double] = None
)
