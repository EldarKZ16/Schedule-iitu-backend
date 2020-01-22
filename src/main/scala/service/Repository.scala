package service

import entities.domain.{EmptyCabinets, User}
import entities.http.Response

import scala.concurrent.Future

trait Repository {

  def getEmptyCabinets(day: String): Future[Option[EmptyCabinets]]

  def deleteAllEmptyCabinets(): Future[Response]

  def updateAllEmptyCabinets(): Future[Response]

  def addUser(user: User): Future[Response]

  def getUser(userId: Int): Future[Option[User]]

}
