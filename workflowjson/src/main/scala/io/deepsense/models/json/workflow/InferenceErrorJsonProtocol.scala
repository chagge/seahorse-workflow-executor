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

package io.deepsense.models.json.workflow

import spray.httpx.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

import io.deepsense.deeplang.exceptions.DeepLangException

trait InferenceErrorJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object InferenceErrorMappingFormat extends JsonFormat[DeepLangException] {
    override def write(exc: DeepLangException): JsValue = JsString(exc.message)
    override def read(value: JsValue): DeepLangException = ???
  }
}
