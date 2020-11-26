package io.scalac.auction.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.routing.Routees
import io.scalac.auction.models.Bid
import io.scalac.auction.protocols._

object LotActor {
  Routees
  def apply(): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case CreateLot(sender, name, description) =>
        sender ! LotStateMessage(context.self, LotState.CLOSED)
        closed(name, description)
      case msg: GeneralProtocol =>
        msg.sender ! MessageRejected(context.self, "Invalid actor state")
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
        sender ! LotStateMessage(context.self, LotState.CLOSED)
        Behaviors.same
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description)
        Behaviors.same
      case SetLotState(sender, state) => state match {
        case LotState.IN_PREVIEW =>
          sender ! LotStateMessage(context.self, LotState.IN_PREVIEW)
          inPreview(title, description)
        case LotState.OPEN =>
          sender ! LotStateMessage(context.self, LotState.OPEN)
          open(title, description, None)
        case _ =>
          sender ! MessageRejected(context.self, "Invalid state transition")
          Behaviors.same
      }
      case msg: GeneralProtocol =>
        msg.sender ! MessageRejected(context.self, "Invalid actor state")
        Behaviors.same
    }
  }

  def inPreview(title: String, description: String): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.IN_PREVIEW)
        Behaviors.same
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description)
        Behaviors.same
      case SetLotState(sender, state) => state match {
        case LotState.CLOSED =>
          sender ! LotStateMessage(context.self, LotState.CLOSED)
          closed(title, description)
        case LotState.OPEN =>
          sender ! LotStateMessage(context.self, LotState.OPEN)
          open(title, description, None)
        case _ =>
          sender ! MessageRejected(context.self, "Invalid state transition")
          Behaviors.same
      }
      case msg: GeneralProtocol =>
        msg.sender ! MessageRejected(context.self, "Invalid actor state")
        Behaviors.same
    }
  }

  def open(title: String, description: String, highestBid: Option[Bid]): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.OPEN)
        Behaviors.same
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description, highestBid)
        Behaviors.same
      case PlaceBid(sender, _, bid) =>
        highestBid match {
          case Some(highest) if bid.value <= highest.value =>
            sender ! BidFailure(context.self, title)
            Behaviors.same
          case _ =>
            sender ! BidSuccess(context.self, title, bid)
            open(title, description, Some(bid))
        }
      case SetLotState(sender, state) => state match {
        case LotState.FINISHED =>
          sender ! LotStateMessage(context.self, LotState.FINISHED)
          finished(title, description, highestBid)
        case _ =>
          sender ! MessageRejected(context.self, "Invalid state transition")
          Behaviors.same
      }
      case msg: GeneralProtocol =>
        msg.sender ! MessageRejected(context.self, "Invalid actor state")
        Behaviors.same
    }
  }

  def finished(title: String, description: String, highestBid: Option[Bid]): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.FINISHED)
        Behaviors.same
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description, highestBid)
        Behaviors.same
      case SetLotState(sender, _) =>
        sender ! MessageRejected(context.self, "Invalid state transition")
        Behaviors.same
      case msg: GeneralProtocol =>
        msg.sender ! MessageRejected(context.self, "Invalid actor state")
        Behaviors.same
    }
  }
}
