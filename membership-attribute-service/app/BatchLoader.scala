import java.io.File

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.model.{PutRequest, WriteRequest}
import com.github.dwhjames.awswrap.dynamodb.{AttributeValue, SingleThreadedBatchWriter}
import configuration.Config._
import models.MembershipAttributes
import org.slf4j.LoggerFactory
import sources.SalesforceCSVExport
import repositories.MembershipAttributesDynamo.membershipAttributesSerializer
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.JavaConverters._

object BatchLoader {
  val logger = LoggerFactory.getLogger(getClass)

  def writeRequest(attrs: MembershipAttributes) = {
    new WriteRequest()
      .withPutRequest(
        new PutRequest()
          .withItem(
            Map(
              "UserId" -> attrs.userId,
              "Tier" -> attrs.tier,
              "MembershipNumber" -> attrs.membershipNumber
            ).mapValues(new AttributeValue(_)).asJava
          )
      )
  }

  // This only works on a DB that contains only fresher records
  // than the ones in the CSV
  def main(args: Array[String]) = {
    args.headOption.fold({
      logger.error("A path is expected as argument")
      sys.exit(1)
    })({ path =>
      val file = new File(path)
      dynamoMapper.scan[MembershipAttributes](Map.empty).onSuccess { case existing =>
        val existingIds = existing.map(_.userId).toSet
        val requests = SalesforceCSVExport
          .membersAttributes(file)
          .filterNot { attrs => existingIds.contains(attrs.userId) }
          .map(writeRequest)
        val loader = new SingleThreadedBatchWriter(dynamoTable, AWS.credentials)
        loader.client.withRegion(Regions.EU_WEST_1)
        loader.run(requests)
      }
    })
  }
}
