package service

import entities.auth.{AuthContext, AuthCredentials, OAuthToken}
import entities.domain.{Account, EmptyCabinets, User}
import entities.http.Response

import scala.concurrent.Future

trait Repository {

  def getEmptyCabinets(day: String): Future[Option[EmptyCabinets]]

  def deleteAllEmptyCabinets(): Future[Response]

  def updateAllEmptyCabinets(): Future[Response]

  def addUser(user: User): Future[Response]

  def getUser(userId: Int): Future[Option[User]]

  def addAccount(account: Account): Future[Response]
  def getAllAuthContext(): Future[List[AuthContext]]
  def login(credentials: AuthCredentials): Future[Either[Response, OAuthToken]]

}
