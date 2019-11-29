package service

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.{Http, HttpExt}

import scala.concurrent.duration._

object Scheduler {

  case object Ping

  def props(system: ActorSystem, hostname: String): Props = Props(new Scheduler(system, hostname))

}

class Scheduler(system: ActorSystem, hostname: String) extends Actor with ActorLogging {
  import Scheduler._
  import system.dispatcher

  val http: HttpExt = Http(context.system)

  val SERVICE_URL = s"$hostname/healthcheck"

  system.scheduler.schedule(Duration.Zero, 15.minutes, self, Ping)

  override def receive: Receive = {
    case Ping =>
      http.singleRequest(HttpRequest(uri = SERVICE_URL))
  }
}
