package com.github.dronegator.nlp.main.websocket

/**
  * Created by cray on 9/18/16.
  */

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}
import akka.util.Timeout
import com.github.dronegator.nlp.main
import com.github.dronegator.nlp.main.Concurent

import scala.concurrent.duration._

object NLPTWebServiceSocketTrait {

}

trait NLPTWebServiceSocketTrait
  extends
    main.NLPTAppForWeb {
  this: Concurent =>

  private implicit val timeout: Timeout = 10 seconds

  val (sink, source) =
    MergeHub.source[(String, String)](perProducerBufferSize = 16)
      .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
      .run()

  Source.tick(0 seconds, 10 seconds, ())
    .scan(0) {
      case (n, _) =>
        n + 1
    }
    .map { n =>
      println(s"send $n")
      "ae718cd4-8299-4c74-939f-49094deeed75" -> n.toString
    }.runWith(sink)

  def flow(sessionId: String): Flow[Message, Message, Any] = {
    Flow[Message]
      .collect {
        case text: TextMessage =>
          text.getStrictText
      }
      .mapConcat{x =>
        println(s"received $x")
        List.empty[String]}
      .merge(source.collect {
        case (name, value) if name == sessionId =>
          println(s"pass $name $value")
          value
      })
      .map {
        x => TextMessage(x)
      }
    }

  abstract override def route: Route =
    path("websocket") {
      println("websocket?")
      cookie("sessionId") { sessionId =>
        println(s"websocket for session=$sessionId")
        handleWebSocketMessages(flow(sessionId.value))
      }
    } ~ super.route
  //  abstract override def route: Route = pathPrefix("system") { request =>
  //    (version.route ~ vocabularyStat.route) (request)
  //  } ~ super.route
}


