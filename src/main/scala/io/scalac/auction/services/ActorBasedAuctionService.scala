package io.scalac.auction.services

import java.time.Instant

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import io.scalac.auction.models.{Auction, Bid, Lot}
import io.scalac.auction.protocols._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import io.scalac.auction.actors.OverseerActor

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

object ActorBasedAuctionService {
  def create(implicit system: ActorSystem[_], ctx: ActorContext[GeneralProtocol]): ActorBasedAuctionService = {
    val overseerActor = ctx.spawn(OverseerActor(), "overseer")
    new ActorBasedAuctionService(overseerActor)
  }
}

class ActorBasedAuctionService(overseerActor: ActorRef[GeneralProtocol])(implicit system: ActorSystem[_]) extends AuctionService {
  implicit val ec: ExecutionContext = system.executionContext
  implicit val timeout: Timeout = 3.seconds

  override def createAuction(userId: String, owner: String, auctionName: String, startTime: Instant, endTime: Instant): Future[Either[ErrorProtocol, Auction]] =
    overseerActor.ask(CreateAuction(_, userId, owner, auctionName, startTime, endTime))
    .flatMap {
      case _: AuctionStateMessage =>
        getAuction(userId, auctionName)
      case e: AuctionAlreadyExists =>
        Future.successful(Left(e))
    }

  override def createAuction(userId: String, owner: String, auction: Auction): Future[Either[ErrorProtocol, Auction]] =
    createAuction(userId, owner, auction.name, auction.startTime, auction.endTime)

  override def getAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, Auction]] =
    overseerActor.ask(GetAuctionData(_, userId, auctionName))
      .flatMap {
        case ad: AuctionData =>
          overseerActor.ask(GetAuctionState(_, userId, auctionName)).map {
            case AuctionStateMessage(_, _, state) =>
              Right(Auction(auctionName, ad.startTime, ad.endTime, ad.lots, state.toString))
            case e: ErrorProtocol =>
              Left(e)
          }
        case e: AccessDenied =>
          Future.successful(Left(e))
      }

  override def getAuctions(userId: String): Future[Either[ErrorProtocol, List[Auction]]] =
    overseerActor.ask(GetAuctionsList(_, userId)).flatMap {
      case AuctionsList(_, auctions) =>
        println(auctions.mkString(", "))
        Future.sequence(auctions.map(getAuction(userId, _).map(_.toOption))).map(l => Right(l.flatten))
      case e: ErrorProtocol =>
        Future.successful(Left(e))
    }

  override def editAuction(userId: String, auctionName: String, startTime: Option[Instant], endTime: Option[Instant]): Future[Either[ErrorProtocol, Auction]] = {
    overseerActor.ask[GeneralProtocol](AlterAuction(_, userId, auctionName, startTime, endTime))
      .flatMap {
        case AuctionData(_, _, _, start, end, lots) =>
          overseerActor.ask[GeneralProtocol](GetAuctionState(_, userId, auctionName)).map {
            case AuctionStateMessage(_, _, state) =>
              Right(Auction(auctionName, start, end, lots, state.toString))
            case e: ErrorProtocol =>
              Left(e)
          }
        case e: ErrorProtocol =>
          Future.successful(Left(e))
      }
  }

  override def startAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, Unit]] =
    overseerActor.ask[GeneralProtocol](SetAuctionState(_, userId, auctionName, AuctionState.IN_PROGRESS))
      .map {
        case AuctionStateMessage(_, _, AuctionState.IN_PROGRESS) =>
          Right()
        case e: ErrorProtocol =>
          Left(e)
      }

  override def endAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, Unit]] =
    overseerActor.ask[GeneralProtocol](SetAuctionState(_, userId, auctionName, AuctionState.IN_PROGRESS))
      .map {
        case AuctionStateMessage(_, _, AuctionState.FINISHED) =>
          Right()
        case e: ErrorProtocol =>
          Left(e)
      }

  override def createLot(userId: String, auctionName: String, lotName: String, description: String): Future[Either[ErrorProtocol, Lot]] =
    overseerActor.ask[GeneralProtocol](CreateLot(_, userId, auctionName, lotName, description))
      .flatMap {
        case LotStateMessage(_, _, LotState.CLOSED) =>
          getLotByAuctionAndName(userId, auctionName, lotName)
        case e: ErrorProtocol =>
          Future.successful(Left(e))
      }

  override def createLot(userId: String, auctionName: String, lot: Lot): Future[Either[ErrorProtocol, Lot]] =
    createLot(userId, auctionName, lot.name, lot.description)

  override def bidOnLot(userId: String, auctionName: String, lotName: String, amount: Double): Future[Either[ErrorProtocol, Boolean]] = {
    overseerActor.ask[GeneralProtocol](PlaceBid(_, userId, auctionName, lotName, Bid(userId, amount)))
      .map {
        case _: BidSuccess =>
          Right(true)
        case _: BidFailure =>
          Right(false)
        case e: ErrorProtocol =>
          Left(e)
      }
  }

  override def getLotsFromAuction(userId: String, auctionName: String): Future[Either[ErrorProtocol, List[Lot]]] =
    overseerActor.ask[GeneralProtocol](GetLotList(_, userId, auctionName))
    .flatMap {
      case LotList(_, _, lots) =>
        Future.sequence(lots.map(getLotByAuctionAndName(userId, auctionName, _))
        .map(_.map(_.toOption))
        ).map(l => Right(l.flatten))
      case e: ErrorProtocol =>
        Future.successful(Left(e))
    }

  override def getLotByAuctionAndName(userId: String, auctionName: String, lotName: String): Future[Either[ErrorProtocol, Lot]] =
    overseerActor.ask[GeneralProtocol](GetLotData(_, userId, auctionName, lotName))
      .map {
        case LotData(_, _, _, description, maybeBid) =>
          Right(Lot(lotName, description, maybeBid.map(_.value)))
        case e: ErrorProtocol =>
          Left(e)
      }
}
