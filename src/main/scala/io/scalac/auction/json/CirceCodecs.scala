package io.scalac.auction.json

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.scalac.auction.models.{Auction, Lot}

object CirceCodecs {
  implicit val config = Configuration.default.withDefaults

  implicit val auctionCodec = deriveConfiguredCodec[Auction]
  implicit val lotCodec = deriveConfiguredCodec[Lot]
}
