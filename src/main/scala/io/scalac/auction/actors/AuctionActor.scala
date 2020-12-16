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
          sender ! AuctionStateMessage(context.self, AuctionState.Unscheduled)
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
          if (lots.keys.exists(_ == createLot.title)) {
            createLot.sender ! LotAlreadyExists(context.self, createLot.title)
            Behaviors.same
          } else {
            val lot = context.spawn(LotActor(), createLot.title)
            lot ! createLot
            unscheduled(
              owner,
              title,
              startTime,
              endTime,
              lots + (createLot.title -> lot)
            )
          }
        case SetAuctionState(sender, _, state) =>
          state match {
            case AuctionState.Scheduled =>
              sender ! AuctionStateMessage(context.self, state)
              scheduled(owner, title, startTime, endTime, lots)
            case AuctionState.InPreview =>
              sender ! AuctionStateMessage(context.self, state)
              lots.values.foreach(_ ! SetLotState(sender, LotState.InPreview))
              inPreview(owner, title, startTime, endTime, lots)
            case AuctionState.InProgress =>
              sender ! AuctionStateMessage(context.self, state)
              lots.values.foreach(_ ! SetLotState(sender, LotState.Open))
              inProgress(owner, title, startTime, endTime, lots)
            case _ =>
              sender ! InvalidStateTransition(context.self)
              Behaviors.same
          }
        case AlterAuction(
              sender,
              _,
              maybeTitle,
              maybeStartTime,
              maybeEndTime
            ) =>
          val (curTitle, curStartTime, curEndTime) = updateAuctionData(
            title,
            startTime,
            endTime,
            maybeTitle,
            maybeStartTime,
            maybeEndTime
          )
          sender ! AuctionData(
            context.self,
            owner,
            curTitle,
            curStartTime,
            curEndTime,
            lots.keys.toList
          )
          unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
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
        case msg: GeneralProtocol with AccessControl =>
          checkAccess(owner, msg, context) {
            case AlterAuction(
                  sender,
                  _,
                  maybeTitle,
                  maybeStartTime,
                  maybeEndTime
                ) =>
              val (curTitle, curStartTime, curEndTime) =
                updateAuctionData(
                  title,
                  startTime,
                  endTime,
                  maybeTitle,
                  maybeStartTime,
                  maybeEndTime
                )
              sender ! AuctionData(
                context.self,
                owner,
                curTitle,
                curStartTime,
                curEndTime,
                lots.keys.toList
              )
              unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.title)) {
                createLot.sender ! LotNotFound(context.self, createLot.title)
                Behaviors.same
              } else {
                val lot = context.spawn(LotActor(), createLot.title)
                lot ! createLot
                createLot.sender ! LotCreated(context.self, createLot.title)
                unscheduled(
                  owner,
                  title,
                  startTime,
                  endTime,
                  lots + (createLot.title -> lot)
                )
              }
            case SetAuctionState(sender, _, state) =>
              state match {
                case AuctionState.InPreview =>
                  sender ! AuctionStateMessage(context.self, state)
                  lots.values.foreach(
                    _ ! SetLotState(sender, LotState.InPreview)
                  )
                  inPreview(owner, title, startTime, endTime, lots)
                case AuctionState.InProgress =>
                  sender ! AuctionStateMessage(context.self, state)
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
          checkAccess(owner, msg, context) {
            case AlterAuction(
                  sender,
                  _,
                  maybeTitle,
                  maybeStartTime,
                  maybeEndTime
                ) =>
              val (curTitle, curStartTime, curEndTime) = updateAuctionData(
                title,
                startTime,
                endTime,
                maybeTitle,
                maybeStartTime,
                maybeEndTime
              )
              sender ! AuctionData(
                context.self,
                owner,
                curTitle,
                curStartTime,
                curEndTime,
                lots.keys.toList
              )
              unscheduled(owner, curTitle, curStartTime, curEndTime, lots)
            case createLot: CreateLot =>
              if (lots.keys.exists(_ == createLot.title)) {
                createLot.sender ! LotAlreadyExists(
                  context.self,
                  createLot.title
                )
                Behaviors.same
              } else {
                val lot = context.spawn(LotActor(), createLot.title)
                lot ! createLot
                lot ! SetLotState(noSender, LotState.InPreview)
                createLot.sender ! LotCreated(context.self, createLot.title)
                unscheduled(
                  owner,
                  title,
                  startTime,
                  endTime,
                  lots + (createLot.title -> lot)
                )
              }
            case SetAuctionState(sender, _, state) =>
              state match {
                case AuctionState.Scheduled =>
                  sender ! AuctionStateMessage(context.self, state)
                  lots.values.foreach(_ ! SetLotState(sender, LotState.Closed))
                  scheduled(owner, title, startTime, endTime, lots)
                case AuctionState.InProgress =>
                  sender ! AuctionStateMessage(context.self, state)
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
            case AuctionState.NearEnd =>
              sender ! AuctionStateMessage(context.self, state)
              nearEnd(owner, title, startTime, endTime, lots)
            case AuctionState.Finished =>
              sender ! AuctionStateMessage(context.self, state)
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
        case SetAuctionState(sender, userId, state) =>
          state match {
            case _ if userId != owner =>
              sender ! AccessDenied(context.self)
              Behaviors.same
            case AuctionState.Finished =>
              sender ! AuctionStateMessage(context.self, state)
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
        case SetAuctionState(sender, userId, state) =>
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
      title: String,
      startTime: Instant,
      endTime: Instant,
      maybeTitle: Option[String],
      maybeStartTime: Option[Instant],
      maybeEndTime: Option[Instant]
  ) =
    (
      maybeTitle.getOrElse(title),
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
    case GetAuctionState(sender, _) =>
      sender ! AuctionStateMessage(context.self, state)
      Behaviors.same
    case GetAuctionData(sender, _) =>
      sender ! AuctionData(
        context.self,
        owner,
        title,
        startTime,
        endTime,
        lots.keys.toList
      )
      Behaviors.same
    case GetLotData(sender, userId, name) =>
      lots.get(name) match {
        case Some(lot) =>
          lot ! GetLotData(sender, userId, name)
        case None =>
          sender ! LotNotFound(context.self, name)
      }
      Behaviors.same
    case msg: GeneralProtocol =>
      msg.sender ! InvalidActorState(context.self)
      Behaviors.same
  }

}
