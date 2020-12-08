package routing

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection, RejectionHandler}
import serialization.Json4sSerialization
import akka.http.scaladsl.server.Directives.complete
import entities.http.Response

trait CustomRejectionHandler {
  this: Json4sSerialization =>

  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case AuthenticationFailedRejection(cause, _) =>
          complete((StatusCodes.Unauthorized, Response(StatusCodes.Unauthorized.intValue, "Authentication failed")))
      }
      .handle {
        case AuthorizationFailedRejection =>
          complete((StatusCodes.Forbidden, Response(StatusCodes.Forbidden.intValue, "Authorization failed")))
      }
      .handleNotFound {
        complete((StatusCodes.NotFound, Response(StatusCodes.NotFound.intValue, "Resource not found")))
      }
      .result()


}
