/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.selfassessmentapi.repositories

import org.joda.time.{DateTime, DateTimeZone}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDouble, BSONNull, BSONObjectID, BSONString}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}
import uk.gov.hmrc.selfassessmentapi.domain.{TaxYear, TaxYearProperties}
import uk.gov.hmrc.selfassessmentapi.repositories.domain.MongoSelfAssessment

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object SelfAssessmentRepository extends MongoDbConnection {
  private lazy val repository = new SelfAssessmentMongoRepository

  def apply() = repository
}

class SelfAssessmentMongoRepository(implicit mongo: () => DB)
  extends ReactiveRepository[MongoSelfAssessment, BSONObjectID](
    "selfAssessments",
    mongo,
    domainFormat = MongoSelfAssessment.mongoFormats,
    idFormat = ReactiveMongoFormats.objectIdFormats)
    with AtomicUpdate[MongoSelfAssessment] {

  override def indexes: Seq[Index] = Seq(
    Index(Seq(("saUtr", Ascending), ("taxYear", Ascending)), name = Some("sa_utr_taxyear"), unique = true),
    Index(Seq(("lastModifiedDateTime", Ascending)), name = Some("sa_last_modified"), unique = false))


  def touch(saUtr: SaUtr, taxYear: TaxYear) = {
    val now = DateTime.now(DateTimeZone.UTC)
    for {
      result <- atomicUpsert(
        BSONDocument("saUtr" -> BSONString(saUtr.toString), "taxYear" -> BSONString(taxYear.toString)),
        BSONDocument(
          "$setOnInsert" -> BSONDocument("createdDateTime" -> BSONDateTime(now.getMillis)),
          "$set" -> BSONDocument("lastModifiedDateTime" -> BSONDateTime(now.getMillis))
        )
      )
    } yield ()
  }


  def findBy(saUtr: SaUtr, taxYear: TaxYear): Future[Option[MongoSelfAssessment]] = {
    find(
      "saUtr" -> BSONString(saUtr.toString), "taxYear" -> BSONString(taxYear.toString)
    ).map(_.headOption)
  }

  def findOlderThan(lastModified: DateTime): Future[Seq[MongoSelfAssessment]] = {
    find(
      "lastModifiedDateTime" -> BSONDocument("$lt" -> BSONDateTime(lastModified.getMillis))
    )
  }

  def delete(saUtr: SaUtr, taxYear: TaxYear): Future[Boolean] = {
    for (option <- remove("saUtr" -> saUtr.utr, "taxYear" -> taxYear.taxYear)) yield option.n > 0
  }

  def isInsertion(suppliedId: BSONObjectID, returned: MongoSelfAssessment): Boolean = suppliedId.equals(returned.id)

  def updateTaxYearProperties(saUtr: SaUtr, taxYear: TaxYear, taxYearProperties: TaxYearProperties): Future[Boolean] = {
    touch(saUtr, taxYear)
    val pensionContributionModifiers =
      taxYearProperties.pensionContributions
        .map(pensionContributions =>
          Seq(
            "$set" -> BSONDocument(
              "taxYearProperties" -> BSONDocument(
                "pensionContributions" -> BSONDocument(
                  Seq(
                    "ukRegisteredPension" -> pensionContributions.ukRegisteredPension.map(x => BSONDouble(x.doubleValue())).getOrElse(BSONNull),
                    "retirementAnnuity" -> pensionContributions.retirementAnnuity.map(x => BSONDouble(x.doubleValue())).getOrElse(BSONNull),
                    "employerScheme" -> pensionContributions.employerScheme.map(x => BSONDouble(x.doubleValue())).getOrElse(BSONNull),
                    "overseasPension" -> pensionContributions.overseasPension.map(x => BSONDouble(x.doubleValue())).getOrElse(BSONNull)))))))
        .getOrElse(Seq("$set" -> BSONDocument("pensionContributions" -> BSONNull)))
    for {
      result <- atomicUpdate(
        BSONDocument("saUtr" -> saUtr.utr, "taxYear" -> taxYear.taxYear),
        BSONDocument(pensionContributionModifiers))
    } yield result.nonEmpty
  }

  def findTaxYearProperties(saUtr: SaUtr, taxYear: TaxYear): Future[Option[TaxYearProperties]] =
    for {
      optionSa <- find("saUtr" -> saUtr.utr, "taxYear" -> taxYear.taxYear).map(_.headOption)
    } yield for {
      sa <- optionSa
      taxYearProperties <- sa.taxYearProperties
    } yield taxYearProperties
}
