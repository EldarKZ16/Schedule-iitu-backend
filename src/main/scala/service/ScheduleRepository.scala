package service

import akka.actor.ActorSystem
import entities.domain.{EmptyCabinets, User}
import entities.http.Response
import org.slf4j.LoggerFactory
import reactivemongo.api.DefaultDB
import service.actors.EmptyCabinetStreamer
import service.dao.mongo.{EmptyCabinetsDAOImpl, UserDAOImpl}
import service.dao.{EmptyCabinetsDAO, UserDAO}
import utils.Utils

import scala.concurrent.Future

object ScheduleRepository {
  private var scheduleRepository: Option[ScheduleRepository] = None

  def apply(system: ActorSystem,
            mongoDatabase: DefaultDB): ScheduleRepository = {
    scheduleRepository match {
      case Some(value) =>
        value
      case None =>
        val newInst = new ScheduleRepository(system, mongoDatabase)
        scheduleRepository = Some(newInst)
        newInst
    }
  }
}

class ScheduleRepository private(system: ActorSystem,
                                 mongoDatabase: DefaultDB)
  extends Repository {

  import system.dispatcher

  private val log = LoggerFactory.getLogger(this.getClass)
  private val userDAO: UserDAO = new UserDAOImpl(mongoDatabase)
  private val emptyCabinetsDAO: EmptyCabinetsDAO = new EmptyCabinetsDAOImpl(mongoDatabase)

  override def getEmptyCabinets(day: String): Future[Option[EmptyCabinets]] = {
    emptyCabinetsDAO.get(day).map(_.orElse(Some(EmptyCabinets(day, Map()))))
  }

  override def deleteAllEmptyCabinets(): Future[Response] = {
    log.info(s"Deleting all empty cabinets")
    emptyCabinetsDAO.deleteAll(Utils.DAYS)
  }

  override def updateAllEmptyCabinets(): Future[Response] = {
    log.info(s"Updating all empty cabinets")
    val scheduleStreamer = system.actorOf(EmptyCabinetStreamer.props(mongoDatabase))
    scheduleStreamer ! EmptyCabinetStreamer.RetrieveCabinet
    Future {
      Response()
    }
  }

  override def addUser(user: User): Future[Response] = {
    log.info(s"Adding new user, userID: ${user.id} and groupID: ${user.groupId}")
    userDAO.add(user)
  }

  override def getUser(userId: Int): Future[Option[User]] = {
    userDAO.get(userId)
  }
}
