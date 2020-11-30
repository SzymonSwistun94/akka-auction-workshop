package io.scalac.auction.actors

import java.time.Instant

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import io.scalac.auction.protocols._
import io.scalac.auction.utils.ActorUtils._
import io.scalac.auction.utils.SecurityUtils._

object AuctionActor {
  def apply(): Behavior[GeneralProtocol] = Behaviors.receive {
    (context, message) =>
      message match {
        case CreateAuction(sender, _, owner, title, startTime, endTime) =>
          sender ! AuctionStateMessage(context.self, title, AuctionState.Unscheduled)
          unscheduled(owner, title, startTime, endTime, Map.empty)
        case msg: GeneralProtocol =>
          msg.sender ! InvalidActorState(context.self)
          Behaviors.same
      }
  }

  def unscheduled(
      owner: String,
      title: String,
      startTime: Instant,
      endTime: Instant,
      lots: Map[String, ActorRef[GeneralProtocol]]
  ): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      val state = AuctionState.Unscheduled

      checkAccess(owner, message, context) {
        case createLot: CreateLot =>
          if (lots.keys.exists(_ == createLot.lotName)) {
            createLot.sender ! LotAlreadyExists(context.self, createLot.lotName)
            Behaviors.same
          } else {
            val lot = context.spawn(LotActor(), createLot.lotName)
            lot ! createLot
            createLot.sender ! LotStateMessage(context.self, createLot.lotName, LotState.Closed)
            unscheduled(
              owner,
              title,
              startTime,
              endTime,
              lots + (createLot.lotName -> lot)
            )
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
          val (curStartTime, curEndTime) = updateAuctionData(startTime, endTime, maybeStartTime, maybeEndTime)
          sender ! AuctionData(context.self, owner, title, curStartTime, curEndTime, lots.keys.toList)
          unscheduled(owner, title, curStartTime, curEndTime, lots)
        case SetAuctionState(sender, _, _, state) => state match {
          case AuctionState.Scheduled =>
            sender ! AuctionStateMessage(context.self, title, state)
            scheduled(owner, title, startTime, endTime, lots)
          case AuctionState.InPreview =>
            sender ! AuctionStateMessage(context.self, title, state)
            lots.values.foreach(_ ! SetLotState(sender, LotState.InPreview))
            inPreview(owner, title, startTime, endTime, lots)
          case AuctionState.InProgress =>
            sender ! AuctionStateMessage(context.self, title, state)
            lots.values.foreach(_ ! SetLotState(sender, LotState.Open))
            inProgress(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! InvalidStateTransition(context.self)
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          common(msg, owner, title, startTime, endTime, lots, state, context)
      }
    }

  def scheduled(
      owner: String,
      title: String,
      startTime: Instant,
      endTime: Instant,
      lots: Map[String, ActorRef[GeneralProtocol]]
  ): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      val state = AuctionState.Scheduled

      message match {
        case msg: GeneralProtocol if msg.isInstanceOf[AccessControl] =>
          checkAccess(owner, message, context) {
            case AlterAuction(sender, _, _, maybeStartTime, maybeEndTime) =>
              val (curStartTime, curEndTime) = updateAuctionData(startTime, endTime, maybeStartTime, maybeEndTime)
              sender ! AuctionData(context.self, owner, title, curStartTime, curEndTime, lots.keys.toList)
              unscheduled(owner, title, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.lotName)) {
                createLot.sender ! LotAlreadyExists(context.self, createLot.lotName)
                Behaviors.same
              } else {
                val lot = context.spawn(LotActor(), createLot.lotName)
                lot ! createLot
                createLot.sender ! LotStateMessage(context.self, createLot.lotName, LotState.Closed)
                unscheduled(
                  owner,
                  title,
                  startTime,
                  endTime,
                  lots + (createLot.lotName -> lot)
                )
              }
            case SetAuctionState(sender, _, _, state) => state match {
              case AuctionState.InPreview =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.InPreview))
                inPreview(owner, title, startTime, endTime, lots)
              case AuctionState.InProgress =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.Open))
                inProgress(owner, title, startTime, endTime, lots)
              case _ =>
                sender ! InvalidStateTransition(context.self)
                Behaviors.same
            }
            case _ =>
              msg.sender ! InvalidActorState(context.self)
              Behaviors.same
          }
        case msg: GeneralProtocol =>
          common(msg, owner, title, startTime, endTime, lots, state, context)
      }
    }

  def inPreview(
      owner: String,
      title: String,
      startTime: Instant,
      endTime: Instant,
      lots: Map[String, ActorRef[GeneralProtocol]]
  ): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      val state = AuctionState.InPreview

      message match {
        case msg: GeneralProtocol with AccessControl =>
          checkAccess(owner, message, context) {
            case AlterAuction(sender, _, _, maybeStartTime, maybeEndTime) =>
              val (curStartTime, curEndTime) = updateAuctionData(startTime, endTime, maybeStartTime, maybeEndTime)
              sender ! AuctionData(context.self, owner, title, curStartTime, curEndTime, lots.keys.toList)
              unscheduled(owner, title, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.lotName)) {
                createLot.sender ! LotAlreadyExists(
                  context.self,
                  createLot.lotName
                )
                Behaviors.same
              } else {
                val lot = context.spawn(LotActor(), createLot.lotName)
                lot ! createLot
                lot ! SetLotState(noSender, LotState.InPreview)
                createLot.sender ! LotStateMessage(context.self, createLot.lotName, LotState.InPreview)
                unscheduled(
                  owner,
                  title,
                  startTime,
                  endTime,
                  lots + (createLot.lotName -> lot)
                )
              }
            case SetAuctionState(sender, _, _, state) => state match {
              case AuctionState.Scheduled =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.Closed))
                scheduled(owner, title, startTime, endTime, lots)
              case AuctionState.InProgress =>
                sender ! AuctionStateMessage(context.self, title, state)
                lots.values.foreach(_ ! SetLotState(sender, LotState.Open))
                inProgress(owner, title, startTime, endTime, lots)
              case _ =>
                sender ! InvalidStateTransition(context.self)
                Behaviors.same
            }
            case _ =>
              msg.sender ! InvalidActorState(context.self)
              Behaviors.same
          }
        case msg: GeneralProtocol =>
          common(msg, owner, title, startTime, endTime, lots, state, context)
      }
    }

  def inProgress(
      owner: String,
      title: String,
      startTime: Instant,
      endTime: Instant,
      lots: Map[String, ActorRef[GeneralProtocol]]
  ): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      val state = AuctionState.InProgress

      message match {
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
            case AuctionState.NearEnd =>
              sender ! AuctionStateMessage(context.self, title, state)
              nearEnd(owner, title, startTime, endTime, lots)
            case AuctionState.Finished =>
              sender ! AuctionStateMessage(context.self, title, state)
              lots.values.foreach(_ ! SetLotState(sender, LotState.Finished))
              finished(owner, title, startTime, endTime, lots)
            case _ =>
              sender ! InvalidStateTransition(context.self)
              Behaviors.same
          }
        case msg: GeneralProtocol =>
          common(msg, owner, title, startTime, endTime, lots, state, context)
      }
    }

  def nearEnd(
      owner: String,
      title: String,
      startTime: Instant,
      endTime: Instant,
      lots: Map[String, ActorRef[GeneralProtocol]]
  ): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      val state = AuctionState.NearEnd

      message match {
        case SetAuctionState(sender, userId, _, state) => state match {
          case _ if userId != owner =>
            sender ! AccessDenied(context.self)
            Behaviors.same
          case AuctionState.Finished =>
            sender ! AuctionStateMessage(context.self, title, state)
            lots.values.foreach(_ ! SetLotState(sender, LotState.Finished))
            finished(owner, title, startTime, endTime, lots)
          case _ =>
            sender ! InvalidStateTransition(context.self)
            Behaviors.same
        }
        case msg: GeneralProtocol =>
          common(msg, owner, title, startTime, endTime, lots, state, context)
      }
    }

  def finished(
      owner: String,
      title: String,
      startTime: Instant,
      endTime: Instant,
      lots: Map[String, ActorRef[GeneralProtocol]]
  ): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      val state = AuctionState.Finished

      message match {
        case SetAuctionState(sender, userId, _, state) =>
          state match {
            case _ if userId != owner =>
              sender ! AccessDenied(context.self)
              Behaviors.same
            case _ =>
              sender ! InvalidStateTransition(context.self)
              Behaviors.same
          }
        case msg: GeneralProtocol =>
          common(msg, owner, title, startTime, endTime, lots, state, context)
      }
    }

  private def updateAuctionData(
      startTime: Instant,
      endTime: Instant,
      maybeStartTime: Option[Instant],
      maybeEndTime: Option[Instant]
  ) =
    (
      maybeStartTime.getOrElse(startTime),
      maybeEndTime.getOrElse(endTime)
    )

  private def common(
      message: GeneralProtocol,
      owner: String,
      title: String,
      startTime: Instant,
      endTime: Instant,
      lots: Map[String, ActorRef[GeneralProtocol]],
      state: AuctionState,
      context: ActorContext[GeneralProtocol]
  ): Behavior[GeneralProtocol] = message match {
    case GetAuctionState(sender, _, _) =>
      sender ! AuctionStateMessage(context.self, title, state)
      Behaviors.same
    case GetAuctionData(sender, _, _) =>
      sender ! AuctionData(
        context.self,
        owner,
        title,
        startTime,
        endTime,
        lots.keys.toList
      )
      Behaviors.same
    case GetLotData(sender, userId, _, lotName) =>
      lots.get(lotName) match {
        case Some(lot) =>
          lot ! GetLotData(sender, userId, title, lotName)
        case None =>
          sender ! LotNotFound(context.self, lotName)
      }
      Behaviors.same
    case msg: GeneralProtocol =>
      msg.sender ! InvalidActorState(context.self)
      Behaviors.same
  }

}
