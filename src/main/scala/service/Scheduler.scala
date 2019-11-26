package service

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.duration._

object Scheduler {

  case object Ping

  def props(system: ActorSystem): Props = Props(new Scheduler(system))

}

class Scheduler(system: ActorSystem) extends Actor with ActorLogging {
  import Scheduler._
  import system.dispatcher

  val http: HttpExt = Http(context.system)

  val SERVICE_URL = "https://schedule-backend-iitu.herokuapp.com/healthcheck"

  system.scheduler.schedule(Duration.Zero, 15.minutes, self, Ping)

  override def receive: Receive = {
    case Ping =>
      http.singleRequest(HttpRequest(uri = SERVICE_URL))
  }
}
