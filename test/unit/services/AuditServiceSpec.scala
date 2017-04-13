/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.services

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrier

class AuditServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "AuditService" must {

    "build an audit event with the correct mandatory details" in {
      val event = SUT.createEvent("investorCreated", "/create", Map("investorID" -> "1234567890"))

      event.auditSource mustBe "lisa-api"
      event.auditType mustBe "investorCreated"
    }

    "build an audit event with the correct tags" in {
      val event = SUT.createEvent("investorCreated", "/create", Map("investorID" -> "1234567890"))

      event.tags must contain ("path" -> "/create")
      event.tags must contain ("transactionName" -> "investorCreated")
      event.tags must contain key "clientIP"
    }

    "build an audit event with the correct detail" in {
      val event = SUT.createEvent("investorCreated", "/create", Map("investorID" -> "1234567890", "investorNINO" -> "AB123456D"))

      event.detail must contain ("investorID" -> "1234567890")
      event.detail must contain ("investorNINO" -> "AB123456D")
      event.detail must contain key "Authorization"
    }

    "send an event via the audit connector" in {
      val event = SUT.createEvent("investorCreated", "/create", Map("investorID" -> "1234567890"))

      val auditResult = SUT.sendEvent(event)

      verify(mockAuditConnector).sendEvent(any())(any(), any())
    }

  }

  implicit val hc:HeaderCarrier = HeaderCarrier()
  val mockAuditConnector = mock[AuditConnector]

  object SUT extends AuditService {
    override val connector:AuditConnector = mockAuditConnector
  }
}
