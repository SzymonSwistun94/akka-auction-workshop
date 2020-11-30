package io.scalac.auction.actors

import java.time.Instant

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.protocols._
import io.scalac.auction.utils.SecureUtils._

object AuctionActor {
  def apply(): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case CreateAuction(sender, _, owner, title, startTime, endTime) =>
        sender ! AuctionStateMessage(context.self, title, AuctionState.UNSCHEDULED)
        unscheduled(owner, title, startTime, endTime, Map.empty)
      case msg: GeneralProtocol =>
        msg.sender ! InvalidActorState(context.self)
        Behaviors.same
    }
  }

  def unscheduled(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      checkAccess(owner, message, context) {
        case GetAuctionState(sender, _, _) =>
          sender ! AuctionStateMessage(context.self, title, AuctionState.UNSCHEDULED)
          Behaviors.same
        case GetAuctionData(sender, _, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case createLot: CreateLot =>
          if (lots.keys.exists(_ == createLot.lotName)) {
            createLot.sender ! LotAlreadyExists(context.self, createLot.lotName)
            Behaviors.same
          }
          else {
            val lot = context.spawn(LotActor(), createLot.lotName)
            lot ! createLot
            createLot.sender ! LotStateMessage(context.self, createLot.lotName, LotState.CLOSED)
            unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.lotName -> lot))
          }
        case GetLotData(sender, userId, _, lotName) =>
            lots.get(lotName) match {
              case Some(lot) =>
                lot ! GetLotData(sender, userId, title, lotName)
              case None =>
                sender ! LotNotFound(context.self, lotName)
            }
          Behaviors.same
        case GetLotList(sender, _, _) =>
          sender ! LotList(context.self, title, lots.keys.toList)
          Behaviors.same
        case AlterAuction(sender, _, _, maybeStartTime, maybeEndTime) =>
          val curStartTime = maybeStartTime.getOrElse(startTime)
          val curEndTime = maybeEndTime.getOrElse(endTime)
          sender ! AuctionData(context.self, owner, title, curStartTime, curEndTime, lots.keys.toList)
          unscheduled(owner, title, curStartTime, curEndTime, lots)
        case SetAuctionState(sender, _, _, state) => state match {
          case AuctionState.SCHEDULED =>
            sender ! AuctionStateMessage(context.self, title, state)
            scheduled(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PREVIEW =>
            sender ! AuctionStateMessage(context.self, title, state)
            lots.values.foreach(_ ! SetLotState(sender, LotState.IN_PREVIEW))
            inPreview(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PROGRESS =>
            sender ! AuctionStateMessage(context.self, title, state)
            lots.values.foreach(_ ! SetLotState(sender, LotState.OPEN))
            inProgress(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! InvalidStateTransition(context.self)
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! InvalidActorState(context.self)
          Behaviors.same
      }
    }

  def scheduled(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionState(sender, _, _) =>
          sender ! AuctionStateMessage(context.self, title, AuctionState.SCHEDULED)
          Behaviors.same
        case GetAuctionData(sender, _, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, _, lotName) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, title, lotName)
            case None =>
              sender ! LotNotFound(context.self, lotName)
          }
          Behaviors.same
        case GetLotList(sender, _, _) =>
          sender ! LotList(context.self, title, lots.keys.toList)
          Behaviors.same
        case msg: GeneralProtocol if msg.isInstanceOf[AccessControl] =>
          checkAccess(owner, message, context) {
            case AlterAuction(sender, _, _, maybeStartTime, maybeEndTime) =>
              val curStartTime = maybeStartTime.getOrElse(startTime)
              val curEndTime = maybeEndTime.getOrElse(endTime)
              sender ! AuctionData(context.self, owner, title, curStartTime, curEndTime, lots.keys.toList)
              unscheduled(owner, title, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.lotName)) {
                createLot.sender ! LotAlreadyExists(context.self, createLot.lotName)
                Behaviors.same
              }
              else {
                val lot = context.spawn(LotActor(), createLot.lotName)
                lot ! createLot
                createLot.sender ! LotStateMessage(context.self, createLot.lotName, LotState.CLOSED)
                unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.lotName -> lot))
              }
            case SetAuctionState(sender, _, _, state) => state match {
              case AuctionState.IN_PREVIEW =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.IN_PREVIEW))
                inPreview(owner, title, startTime, endTime, lots)
              case AuctionState.IN_PROGRESS =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.OPEN))
                inProgress(owner, title, startTime, endTime, lots)
              case _ =>
                sender ! InvalidStateTransition(context.self)
                Behaviors.same
            }
            case _ =>
              message.sender ! InvalidActorState(context.self)
              Behaviors.same
          }
        case _ =>
          message.sender ! InvalidActorState(context.self)
          Behaviors.same
      }
    }

  def inPreview(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionState(sender, _, _) =>
          sender ! AuctionStateMessage(context.self, title, AuctionState.IN_PREVIEW)
          Behaviors.same
        case GetAuctionData(sender, _, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, _, lotName) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, title, lotName)
            case None =>
              sender ! LotNotFound(context.self, lotName)
          }
          Behaviors.same
        case GetLotList(sender, _, _) =>
          sender ! LotList(context.self, title, lots.keys.toList)
          Behaviors.same
        case msg: GeneralProtocol if msg.isInstanceOf[AccessControl] =>
          checkAccess(owner, message, context) {
            case AlterAuction(sender, _, _, maybeStartTime, maybeEndTime) =>
              val curStartTime = maybeStartTime.getOrElse(startTime)
              val curEndTime = maybeEndTime.getOrElse(endTime)
              sender ! AuctionData(context.self, owner, title, curStartTime, curEndTime, lots.keys.toList)
              unscheduled(owner, title, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.lotName)) {
                createLot.sender ! LotAlreadyExists(context.self, createLot.lotName)
                Behaviors.same
              }
              else {
                val lot = context.spawn(LotActor(), createLot.lotName)
                lot ! createLot
                lot ! SetLotState(null, LotState.IN_PREVIEW)
                createLot.sender ! LotStateMessage(context.self, createLot.lotName, LotState.IN_PREVIEW)
                unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.lotName -> lot))
              }
            case SetAuctionState(sender, _, _, state) => state match {
              case AuctionState.SCHEDULED =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.CLOSED))
                scheduled(owner, title, startTime, endTime, lots)
              case AuctionState.IN_PROGRESS =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.OPEN))
                inProgress(owner, title, startTime, endTime, lots)
              case _ =>
                sender ! InvalidStateTransition(context.self)
                Behaviors.same
            }
            case _ =>
              message.sender ! InvalidActorState(context.self)
              Behaviors.same
          }
        case _ =>
          message.sender ! InvalidActorState(context.self)
          Behaviors.same
      }
    }

  def inProgress(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionState(sender, _, _) =>
          sender ! AuctionStateMessage(context.self, title, AuctionState.IN_PROGRESS)
          Behaviors.same
        case GetAuctionData(sender, _, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, _, lotName) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! GetLotData(sender, title, lotName, userId)
            case None =>
              sender ! LotNotFound(context.self, lotName)
          }
          Behaviors.same
        case GetLotList(sender, _, _) =>
          sender ! LotList(context.self, title, lots.keys.toList)
          Behaviors.same
        case PlaceBid(sender, userId, _, lotName, bid) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! PlaceBid(sender, userId, title, lotName, bid)
            case None =>
              sender ! LotNotFound(context.self, lotName)
          }
          Behaviors.same
        case SetAuctionState(sender, userId, _, state) =>
          state match {
            case _ if userId != owner =>
              sender ! AccessDenied(context.self)
              Behaviors.same
            case AuctionState.NEAR_END =>
              sender ! AuctionStateMessage(context.self, title, state)
              nearEnd(owner, title, startTime, endTime, lots)
            case AuctionState.FINISHED =>
              sender ! AuctionStateMessage(context.self, title, state)
              lots.values.foreach(_ ! SetLotState(sender, LotState.FINISHED))
              finished(owner, title, startTime, endTime, lots)
            case _ =>
              sender ! InvalidStateTransition(context.self)
              Behaviors.same
          }

        case msg: GeneralProtocol =>
          msg.sender ! InvalidActorState(context.self)
          Behaviors.same
      }
    }

  def nearEnd(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionState(sender, _, _) =>
          sender ! AuctionStateMessage(context.self, title, AuctionState.NEAR_END)
          Behaviors.same
        case GetAuctionData(sender, _, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, _, lotName) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, title, lotName)
            case None =>
              sender ! LotNotFound(context.self, lotName)
          }
          Behaviors.same
        case GetLotList(sender, _, _) =>
          sender ! LotList(context.self, title, lots.keys.toList)
          Behaviors.same
        case SetAuctionState(sender, userId, _, state) => state match {
          case _ if userId != owner =>
            sender ! AccessDenied(context.self)
            Behaviors.same
          case AuctionState.FINISHED =>
            sender ! AuctionStateMessage(context.self, title, state)
            lots.values.foreach(_ ! SetLotState(sender, LotState.FINISHED))
            finished(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! InvalidStateTransition(context.self)
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! InvalidActorState(context.self)
          Behaviors.same
      }
    }

  def finished(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetAuctionState(sender, _, _) =>
          sender ! AuctionStateMessage(context.self, title, AuctionState.FINISHED)
          Behaviors.same
        case GetAuctionData(sender, _, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, _, lotName) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, title, lotName)
            case None =>
              sender ! LotNotFound(context.self, lotName)
          }
          Behaviors.same
        case GetLotList(sender, _, _) =>
          sender ! LotList(context.self, title, lots.keys.toList)
          Behaviors.same
        case SetAuctionState(sender, userId, _, state) => state match {
          case _ if userId != owner =>
            sender ! AccessDenied(context.self)
            Behaviors.same
          case _ =>
            sender ! InvalidStateTransition(context.self)
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          msg.sender ! InvalidActorState(context.self)
          Behaviors.same
      }
    }

}
