package io.scalac.auction.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.models.Bid
import io.scalac.auction.protocols._

object LotActor {
  def apply(): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case CreateLot(sender, name, description) =>
        sender ! LotStateMessage(context.self, LotState.CLOSED)
        closed(name, description)
      case msg: GeneralProtocol =>
        msg.sender ! MessageRejected(context.self, "Invalid actor state")
        LotActor()
    }
  }

  def closed(title: String, description: String): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case AlterLot(sender, maybeTitle, maybeDescription) =>
        val curTitle = maybeTitle.getOrElse(title)
        val curDescription = maybeDescription.getOrElse(description)

        sender ! LotData(context.self, curTitle, curDescription)
        closed(curTitle, curDescription)
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.CLOSED)
        closed(title, description)
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description)
        closed(title, description)
      case SetState(sender, state) => state match {
        case LotState.IN_PREVIEW =>
          sender ! LotStateMessage(context.self, LotState.IN_PREVIEW)
          inPreview(title, description)
        case LotState.OPEN =>
          sender ! LotStateMessage(context.self, LotState.OPEN)
          open(title, description, None)
        case _ =>
          sender ! MessageRejected(context.self, "Invalid state transition")
          closed(title, description)
      }
    }
  }

  def inPreview(title: String, description: String): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.IN_PREVIEW)
        inPreview(title, description)
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description)
        inPreview(title, description)
      case SetState(sender, state) => state match {
        case LotState.CLOSED =>
          sender ! LotStateMessage(context.self, LotState.CLOSED)
          closed(title, description)
        case LotState.OPEN =>
          sender ! LotStateMessage(context.self, LotState.OPEN)
          open(title, description, None)
        case _ =>
          sender ! MessageRejected(context.self, "Invalid state transition")
          inPreview(title, description)
      }
    }
  }

  def open(title: String, description: String, highestBid: Option[Bid]): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.OPEN)
        open(title, description, highestBid)
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description, highestBid)
        open(title, description, highestBid)
      case PlaceBid(sender, bid) =>
        highestBid match {
          case Some(highest) if bid.value <= highest.value =>
            sender ! BidFailure(context.self)
            open(title, description, highestBid)
          case _ =>
            sender ! BidSuccess(context.self, bid)
            open(title, description, Some(bid))
        }
      case SetState(sender, state) => state match {
        case LotState.FINISHED =>
          sender ! LotStateMessage(context.self, LotState.FINISHED)
          finished(title, description, highestBid)
        case _ =>
          sender ! MessageRejected(context.self, "Invalid state transition")
          inPreview(title, description)
      }
    }
  }

  def finished(title: String, description: String, highestBid: Option[Bid]): Behavior[GeneralProtocol] = Behaviors.receive { (context, message) =>
    message match {
      case GetLotState(sender) =>
        sender ! LotStateMessage(context.self, LotState.FINISHED)
        finished(title, description, highestBid)
      case GetLotData(sender) =>
        sender ! LotData(context.self, title, description, highestBid)
        finished(title, description, highestBid)
      case SetState(sender, _) =>
        sender ! MessageRejected(context.self, "Invalid state transition")
        finished(title, description, highestBid)
    }
  }
}
