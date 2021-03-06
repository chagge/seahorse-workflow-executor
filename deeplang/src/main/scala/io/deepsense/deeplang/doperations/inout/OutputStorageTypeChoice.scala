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

package io.deepsense.deeplang.doperations.inout

import io.deepsense.deeplang.params.choice.{Choice, ChoiceParam}
import io.deepsense.deeplang.params.{StorageType, Param, StringParam}

sealed trait OutputStorageTypeChoice extends Choice {
  import OutputStorageTypeChoice._

  override val choiceOrder: List[Class[_ <: OutputStorageTypeChoice]] = List(
    classOf[File],
    classOf[Jdbc])
}

object OutputStorageTypeChoice {

  case class File() extends OutputStorageTypeChoice {

    override val name: String = StorageType.FILE.toString

    val outputFile = StringParam(
      name = "output file",
      description = "Output file path.")

    def getOutputFile: String = $(outputFile)
    def setOutputFile(value: String): this.type = set(outputFile, value)

    val fileFormat = ChoiceParam[OutputFileFormatChoice](
      name = "format",
      description = "Format of the output file.")
    setDefault(fileFormat, OutputFileFormatChoice.Csv())

    def getFileFormat: OutputFileFormatChoice = $(fileFormat)
    def setFileFormat(value: OutputFileFormatChoice): this.type = set(fileFormat, value)

    override val params = declareParams(outputFile, fileFormat)
  }

  case class Jdbc()
    extends OutputStorageTypeChoice
    with JdbcParameters {

    override val name: String = StorageType.JDBC.toString
    override val params: Array[Param[_]] =
      declareParams(jdbcUrl, jdbcDriverClassName, jdbcTableName)
  }
}
