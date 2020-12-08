package service.dao

import entities.auth.{AuthContext, OAuthToken}

import scala.concurrent.Future

trait AuthContextDAO {

  def update(ctx: AuthContext): Future[OAuthToken]

  def get(accountId: String): Future[Option[AuthContext]]

  def getAll(): Future[List[AuthContext]]

}
