package service.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.{Http, HttpExt}

import scala.concurrent.duration._

object Scheduler {

  case object Ping

  def props(hostname: String): Props = Props(new Scheduler(hostname))

}

// Scheduler for heroku which pings host every 15 minutes.
// Free Heroku services sleep after 30 minutes due to inactivity.
class Scheduler(hostname: String) extends Actor with ActorLogging {

  import Scheduler._
  import context.dispatcher

  val http: HttpExt = Http(context.system)

  private val SERVICE_URL = s"$hostname/healthcheck"

  context.system.scheduler.schedule(Duration.Zero, 15.minutes, self, Ping)

  override def receive: Receive = {
    case Ping =>
      http.singleRequest(HttpRequest(uri = SERVICE_URL))
  }
}
