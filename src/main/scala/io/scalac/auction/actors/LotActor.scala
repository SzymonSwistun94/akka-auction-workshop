package io.scalac.auction.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.routing.Routees
import io.scalac.auction.models.Bid
import io.scalac.auction.protocols._

object LotActor {

  val bidThreshold = 1.05

  def apply(): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case CreateLot(sender, _, name, description) =>
        sender ! LotStateMessage(context.self, LotState.Closed)
        closed(name, description)
      case msg: GeneralProtocol =>
        msg.sender ! InvalidActorState(context.self)
        Behaviors.same
      case _ =>
        Behaviors.same
    }
  }

  def closed(title: String, description: String): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case AlterLot(sender, maybeTitle, maybeDescription) =>
        val curTitle = maybeTitle.getOrElse(title)
        val curDescription = maybeDescription.getOrElse(description)

        sender ! LotData(context.self, curTitle, curDescription)
        Behaviors.same
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.Closed)
        Behaviors.same
      case GetLotData(sender, _, _) =>
        sender ! LotData(context.self, title, description)
        Behaviors.same
      case SetLotState(sender, state) => state match {
        case LotState.InPreview =>
          sender ! LotStateMessage(context.self, LotState.InPreview)
          inPreview(title, description)
        case LotState.Open =>
          sender ! LotStateMessage(context.self, LotState.Open)
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

  def inPreview(title: String, description: String): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.InPreview)
        Behaviors.same
      case GetLotData(sender, _, _) =>
        sender ! LotData(context.self, title, description)
        Behaviors.same
      case SetLotState(sender, state) => state match {
        case LotState.Closed =>
          sender ! LotStateMessage(context.self, LotState.Closed)
          closed(title, description)
        case LotState.Open =>
          sender ! LotStateMessage(context.self, LotState.Open)
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

  def open(title: String, description: String, highestBid: Option[Bid]): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.Open)
        Behaviors.same
      case GetLotData(sender, _, _) =>
        sender ! LotData(context.self, title, description, highestBid)
        Behaviors.same
      case PlaceBid(sender, _, _, bid) =>
        highestBid match {
          case Some(highest) if bid.value <= highest.value * bidThreshold =>
            sender ! BidTooLow(context.self, title)
            Behaviors.same
          case _ =>
            sender ! BidSuccess(context.self, title, bid)
            open(title, description, Some(bid))
        }
      case SetLotState(sender, state) => state match {
        case LotState.Finished =>
          sender ! LotStateMessage(context.self, LotState.Finished)
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

  def finished(title: String, description: String, highestBid: Option[Bid]): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.Finished)
        Behaviors.same
      case GetLotData(sender, _, _) =>
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
