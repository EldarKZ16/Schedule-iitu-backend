package service.dao

import entities.domain.Account
import entities.http.Response

import scala.concurrent.Future

trait AccountDAO {

  def add(account: Account): Future[Response]
  def update(account: Account): Future[Response]
  def get(id: String): Future[Option[Account]]

}
