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

package uk.gov.hmrc.selfassessmentapi.connectors

import uk.gov.hmrc.selfassessmentapi.config.{AppContext, WSHttp}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.selfassessmentapi.domain.Example

import scala.concurrent.Future

trait ExampleBackendConnector {

  val http: HttpGet
  val desUrl: String

  def fetchExample(saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[Example] =
    http.GET[Example](s"${desUrl}/des-example-service/sa/${saUtr.utr}/example")
}

object ExampleBackendConnector extends ExampleBackendConnector {
  override val http: HttpGet = WSHttp
  override val desUrl: String = AppContext.desUrl
}