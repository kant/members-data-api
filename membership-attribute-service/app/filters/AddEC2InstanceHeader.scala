package filters

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.Future

object AddEC2InstanceHeader extends Filter {

  // http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
  lazy val instanceIdF = WS.url("http://169.254.169.254/latest/meta-data/instance-id").get().map(_.body)

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = for {
    result <- nextFilter(requestHeader)
  } yield {
    val instanceIdOpt = instanceIdF.value.flatMap(_.toOption) // We don't want to block if the value is not available
    instanceIdOpt.fold(result)(instanceId => result.withHeaders("X-EC2-instance-id" -> instanceId))
  }

}
