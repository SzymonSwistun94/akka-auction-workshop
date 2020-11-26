package io.scalac.auction.actors

import java.time.Instant

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.protocols._

object AuctionActor {
  def apply(): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case CreateAuction(sender, owner, title, startTime, endTime) =>
        sender ! AuctionStateMessage(context.self, AuctionState.SCHEDULED)
        unscheduled(owner, title, startTime, endTime, Map.empty)
      case msg: GeneralProtocol =>
        msg.sender ! MessageRejected(context.self, "Invalid actor state")
        Behaviors.same
    }
  }

  def unscheduled(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionData(sender) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case createLot: CreateLot =>
          if (lots.keys.exists(_ == createLot.title)) {
            createLot.sender ! MessageRejected(context.self, s"Lot with name ${createLot.title} already exists")
            Behaviors.same
          }
          else {
            val lot = context.spawn(LotActor(), createLot.title)
            lot ! createLot
            createLot.sender ! LotCreated(context.self, createLot.title)
            unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.title -> lot))
          }
        case AlterAuction(sender, maybeTitle, maybeStartTime, maybeEndTime) =>
          val curTitle = maybeTitle.getOrElse(title)
          val curStartTime = maybeStartTime.getOrElse(startTime)
          val curEndTime = maybeEndTime.getOrElse(endTime)
          sender ! AuctionData(context.self, owner, curTitle, curStartTime, curEndTime, lots.keys.toList)
          unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
        case SetAuctionState(sender, state) => state match {
          case AuctionState.SCHEDULED =>
            sender ! AuctionStateMessage(context.self, state)
            scheduled(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PREVIEW =>
            sender ! AuctionStateMessage(context.self, state)
            inPreview(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PROGRESS =>
            sender ! AuctionStateMessage(context.self, state)
            inProgress(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! MessageRejected(context.self, "Invalid state transition")
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! MessageRejected(context.self, "Invalid actor state")
          Behaviors.same
      }
    }

  def scheduled(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionData(sender) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case AlterAuction(sender, maybeTitle, maybeStartTime, maybeEndTime) =>
          val curTitle = maybeTitle.getOrElse(title)
          val curStartTime = maybeStartTime.getOrElse(startTime)
          val curEndTime = maybeEndTime.getOrElse(endTime)
          sender ! AuctionData(context.self, owner, curTitle, curStartTime, curEndTime, lots.keys.toList)
          unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
        case createLot: CreateLot =>
          if (lots.keys.exists(_ == createLot.title)) {
            createLot.sender ! MessageRejected(context.self, s"Lot with name ${createLot.title} already exists")
            Behaviors.same
          }
          else {
            val lot = context.spawn(LotActor(), createLot.title)
            lot ! createLot
            createLot.sender ! LotCreated(context.self, createLot.title)
            unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.title -> lot))
          }
        case SetAuctionState(sender, state) => state match {
          case AuctionState.IN_PREVIEW =>
            sender ! AuctionStateMessage(context.self, state)
            inPreview(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PROGRESS =>
            sender ! AuctionStateMessage(context.self, state)
            inProgress(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! MessageRejected(context.self, "Invalid state transition")
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! MessageRejected(context.self, "Invalid actor state")
          Behaviors.same
      }
    }

  def inPreview(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionData(sender) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case AlterAuction(sender, maybeTitle, maybeStartTime, maybeEndTime) =>
          val curTitle = maybeTitle.getOrElse(title)
          val curStartTime = maybeStartTime.getOrElse(startTime)
          val curEndTime = maybeEndTime.getOrElse(endTime)
          sender ! AuctionData(context.self, owner, curTitle, curStartTime, curEndTime, lots.keys.toList)
          unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
        case createLot: CreateLot =>
          if (lots.keys.exists(_ == createLot.title)) {
            createLot.sender ! MessageRejected(context.self, s"Lot with name ${createLot.title} already exists")
            Behaviors.same
          }
          else {
            val lot = context.spawn(LotActor(), createLot.title)
            lot ! createLot
            createLot.sender ! LotCreated(context.self, createLot.title)
            unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.title -> lot))
          }
        case SetAuctionState(sender, state) => state match {
          case AuctionState.SCHEDULED =>
            sender ! AuctionStateMessage(context.self, state)
            scheduled(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PROGRESS =>
            sender ! AuctionStateMessage(context.self, state)
            inProgress(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! MessageRejected(context.self, "Invalid state transition")
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! MessageRejected(context.self, "Invalid actor state")
          Behaviors.same
      }
    }

  def inProgress(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionData(sender) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case PlaceBid(sender, lotName, bid) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! PlaceBid(sender, lotName, bid)
            case None =>
              sender ! MessageRejected(context.self, "Lot not found")
          }
          Behaviors.same
        case SetAuctionState(sender, state) => state match {
          case AuctionState.NEAR_END =>
            sender ! AuctionStateMessage(context.self, state)
            nearEnd(owner, title, startTime, endTime, lots)
          case AuctionState.FINISHED =>
            sender ! AuctionStateMessage(context.self, state)
            finished(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! MessageRejected(context.self, "Invalid state transition")
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! MessageRejected(context.self, "Invalid actor state")
          Behaviors.same
      }
    }

  def nearEnd(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionData(sender) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case SetAuctionState(sender, state) => state match {
          case AuctionState.FINISHED =>
            sender ! AuctionStateMessage(context.self, state)
            finished(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! MessageRejected(context.self, "Invalid state transition")
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! MessageRejected(context.self, "Invalid actor state")
          Behaviors.same
      }
    }

  def finished(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionData(sender) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case SetAuctionState(sender, state) => state match {
          case _ =>
            sender ! MessageRejected(context.self, "Invalid state transition")
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! MessageRejected(context.self, "Invalid actor state")
          Behaviors.same
      }
    }

}
