package entities.auth


import akka.http.scaladsl.server.directives.Credentials
import org.joda.time.DateTime
import serialization.Json4sSerialization
import service.Repository

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait OAuthAuthenticator {
  this: Json4sSerialization =>

  def repository: Repository

  def authenticator(credentials: Credentials)(implicit executionContext: ExecutionContext): Future[Option[AuthContext]] = {
    val result = Promise[Option[AuthContext]]()
    credentials match {
      case p @ Credentials.Provided(_) =>
        val retrievedLoggedInUsers = repository.getAllAuthContext()
        retrievedLoggedInUsers onComplete {
          case Success(contexts) =>
            result.success(contexts.find{user => p.verify(user.oAuthToken.access_token) && isValidToken(user.loggedInAt, user.oAuthToken)})
          case Failure(exception) =>
            result.failure(exception)
        }
      case _ => result.success(None)
    }
    result.future
  }

  private def isValidToken(loggedInTime: DateTime, oAuthToken: OAuthToken): Boolean = {
    loggedInTime.plusSeconds(oAuthToken.expires_in).isAfterNow
  }

}