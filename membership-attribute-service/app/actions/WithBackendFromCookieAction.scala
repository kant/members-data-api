package actions

import components._
import configuration.Config
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ActionRefiner, Request, Result}
import services.IdentityAuthService

import scala.concurrent.Future

object WithBackendFromCookieAction extends ActionRefiner[Request, BackendRequest] {
  override protected def refine[A](request: Request[A]): Future[Either[Result, BackendRequest[A]]] = Future {
    val firstName = IdentityAuthService.username(request).flatMap(_.split(' ').headOption) //Identity checks for test users by first name
    val exists = firstName.exists(Config.testUsernames.isValid)

    val backendConf = if (exists) {
      TestTouchpointComponents
    } else {
      NormalTouchpointComponents
    }
    Right(new BackendRequest[A](backendConf, request))
  }
}
