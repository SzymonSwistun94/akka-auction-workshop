package io.scalac.auction

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import io.scalac.auction.http.ApiRoutes
import io.scalac.auction.protocols.GeneralProtocol
import io.scalac.auction.services.ActorBasedAuctionService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Boot {

  private def startHttpServer(
      route: Route,
      iface: String = "0.0.0.0",
      port: Int = 8080
  )(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val fBinding = Http().newServerAt(iface, port).bind(route)
    fBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        println(
          s"Failed to bind HTTP endpoint, terminating system: ${ex.getLocalizedMessage}"
        )
        system.terminate()
    }
  }

  def main(args: Array[String]) {
    val rootBehaviour = Behaviors.setup[GeneralProtocol] { implicit ctx =>
      implicit val system: ActorSystem[_] = ctx.system
      implicit val ec: ExecutionContext = ctx.executionContext

      val auctionService = ActorBasedAuctionService.create
      val apiRoutes = new ApiRoutes(auctionService)

      startHttpServer(apiRoutes.route)
      Behaviors.empty
    }
    val system = ActorSystem[GeneralProtocol](rootBehaviour, "auctions")
  }

}
