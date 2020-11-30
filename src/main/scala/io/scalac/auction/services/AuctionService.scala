package io.scalac.auction.services

import java.time.Instant

import io.scalac.auction.models.{Auction, Lot}
import io.scalac.auction.protocols.ErrorProtocol

import scala.concurrent.Future

trait AuctionService {

  def createAuction(userId: String, owner: String, auctionName: String, startTime: Instant, endTime: Instant): Future[Either[ErrorProtocol, Auction]]
  def createAuction(userId: String, owner: String, auction: Auction): Future[Either[ErrorProtocol, Auction]]

  def getAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, Auction]]

  def getAuctions(userId: String): Future[Either[ErrorProtocol, List[Auction]]]

  def editAuction(userId: String, auctionName: String, startTime: Option[Instant], endTime: Option[Instant]): Future[Either[ErrorProtocol, Auction]]

  def startAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, Unit]]

  def endAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, Unit]]

  def createLot(userId: String, auctionName: String, lotName: String, description: String): Future[Either[ErrorProtocol, Lot]]
  def createLot(userId: String, auctionName: String, lot: Lot): Future[Either[ErrorProtocol, Lot]]

  def bidOnLot(userId: String, auctionName: String, lotName: String, amount: Double): Future[Either[ErrorProtocol, Boolean]]

  def getLotsFromAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, List[Lot]]]

  def getLotByAuctionAndName(userId: String, auctionName: String, lotName: String): Future[Either[ErrorProtocol, Lot]]

}
