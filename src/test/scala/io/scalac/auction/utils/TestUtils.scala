package io.scalac.auction.utils

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef

import scala.concurrent.duration._
import scala.language.postfixOps

object TestUtils {
  def testCallAndResponses[T1, T2](
      probe: TestProbe[T2],
      actorRef: ActorRef[T1],
      callAndResponseList: Seq[(T1, T2)]
  ): Unit = {
    callAndResponseList.foreach { case (call, response) =>
      actorRef ! call
      probe.expectMessage(response)
    }
  }
  def testCallchain[T1, T2](
      probe: TestProbe[T2],
      actorRef: ActorRef[T1],
      callchain: Seq[T1],
      lastMessage: T1,
      lastResponse: T2
  ): Unit = {
    callchain.foreach { msg =>
      actorRef ! msg
//      probe.receiveMessage()
      print(s"$msg: ")
      println(s"${probe.receiveMessage()}")
    }
    actorRef ! lastMessage
    probe.expectMessage(lastResponse)
  }
}
