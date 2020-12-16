package io.scalac.auction.services

import java.time.Instant

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.scalac.auction.json.CirceCodecs._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.scalac.auction.models.{Auction, Lot}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class AuctionHttpService(auctionService: AuctionService) {

  def updateAuction(id: String, auctionName: String): Route = {
    extractStrictEntity(1 second) { entity =>
      if (entity.contentType != ContentTypes.`application/json`) {
        complete(StatusCodes.BadRequest, "application/json expected")
      } else {
        import io.circe.parser._
        parse(entity.data.utf8String) match {
          case Right(json) =>
            val startTime = json.hcursor
              .downField("startTime")
              .success
              .map(_.as[Instant])
              .flatMap(_.toOption)
            val endTime = json.hcursor
              .downField("endTime")
              .success
              .map(_.as[Instant])
              .flatMap(_.toOption)
            onSuccess(
              auctionService.editAuction(id, auctionName, startTime, endTime)
            ) {
              case Right(auction) =>
                complete(StatusCodes.OK, auction)
              case Left(e) =>
                complete(e.code, e.msg)
            }
          case Left(_) =>
            complete(StatusCodes.BadRequest, "Malformed request")
        }
      }
    }
  }

  def getLot(id: String, auctionName: String, lotName: String): Route =
    onSuccess(auctionService.getLotByAuctionAndName(id, auctionName, lotName)) {
      case Right(lot) =>
        complete(StatusCodes.OK, lot)
      case Left(ex) =>
        complete(ex.code, ex.msg)
    }

  def getAuction(id: String, auctionName: String): Route = {
    onSuccess(auctionService.getAuction(id, auctionName)) {
      case Right(auction) =>
        complete(StatusCodes.OK, auction)
      case Left(er) =>
        complete(er.code, er.msg)
    }
  }

  def createAuction(id: String): Route = {
    entity(as[Auction]) { auction =>
      onSuccess(auctionService.createAuction(id, id, auction)) {
        case Right(auction) =>
          complete(StatusCodes.OK, auction)
        case Left(ex) =>
          complete(ex.code, ex.msg)
      }
    }
  }

  def getAuctions(id: String): Route = {
    onSuccess(auctionService.getAuctions(id)) {
      case Right(auctions) =>
        complete(StatusCodes.OK, auctions)
      case Left(e) =>
        complete(e.code, e.msg)
    }
  }

  def startAuction(id: String, auctionName: String): Route = {
    onSuccess(auctionService.startAuction(id, auctionName)) {
      case Right(()) =>
        complete(StatusCodes.OK)
      case Left(e) =>
        complete(e.code, e.msg)
    }
  }

  def endAuction(id: String, auctionName: String): Route = {
    onSuccess(auctionService.endAuction(id, auctionName)) {
      case Right(()) =>
        complete(StatusCodes.OK)
      case Left(e) =>
        complete(e.code, e.msg)
    }
  }

  def createLot(id: String, auctionName: String): Route = {
    entity(as[Lot]) { lot =>
      onSuccess(auctionService.createLot(id, auctionName, lot)) {
        case Right(lot) =>
          complete(StatusCodes.OK, lot)
        case Left(e) =>
          complete(e.code, e.msg)
      }
    }
  }

  def bidOnLot(id: String, auctionName: String, lotName: String): Route = {
    parameters("amount".as[Double]) { amount =>
      onSuccess(auctionService.bidOnLot(id, auctionName, lotName, amount)) {
        case Right(true) =>
          complete(StatusCodes.OK, "Bid successful")
        case Right(false) =>
          complete(StatusCodes.OK, "Bid too low")
        case Left(e) =>
          complete(e.code, e.msg)
      }
    }
  }

}
