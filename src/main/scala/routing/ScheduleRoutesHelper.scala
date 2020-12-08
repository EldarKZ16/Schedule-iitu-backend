package routing

import entities.auth.{AuthContext, AuthCredentials, OAuthToken}
import entities.domain.{Account, EmptyCabinets, User}
import entities.http.Response
import service.Repository

import scala.concurrent.{ExecutionContext, Future}

trait ScheduleRoutesHelper {

  implicit def executionContext: ExecutionContext

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

  def addAccount(account: Account): Future[Response] = repository.addAccount(account)
  def login(credentials: AuthCredentials): Future[Either[Response, OAuthToken]] = repository.login(credentials)

}
