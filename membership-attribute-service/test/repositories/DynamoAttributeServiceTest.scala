package repositories

import java.util.UUID

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.github.dwhjames.awswrap.dynamodb.{AmazonDynamoDBScalaClient, Schema}
import models.Attributes
import org.joda.time.LocalDate
import org.specs2.mutable.Specification
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.MembershipAttributesSerializer.AttributeNames
import services.ScanamoAttributeService

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Depends upon DynamoDB Local to be running on the default port of 8000.
 *
 * Amazon's embedded version doesn't work with an async client, so using https://github.com/grahamar/sbt-dynamodb
 */
class DynamoAttributeServiceTest extends Specification {

  private val awsDynamoClient = new AmazonDynamoDBAsyncClient(new BasicAWSCredentials("foo", "bar"))
  awsDynamoClient.setEndpoint("http://localhost:8000")

  private val testTable = "MembershipAttributes-TEST"
  implicit private val serializer = MembershipAttributesSerializer(testTable)
  private val repo = new ScanamoAttributeService(awsDynamoClient, testTable)

  val tableRequest =
    new CreateTableRequest()
      .withTableName(testTable)
      .withProvisionedThroughput(
        Schema.provisionedThroughput(10L, 5L))
      .withAttributeDefinitions(
        Schema.stringAttribute(AttributeNames.userId))
      .withKeySchema(
        Schema.hashKey(AttributeNames.userId))

  private val dynamoClient = new AmazonDynamoDBScalaClient(awsDynamoClient)
  val createTableResult = Await.result(dynamoClient.createTable(tableRequest), 5.seconds)

  "get" should {
    "retrieve attributes for given user" in {
      val userId = UUID.randomUUID().toString
      val attributes = Attributes(
        userId,
        Tier = Some("Patron"),
        MembershipNumber =  Some("abc"),
        RecurringContributionPaymentPlan = Some("Monthly Contribution"),
        MembershipJoinDate = Some(new LocalDate(2017, 6, 13)))

      val result = for {
        insertResult <- repo.set(attributes)
        retrieved <- repo.get(userId)
      } yield retrieved

      Await.result(result, 5.seconds) shouldEqual Some(attributes)
    }

    "retrieve not found api error when attributes not found for user" in {
      val result = for {
        retrieved <- repo.get(UUID.randomUUID().toString)
      } yield retrieved

      Await.result(result, 5.seconds) shouldEqual None
    }
  }

  "getMany" should {

    val testUsers = Seq(
      Attributes(UserId = "1234", Tier = Some("Partner")),
      Attributes(UserId = "2345", Tier = Some("Partner"), MembershipJoinDate = Some(new LocalDate(2017, 6, 12))),
      Attributes(UserId = "3456", Tier = Some("Partner"), MembershipJoinDate = Some(new LocalDate(2017, 6, 11))),
      Attributes(UserId = "4567", Tier = Some("Partner"), MembershipJoinDate = Some(new LocalDate(2017, 6, 10)))
    )

    "Fetch many people by user id" in {
      Await.result(Future.sequence(testUsers.map(repo.set)), 5.seconds)
      Await.result(repo.getMany(List("1234", "3456", "abcd")), 5.seconds) mustEqual Seq(
        Attributes(UserId = "1234", Tier = Some("Partner")),
        Attributes(UserId = "3456", Tier = Some("Partner"), MembershipJoinDate = Some(new LocalDate(2017, 6, 11)))
      )
    }
  }
}
