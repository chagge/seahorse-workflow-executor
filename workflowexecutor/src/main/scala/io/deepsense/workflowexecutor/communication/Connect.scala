/**
 * Copyright 2015, deepsense.io
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

package io.deepsense.workflowexecutor.communication

import spray.json._

import io.deepsense.commons.json.IdJsonProtocol
import io.deepsense.models.workflows.Workflow


case class Connect(workflowId: Workflow.Id) extends MessageMQ {
  override protected def jsMessageType: JsValue = JsString(Connect.messageType)
  override protected def jsMessageBody: JsValue = {
    this.toJson(ConnectJsonProtocol.connectFormat)
  }
}

object Connect {
  val messageType: String = "connect"
}

trait ConnectJsonProtocol extends DefaultJsonProtocol with IdJsonProtocol {
  implicit def connectFormat: RootJsonFormat[Connect] = jsonFormat1(Connect.apply)
}

object ConnectJsonProtocol extends ConnectJsonProtocol