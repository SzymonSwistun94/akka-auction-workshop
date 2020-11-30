package io.scalac.auction.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.models.Bid
import io.scalac.auction.protocols._

object LotActor {

  val bidThreshold = 1.05

  def apply(): Behavior[GeneralProtocol] = Behaviors.receive {
    (context, message) =>
      message match {
        case CreateLot(_, _, _, lotName, description) =>
          closed(lotName, description)
        case msg: GeneralProtocol =>
          msg.sender ! InvalidActorState(context.self)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
  }

  def closed(title: String, description: String): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case AlterLot(sender, _, maybeDescription) =>
          val curDescription = maybeDescription.getOrElse(description)

        sender ! LotData(context.self, title, curDescription)
        Behaviors.same
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, title, LotState.Closed)
        Behaviors.same
      case GetLotData(sender, _, _, _) =>
        sender ! LotData(context.self, title, description)
        Behaviors.same
      case SetLotState(sender, state) => state match {
        case LotState.InPreview =>
          sender ! LotStateMessage(context.self, title, LotState.InPreview)
          inPreview(title, description)
        case LotState.Open =>
          sender ! LotStateMessage(context.self, title, LotState.Open)
          open(title, description, None)
        case _ =>
          sender ! InvalidStateTransition(context.self)
          Behaviors.same
      }
      case msg: GeneralProtocol =>
        msg.sender ! InvalidActorState(context.self)
        Behaviors.same
    }
  }

  def inPreview(title: String, description: String): Behavior[GeneralProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetLotState(sender) =>
          sender ! LotStateMessage(context.self, title, LotState.InPreview)
          Behaviors.same
        case GetLotData(sender, _, _, _) =>
          sender ! LotData(context.self, title, description)
          Behaviors.same
        case SetLotState(sender, state) =>
          state match {
            case LotState.Closed =>
              sender ! LotStateMessage(context.self, title, LotState.Closed)
              closed(title, description)
            case LotState.Open =>
              sender ! LotStateMessage(context.self, title, LotState.Open)
              open(title, description, None)
            case _ =>
              sender ! InvalidStateTransition(context.self)
              Behaviors.same
          }
        case msg: GeneralProtocol =>
          msg.sender ! InvalidActorState(context.self)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }

  def open(
      title: String,
      description: String,
      highestBid: Option[Bid]
  ): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, title, LotState.Open)
        Behaviors.same
      case GetLotData(sender, _, _, _) =>
        sender ! LotData(context.self, title, description, highestBid)
        Behaviors.same
      case PlaceBid(sender, _, _, _, bid) =>
        highestBid match {
          case Some(highest) if bid.value <= highest.value * bidThreshold =>
            sender ! BidTooLow(context.self, title)
            Behaviors.same
          case _ =>
            sender ! BidSuccess(context.self, title, bid)
            open(title, description, Some(bid))
        }
      case SetLotState(sender, state) =>
        state match {
          case LotState.Finished =>
            sender ! LotStateMessage(context.self, title, LotState.Finished)
            finished(title, description, highestBid)
          case _ =>
            sender ! InvalidStateTransition(context.self)
            Behaviors.same
        }
      case msg: GeneralProtocol =>
        msg.sender ! InvalidActorState(context.self)
        Behaviors.same
      case _ =>
        Behaviors.same
    }
  }

  def finished(
      title: String,
      description: String,
      highestBid: Option[Bid]
  ): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, title, LotState.Finished)
        Behaviors.same
      case GetLotData(sender, _, _, _) =>
        sender ! LotData(context.self, title, description, highestBid)
        Behaviors.same
      case SetLotState(sender, _) =>
        sender ! InvalidStateTransition(context.self)
        Behaviors.same
      case msg: GeneralProtocol =>
        msg.sender ! InvalidActorState(context.self)
        Behaviors.same
      case _ =>
        Behaviors.same
    }
  }
}
