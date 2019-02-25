/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.controllers

import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.TransactionService

import scala.concurrent.{ExecutionContext, Future}

class TransactionController @Inject() (
                                        val authConnector: AuthConnector,
                                        val appContext: AppContext,
                                        service: TransactionService,
                                        val lisaMetrics: LisaMetrics
                                      )(implicit ec: ExecutionContext) extends LisaController {

  def getTransaction(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateHeader().async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { (_) =>
          withValidAccountId(accountId) { () =>
            service.getTransaction(lisaManager, accountId, transactionId) flatMap {
              case success: GetTransactionSuccessResponse => {
                Logger.debug("Matched Valid Response")

                lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.TRANSACTION)

                withApiVersion {
                  case Some(VERSION_1) => Future.successful(Ok(Json.toJson(success.copy(transactionType = None, supersededBy = None))))
                  case Some(VERSION_2) => Future.successful(Ok(Json.toJson(success.copy(bonusDueForPeriod = None))))
                }
              }
              case GetTransactionTransactionNotFoundResponse => {
                Logger.debug("Matched Not Found Response")

                lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.TRANSACTION)

                withApiVersion {
                  case Some(VERSION_1) => Future.successful(ErrorBonusPaymentTransactionNotFound.asResult)
                  case Some(VERSION_2) => Future.successful(ErrorTransactionNotFound.asResult)
                }
              }
              case res: GetTransactionResponse => {
                Logger.debug("Matched an error")

                withApiVersion {
                  case Some(VERSION_1) => {
                    val errorResponse = v1Errors.applyOrElse(res, { _: GetTransactionResponse =>
                      Logger.debug(s"Matched an unexpected response: $res, returning a 500 error")
                      ErrorInternalServerError
                    })

                    lisaMetrics.incrementMetrics(startTime, errorResponse.httpStatusCode, LisaMetricKeys.TRANSACTION)

                    Future.successful(errorResponse.asResult)
                  }
                  case Some(VERSION_2) => {
                    val errorResponse = v2Errors.applyOrElse(res, { _: GetTransactionResponse =>
                      Logger.debug(s"Matched an unexpected response: $res, returning a 500 error")
                      ErrorInternalServerError
                    })

                    lisaMetrics.incrementMetrics(startTime, errorResponse.httpStatusCode, LisaMetricKeys.TRANSACTION)

                    Future.successful(errorResponse.asResult)
                  }
                }
              }
            }
          }
        }
      }
    }

  private val commonErrors: PartialFunction[GetTransactionResponse, ErrorResponse] = {
    case GetTransactionAccountNotFoundResponse => ErrorAccountNotFound
    case GetTransactionServiceUnavailableResponse => ErrorServiceUnavailable
  }

  private val v1Errors: PartialFunction[GetTransactionResponse, ErrorResponse] = commonErrors.orElse({
    case GetTransactionCouldNotProcessResponse => ErrorServiceUnavailable
  })

  private val v2Errors: PartialFunction[GetTransactionResponse, ErrorResponse] = commonErrors.orElse({
    case GetTransactionCouldNotProcessResponse => ErrorCouldNotProcessWithdrawalRefund
  })

}
