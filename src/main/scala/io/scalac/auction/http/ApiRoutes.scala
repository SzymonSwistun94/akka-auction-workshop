package io.scalac.auction.http

import java.time.Instant

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import io.scalac.auction.services.{AuctionHttpService, AuctionService}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import io.scalac.auction.json.CirceCodecs._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.scalac.auction.models.Auction

import scala.concurrent.duration._
import scala.language.postfixOps

class ApiRoutes(auctionService: AuctionService) {
  val auctionHttpService = new AuctionHttpService(auctionService)

  def authenticate(cred: Credentials): Option[String] = cred match {
    case Missing              => None
    case Provided(identifier) => Some(identifier)
  }

  val route = pathPrefix("api") {
    authenticateOAuth2("auctionApi", authenticate) { id =>
      pathPrefix("auction") {
        pathPrefix(Segment) { auctionName =>
          pathPrefix("lot") {
            pathPrefix(Segment) { lotName =>
              pathPrefix("bid") {
                auctionHttpService.bidOnLot(
                  id,
                  auctionName,
                  lotName
                ) // GET /api/auction/<id>/lot/<id>/bid/?amount
              } ~ auctionHttpService.getLot(
                id,
                auctionName,
                lotName
              ) // GET /api/auction/<id>/lot/<id>/
            } ~ post {
              auctionHttpService.createLot(
                id,
                auctionName
              ) // POST /api/auction/<id>/lot/
            }
          } ~ pathPrefix("start") {
            auctionHttpService.startAuction(
              id,
              auctionName
            ) // GET /api/auction/<id>/start/
          } ~ pathPrefix("end") {
            auctionHttpService.endAuction(
              id,
              auctionName
            ) // GET /api/auction/<id>/end/
          } ~ post {
            auctionHttpService.updateAuction(
              id,
              auctionName
            ) // POST /api/auction/<id>/
          } ~ get {
            auctionHttpService.getAuction(
              id,
              auctionName
            ) // GET /api/auction/<id>/
          }
        } ~ get {
          auctionHttpService.getAuctions(id) // GET /api/auction/
        } ~ post {
          auctionHttpService.createAuction(id) //POST /api/auction/
        }
      }
    }
  }
}
