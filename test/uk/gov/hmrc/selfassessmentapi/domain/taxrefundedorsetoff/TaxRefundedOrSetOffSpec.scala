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

package uk.gov.hmrc.selfassessmentapi.domain.taxrefundedorsetoff

import uk.gov.hmrc.selfassessmentapi.domain.ErrorCode._
import uk.gov.hmrc.selfassessmentapi.domain.JsonSpec

class TaxRefundedOrSetOffSpec extends JsonSpec {

  "format" should {
    "round trip valid TaxRefundedOrSetOff json" in {
      roundTripJson(TaxRefundedOrSetOff.example())
    }
  }

  "validate" should {
    "reject amounts with more than 2 decimal values" in {
      val testAmount = BigDecimal(1000.123)
      assertValidationError[TaxRefundedOrSetOff](
        TaxRefundedOrSetOff(amount = testAmount),
        Map("/amount" -> INVALID_MONETARY_AMOUNT), "Expected invalid amount with more than 2 decimal places")
    }

    "reject negative amount" in {
      val testAmount = BigDecimal(-1000.123)
      assertValidationError[TaxRefundedOrSetOff](
        TaxRefundedOrSetOff(amount = testAmount),
        Map("/amount" -> INVALID_MONETARY_AMOUNT), "Expected negative amount")
    }
  }

}
