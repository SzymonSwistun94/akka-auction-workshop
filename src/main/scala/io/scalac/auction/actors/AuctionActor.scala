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
        sender ! AuctionStateMessage(context.self, AuctionState.UNSCHEDULED)
        unscheduled(owner, title, startTime, endTime, Map.empty)
      case msg: GeneralProtocol =>
        msg.sender ! InvalidActorState(context.self)
        Behaviors.same
    }
  }

  def unscheduled(owner: String, title: String, startTime: Instant, endTime: Instant, lots: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      checkAccess(owner, message, context) {
        case GetAuctionState(sender, _) =>
          sender ! AuctionStateMessage(context.self, AuctionState.UNSCHEDULED)
          Behaviors.same
        case GetAuctionData(sender, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case createLot: CreateLot =>
          if (lots.keys.exists(_ == createLot.title)) {
            createLot.sender ! LotAlreadyExists(context.self, createLot.title)
            Behaviors.same
          }
          else {
            val lot = context.spawn(LotActor(), createLot.title)
            lot ! createLot
            unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.title -> lot))
          }
        case GetLotData(sender, userId, name) =>
          lots.get(name) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, name)
            case None =>
              sender ! LotNotFound(context.self, name)
          }
          Behaviors.same
        case AlterAuction(sender, _, maybeTitle, maybeStartTime, maybeEndTime) =>
          val (curTitle: String, curStartTime: Instant, curEndTime: Instant) = updateAuctionData(title, startTime, endTime, maybeTitle, maybeStartTime, maybeEndTime)
          sender ! AuctionData(context.self, owner, curTitle, curStartTime, curEndTime, lots.keys.toList)
          unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
        case SetAuctionState(sender, _, state) => state match {
          case AuctionState.SCHEDULED =>
            sender ! AuctionStateMessage(context.self, state)
            scheduled(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PREVIEW =>
            sender ! AuctionStateMessage(context.self, state)
            lots.values.foreach(_ ! SetLotState(sender, LotState.IN_PREVIEW))
            inPreview(owner, title, startTime, endTime, lots)
          case AuctionState.IN_PROGRESS =>
            sender ! AuctionStateMessage(context.self, state)
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
        case GetAuctionState(sender, _) =>
          sender ! AuctionStateMessage(context.self, AuctionState.SCHEDULED)
          Behaviors.same
        case GetAuctionData(sender, _) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, name) =>
          lots.get(name) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, name)
            case None =>
              sender ! LotNotFound(context.self, name)
          }
          Behaviors.same
        case msg: GeneralProtocol if msg.isInstanceOf[AccessControl] =>
          checkAccess(owner, message, context) {
            case AlterAuction(sender, _, maybeTitle, maybeStartTime, maybeEndTime) =>
              val (curTitle: String, curStartTime: Instant, curEndTime: Instant) =
                updateAuctionData(title, startTime, endTime, maybeTitle, maybeStartTime, maybeEndTime)
              sender ! AuctionData(context.self, owner, curTitle, curStartTime, curEndTime, lots.keys.toList)
              unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.title)) {
                createLot.sender ! LotNotFound(context.self, createLot.title)
                Behaviors.same
              }
              else {
                val lot = context.spawn(LotActor(), createLot.title)
                lot ! createLot
                createLot.sender ! LotCreated(context.self, createLot.title)
                unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.title -> lot))
              }
            case SetAuctionState(sender, userId, state) => state match {
              case AuctionState.IN_PREVIEW =>
                sender ! AuctionStateMessage(context.self, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.IN_PREVIEW))
                inPreview(owner, title, startTime, endTime, lots)
              case AuctionState.IN_PROGRESS =>
                sender ! AuctionStateMessage(context.self, state)
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
        case GetAuctionState(sender, userId) =>
          sender ! AuctionStateMessage(context.self, AuctionState.IN_PREVIEW)
          Behaviors.same
        case GetAuctionData(sender, userId) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, name) =>
          lots.get(name) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, name)
            case None =>
              sender ! LotNotFound(context.self, name)
          }
          Behaviors.same
        case msg: GeneralProtocol if msg.isInstanceOf[AccessControl] =>
          checkAccess(owner, message, context) {
            case AlterAuction(sender, userId, maybeTitle, maybeStartTime, maybeEndTime) =>
              val (curTitle: String, curStartTime: Instant, curEndTime: Instant) = updateAuctionData(title, startTime, endTime, maybeTitle, maybeStartTime, maybeEndTime)
              sender ! AuctionData(context.self, owner, curTitle, curStartTime, curEndTime, lots.keys.toList)
              unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.title)) {
                createLot.sender ! LotAlreadyExists(context.self, createLot.title)
                Behaviors.same
              }
              else {
                val lot = context.spawn(LotActor(), createLot.title)
                lot ! createLot
                lot ! SetLotState(null, LotState.IN_PREVIEW)
                createLot.sender ! LotCreated(context.self, createLot.title)
                unscheduled(owner, title, startTime, endTime, lots ++ Map(createLot.title -> lot))
              }
            case SetAuctionState(sender, userId, state) => state match {
              case AuctionState.SCHEDULED =>
                sender ! AuctionStateMessage(context.self, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.CLOSED))
                scheduled(owner, title, startTime, endTime, lots)
              case AuctionState.IN_PROGRESS =>
                sender ! AuctionStateMessage(context.self, state)
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
        case GetAuctionState(sender, userId) =>
          sender ! AuctionStateMessage(context.self, AuctionState.IN_PROGRESS)
          Behaviors.same
        case GetAuctionData(sender, userId) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, name) =>
          lots.get(name) match {
            case Some(lot) =>
              lot ! GetLotData(sender, name, userId)
            case None =>
              sender ! LotNotFound(context.self, name)
          }
          Behaviors.same
        case PlaceBid(sender, userId, lotName, bid) =>
          lots.get(lotName) match {
            case Some(lot) =>
              lot ! PlaceBid(sender, userId, lotName, bid)
            case None =>
              sender ! LotNotFound(context.self, lotName)
          }
          Behaviors.same
        case SetAuctionState(sender, userId, state) =>
          state match {
            case _ if userId != owner =>
              sender ! AccessDenied(context.self)
              Behaviors.same
            case AuctionState.NEAR_END =>
              sender ! AuctionStateMessage(context.self, state)
              nearEnd(owner, title, startTime, endTime, lots)
            case AuctionState.FINISHED =>
              sender ! AuctionStateMessage(context.self, state)
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
        case GetAuctionState(sender, userId) =>
          sender ! AuctionStateMessage(context.self, AuctionState.NEAR_END)
          Behaviors.same
        case GetAuctionData(sender, userId) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, name) =>
          lots.get(name) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, name)
            case None =>
              sender ! LotNotFound(context.self, name)
          }
          Behaviors.same
        case SetAuctionState(sender, userId, state) => state match {
          case _ if userId != owner =>
            sender ! AccessDenied(context.self)
            Behaviors.same
          case AuctionState.FINISHED =>
            sender ! AuctionStateMessage(context.self, state)
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
        case GetAuctionState(sender, userId) =>
          sender ! AuctionStateMessage(context.self, AuctionState.FINISHED)
          Behaviors.same
        case GetAuctionData(sender, userId) =>
          sender ! AuctionData(context.self, owner, title, startTime, endTime, lots.keys.toList)
          Behaviors.same
        case GetLotData(sender, userId, name) =>
          lots.get(name) match {
            case Some(lot) =>
              lot ! GetLotData(sender, userId, name)
            case None =>
              sender ! LotNotFound(context.self, name)
          }
          Behaviors.same
        case SetAuctionState(sender, userId, state) => state match {
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

  private def updateAuctionData(title: String, startTime: Instant, endTime: Instant, maybeTitle: Option[String], maybeStartTime: Option[Instant], maybeEndTime: Option[Instant]) = {
    val curTitle = maybeTitle.getOrElse(title)
    val curStartTime = maybeStartTime.getOrElse(startTime)
    val curEndTime = maybeEndTime.getOrElse(endTime)
    (curTitle, curStartTime, curEndTime)
  }

}
