package entities.auth

import org.joda.time.DateTime

case class AuthContext(accountId: String,
                       attempts: Int = 3,
                       oAuthToken: OAuthToken = new OAuthToken,
                       loggedInAt: DateTime = DateTime.now())
