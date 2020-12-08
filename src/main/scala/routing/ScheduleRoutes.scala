package routing

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import entities.auth.{AuthCredentials, OAuthAuthenticator}
import entities.domain.{Account, User}
import serialization.Json4sSerialization

trait ScheduleRoutes extends ScheduleRoutesHelper with Json4sSerialization with OAuthAuthenticator {

  val scheduleRoutes: Route = {
    concat(
      pathPrefix("auth") {
        concat(
          pathPrefix("create-account") {
            pathEndOrSingleSlash {
              post {
                entity(as[Account]) {account =>
                  onSuccess(addAccount(account)) { result =>
                    complete((StatusCode.int2StatusCode(result.status), result))
                  }
                }
              }
            }
          },
          pathPrefix("login") {
            pathEndOrSingleSlash {
              post {
                entity(as[AuthCredentials]) { account =>
                  onSuccess(login(account)) {
                    case Right(result) =>
                      complete(result)
                    case Left(result) =>
                      complete((StatusCode.int2StatusCode(result.status), result))
                  }
                }
              }
            }
          }
        )
      },
      pathPrefix("schedule") {
        authenticateOAuth2Async("api", authenticator) { _ =>
          concat(
            pathPrefix("room") {
              pathEndOrSingleSlash {
                get {
                  parameter('day.as[String]) { day =>
                    onSuccess(getEmptyCabinets(day)) { schedule =>
                      complete(schedule)
                    }
                  }
                }
              }
            },
            pathPrefix("clear") {
              pathEndOrSingleSlash {
                post {
                  onSuccess(deleteAllEmptyCabinets()) { response =>
                    complete(response)
                  }
                }
              }
            },
            pathPrefix("update") {
              pathEndOrSingleSlash {
                post {
                  onSuccess(updateAllEmptyCabinets()) { response =>
                    complete(response)
                  }
                }
              }
            },
            pathPrefix("user") {
              pathEndOrSingleSlash {
                concat(
                  post {
                    entity(as[User]) { user =>
                      onSuccess(addUser(user)) { response =>
                        complete(response)
                      }
                    }
                  },
                  get {
                    parameters('id.as[Int]) { userId =>
                      onSuccess(getUser(userId)) { user =>
                        complete(user)
                      }
                    }
                  }
                )
              }
            }
          )
        }
      }
    )
  }
}

