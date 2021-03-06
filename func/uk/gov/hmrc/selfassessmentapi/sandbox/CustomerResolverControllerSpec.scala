package uk.gov.hmrc.selfassessmentapi.sandbox

import uk.gov.hmrc.support.BaseFunctionalSpec

class CustomerResolverControllerSpec extends BaseFunctionalSpec {

  "Sandbox Customer Resolver" should {
    "return a 200 response with a link to /self-assessment/utr" in {
      when()
        .get("/sandbox")
        .thenAssertThat()
        .statusIs(200)
        .contentTypeIsHalJson()
        .bodyHasLink("self-assessment", """^/self-assessment/[0-9]{10}$""".r)
    }
  }

}
