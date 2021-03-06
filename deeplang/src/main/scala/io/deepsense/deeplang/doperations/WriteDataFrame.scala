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

package io.deepsense.deeplang.doperations

import java.io.IOException
import java.sql.Timestamp
import java.util.Properties

import scala.reflect.runtime.{universe => ru}

import org.apache.spark.SparkException
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import io.deepsense.commons.datetime.DateTimeConverter
import io.deepsense.commons.utils.Version
import io.deepsense.deeplang.DOperation.Id
import io.deepsense.deeplang.doperables.dataframe.DataFrame
import io.deepsense.deeplang.doperations.exceptions.{DeepSenseIOException, UnsupportedColumnTypeException, WriteFileException}
import io.deepsense.deeplang.doperations.inout._
import io.deepsense.deeplang.params.Params
import io.deepsense.deeplang.params.choice.ChoiceParam
import io.deepsense.deeplang.{DOperation1To0, ExecutionContext, FileSystemClient}

case class WriteDataFrame()
  extends DOperation1To0[DataFrame]
  with Params {

  override val id: Id = "9e460036-95cc-42c5-ba64-5bc767a40e4e"
  override val name: String = "Write DataFrame"
  override val description: String = "Writes a DataFrame to a file or database"

  override val since: Version = Version(0, 4, 0)

  val storageType = ChoiceParam[OutputStorageTypeChoice](
    name = "data storage type",
    description = "Storage type.")

  def getStorageType: OutputStorageTypeChoice = $(storageType)
  def setStorageType(value: OutputStorageTypeChoice): this.type = set(storageType, value)

  val params = declareParams(storageType)
  setDefault(storageType, OutputStorageTypeChoice.File())

  override protected def _execute(context: ExecutionContext)(dataFrame: DataFrame): Unit = {
    try {
      getStorageType match {
        case (jdbcChoice: OutputStorageTypeChoice.Jdbc) =>
          writeToJdbc(jdbcChoice, context, dataFrame)
        case (fileChoice: OutputStorageTypeChoice.File) =>
          writeToFile(fileChoice, context, dataFrame)
      }
    } catch {
      case e: IOException =>
        logger.error(s"WriteDataFrame error. Could not write file to designated storage", e)
        throw DeepSenseIOException(e)
    }
  }

  private def writeToJdbc(
      jdbcChoice: OutputStorageTypeChoice.Jdbc,
      context: ExecutionContext,
      dataFrame: DataFrame): Unit = {

    val properties = new Properties()
    properties.setProperty("driver", jdbcChoice.getJdbcDriverClassName)

    val jdbcUrl = jdbcChoice.getJdbcUrl
    val jdbcTableName = jdbcChoice.getJdbcTableName

    dataFrame.sparkDataFrame.write.jdbc(jdbcUrl, jdbcTableName, properties)
  }

  private def writeToFile(
      fileChoice: OutputStorageTypeChoice.File,
      context: ExecutionContext,
      dataFrame: DataFrame): Unit = {

    val path =
      FileSystemClient.replaceLeadingTildeWithHomeDirectory(fileChoice.getOutputFile)

    try {
      fileChoice.getFileFormat match {
        case (csvChoice: OutputFileFormatChoice.Csv) =>
          requireNoComplexTypes(dataFrame)
          val namesIncluded = csvChoice.getCsvNamesIncluded
          val columnSeparator = csvChoice.determineColumnSeparator().toString
          prepareDataFrameForCsv(context, dataFrame)
            .sparkDataFrame
            .write.format("com.databricks.spark.csv")
            .option("header", if (namesIncluded) "true" else "false")
            .option("delimiter", columnSeparator)
            .save(path)

        case OutputFileFormatChoice.Parquet() =>
          // TODO: DS-1480 Writing DF in parquet format when column names contain forbidden chars
          dataFrame.sparkDataFrame.write.parquet(path)

        case OutputFileFormatChoice.Json() =>
          dataFrame.sparkDataFrame.write.json(path)
      }
    } catch {
      case e: SparkException =>
        logger.error(s"WriteDataFrame error: Spark problem. Unable to write file to $path", e)
        throw WriteFileException(path, e)
    }
  }

  private def requireNoComplexTypes(dataFrame: DataFrame): Unit = {
    dataFrame.sparkDataFrame.schema.fields.map(structField =>
        (structField.dataType, structField.name)
    ).foreach {
      case (dataType, columnName) => dataType match {
        case _: ArrayType | _: MapType | _: StructType =>
          throw UnsupportedColumnTypeException(columnName, dataType)
        case _ => ()
      }
    }

  }

  private def prepareDataFrameForCsv(context: ExecutionContext, dataFrame: DataFrame): DataFrame = {
    val schema = dataFrame.sparkDataFrame.schema

    def stringifySelectedTypes(schema: StructType): StructType = {
      StructType(
        schema.map {
          case field: StructField => field.copy(dataType = StringType)
        }
      )
    }

    context.dataFrameBuilder.buildDataFrame(
      stringifySelectedTypes(schema),
      dataFrame.sparkDataFrame.map(WriteDataFrame.stringifySelectedCells(schema)))
  }

  @transient
  override lazy val tTagTI_0: ru.TypeTag[DataFrame] = ru.typeTag[DataFrame]
}

object WriteDataFrame {
  def stringifySelectedCells(originalSchema: StructType)(row: Row): Row = {
    Row.fromSeq(
      row.toSeq.zipWithIndex map { case (value, index) =>
        (value, originalSchema(index).dataType) match {
          case (null, _) => ""
          case (_, BooleanType) =>
            if (value.asInstanceOf[Boolean]) "1" else "0"
          case (_, TimestampType) =>
            DateTimeConverter.toString(
              DateTimeConverter.fromMillis(value.asInstanceOf[Timestamp].getTime))
          case (x, _) => value.toString
        }
      })
  }
}
