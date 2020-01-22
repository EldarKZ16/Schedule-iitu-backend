package service.dao

import entities.domain.User
import entities.http.Response

import scala.concurrent.Future

trait UserDAO {
  def add(user: User): Future[Response]

  def get(userId: Int): Future[Option[User]]
}
