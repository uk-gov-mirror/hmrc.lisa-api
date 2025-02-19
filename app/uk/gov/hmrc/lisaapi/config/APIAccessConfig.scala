/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.config

import play.api.Configuration

case class APIAccessConfig(value: Option[Configuration]) {

  val PUBLIC = "PUBLIC"

  def accessType: String = {
    value match {
      case Some(config) => config.getOptional[String]("type").getOrElse(PUBLIC)
      case None => PUBLIC
    }
  }

  def whiteListedApplicationIds: Option[Seq[String]] = {
    value match {
      case Some(config) => config.getOptional[Seq[String]]("white-list.applicationIds")
      case None => Some(Seq.empty)
    }
  }
}
