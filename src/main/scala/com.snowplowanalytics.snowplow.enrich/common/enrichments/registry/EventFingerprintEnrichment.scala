/*
 * Copyright (c) 2012-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich.common
package enrichments.registry

import com.snowplowanalytics.iglu.client.{SchemaCriterion, SchemaKey}
import com.snowplowanalytics.iglu.client.validation.ProcessingMessageMethods._
import io.circe._
import org.apache.commons.codec.digest.DigestUtils
import scalaz._
import Scalaz._

import utils.ScalazCirceUtils

/** Lets us create an EventFingerprintEnrichmentConfig from a Json. */
object EventFingerprintEnrichmentConfig extends ParseableEnrichment {

  val supportedSchema =
    SchemaCriterion(
      "com.snowplowanalytics.snowplow",
      "event_fingerprint_config",
      "jsonschema",
      1,
      0
    )

  /**
   * Creates an EventFingerprintEnrichment instance from a JValue.
   * @param config The enrichment JSON
   * @param schemaKey provided for the enrichment, must be supported fby this enrichment
   * @return a configured EventFingerprintEnrichment instance
   */
  def parse(config: Json, schemaKey: SchemaKey): ValidatedNelMessage[EventFingerprintEnrichment] =
    isParseable(config, schemaKey).flatMap(conf => {
      (for {
        excludedParameters <- ScalazCirceUtils
          .extract[List[String]](config, "parameters", "excludeParameters")
        algorithmName <- ScalazCirceUtils.extract[String](config, "parameters", "hashAlgorithm")
        algorithm <- getAlgorithm(algorithmName)
      } yield EventFingerprintEnrichment(algorithm, excludedParameters)).toValidationNel
    })

  /**
   * Look up the fingerprinting algorithm by name
   * @param algorithmName
   * @return A hashing algorithm
   */
  private[registry] def getAlgorithm(algorithmName: String): ValidatedMessage[String => String] =
    algorithmName match {
      case "MD5" => ((s: String) => DigestUtils.md5Hex(s)).success
      case "SHA1" => ((s: String) => DigestUtils.sha1Hex(s)).success
      case "SHA256" => ((s: String) => DigestUtils.sha256Hex(s)).success
      case "SHA384" => ((s: String) => DigestUtils.sha384Hex(s)).success
      case "SHA512" => ((s: String) => DigestUtils.sha512Hex(s)).success
      case other =>
        s"[$other] is not a supported event fingerprint generation algorithm".toProcessingMessage.fail
    }

}

object EventFingerprintEnrichment {
  private val UnitSeparator = "\u001f"
}

/**
 * Config for an event fingerprint enrichment
 * @param algorithm Hashing algorithm
 * @param excludedParameters List of querystring parameters to exclude from the calculation
 * @return Event fingerprint
 */
final case class EventFingerprintEnrichment(
  algorithm: String => String,
  excludedParameters: List[String]
) extends Enrichment {

  /**
   * Calculate an event fingerprint using all querystring fields except the excludedParameters
   * @param parameterMap
   * @return Event fingerprint
   */
  def getEventFingerprint(parameterMap: Map[String, String]): String = {
    val builder = new StringBuilder
    parameterMap.toList.sortWith(_._1 < _._1).foreach {
      case (key, value) =>
        if (!excludedParameters.contains(key)) {
          builder.append(key)
          builder.append(EventFingerprintEnrichment.UnitSeparator)
          builder.append(value)
          builder.append(EventFingerprintEnrichment.UnitSeparator)
        }
    }
    algorithm(builder.toString)
  }
}
