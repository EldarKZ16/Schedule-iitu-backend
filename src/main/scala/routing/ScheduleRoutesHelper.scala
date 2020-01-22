package routing

import entities.domain.{EmptyCabinets, User}
import entities.http.Response
import service.Repository

import scala.concurrent.Future

trait ScheduleRoutesHelper {

  def repository: Repository

  def getEmptyCabinets(day: String): Future[Option[EmptyCabinets]] = {
    repository.getEmptyCabinets(day)
  }

  def deleteAllEmptyCabinets(): Future[Response] = {
    repository.deleteAllEmptyCabinets()
  }

  def updateAllEmptyCabinets(): Future[Response] = {
    repository.updateAllEmptyCabinets()
  }

  def addUser(user: User): Future[Response] = {
    repository.addUser(user)
  }

  def getUser(userId: Int): Future[Option[User]] = {
    repository.getUser(userId)
  }

}
