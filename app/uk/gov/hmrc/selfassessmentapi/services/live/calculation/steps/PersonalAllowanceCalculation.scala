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

package uk.gov.hmrc.selfassessmentapi.services.live.calculation.steps

import uk.gov.hmrc.selfassessmentapi.PositiveOrZero
import uk.gov.hmrc.selfassessmentapi.repositories.domain.MongoLiability

object PersonalAllowanceCalculation extends CalculationStep {

  private val standardAllowance = BigDecimal(11000)

  private val taperingThreshold = BigDecimal(100000)

  override def run(selfAssessment: SelfAssessment, liability: MongoLiability): MongoLiability = {

    val personalAllowance = liability.totalTaxableIncome.map { income =>
      income - income % 2
    } map {
      case income if income <= taperingThreshold => standardAllowance
      case income if income > taperingThreshold => PositiveOrZero(standardAllowance - ((income - taperingThreshold) / 2))
    }

    liability.copy(personalAllowance = personalAllowance)
  }
}
