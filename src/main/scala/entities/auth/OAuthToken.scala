package entities.auth

import reactivemongo.bson.{BSONDocumentHandler, Macros}

case class OAuthToken(access_token: String = java.util.UUID.randomUUID().toString,
                      token_type: String = "bearer",
                      expires_in: Int = 10000000)

object OAuthToken {
  implicit val oAuthHandler: BSONDocumentHandler[OAuthToken] = Macros.handler[OAuthToken]
}