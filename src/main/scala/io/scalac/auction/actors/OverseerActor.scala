package io.scalac.auction.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import io.scalac.auction.protocols._

object OverseerActor {

  def apply(): Behavior[GeneralProtocol] = router(Map.empty)


  private def router(auctions: Map[String, ActorRef[GeneralProtocol]]): Behavior[GeneralProtocol] = Behaviors.receive {
    (context, message) =>
      message match {
        case GetAuctionsList(sender, _) =>
          sender ! AuctionsList(context.self, auctions.keys.toList)
          Behaviors.same
        case query: AuctionQuery =>
          query match {
            case CreateAuction(sender, _, _, auctionName, _, _) =>
              if (auctions.contains(auctionName)) {
                sender ! AuctionAlreadyExists(context.self, auctionName)
                Behaviors.same
              }
              else {
                val auction = context.spawn(AuctionActor(), query.auctionName)
                auction ! query
                router(auctions ++ Map(auctionName -> auction))
              }
            case _ =>
              auctions.get(query.auctionName) match {
                case Some(auction) =>
                  auction ! query
                  Behaviors.same
                case None =>
                  query.sender ! AuctionNotFound(context.self, query.auctionName)
                  Behaviors.same
              }
          }
      }
  }
}
