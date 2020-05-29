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
package loaders

import java.nio.charset.Charset

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits._
import com.snowplowanalytics.iglu.client.{SchemaCriterion, SchemaKey}
import com.snowplowanalytics.snowplow.CollectorPayload.thrift.model1.{
  CollectorPayload => CollectorPayload1
}
import com.snowplowanalytics.snowplow.SchemaSniffer.thrift.model1.SchemaSniffer
import com.snowplowanalytics.snowplow.collectors.thrift.SnowplowRawEvent
import org.apache.http.NameValuePair
import org.apache.thrift.TDeserializer
import org.joda.time.{DateTime, DateTimeZone}

/** Loader for Thrift SnowplowRawEvent objects. */
object ThriftLoader extends Loader[Array[Byte]] {
  private val thriftDeserializer = new TDeserializer

  private val ExpectedSchema =
    SchemaCriterion("com.snowplowanalytics.snowplow", "CollectorPayload", "thrift", 1, 0)

  /**
   * Converts the source string into a ValidatedMaybeCollectorPayload.
   * Checks the version of the raw event and calls the appropriate method.
   * @param line A serialized Thrift object Byte array mapped to a String. The method calling this
   * should encode the serialized object with `snowplowRawEventBytes.map(_.toChar)`.
   * Reference: http://stackoverflow.com/questions/5250324/
   * @return either a set of validation errors or an Option-boxed CanonicalInput object, wrapped in
   * a Scalaz ValidatioNel.
   */
  def toCollectorPayload(line: Array[Byte]): ValidatedNel[String, Option[CollectorPayload]] =
    try {
      var schema = new SchemaSniffer
      this.synchronized {
        thriftDeserializer.deserialize(
          schema,
          line
        )
      }

      if (schema.isSetSchema) {
        val actualSchema = SchemaKey.parse(schema.getSchema) match {
          case scalaz.Success(s) => s.asRight
          case scalaz.Failure(e) => e.toString.asLeft
        }
        (for {
          as <- actualSchema.leftMap(NonEmptyList.one)
          res <- if (ExpectedSchema.matches(as)) {
            convertSchema1(line).toEither
          } else {
            NonEmptyList.one(s"Verifying record as $ExpectedSchema failed: found $as").asLeft
          }
        } yield res).toValidated
      } else {
        convertOldSchema(line)
      }
    } catch {
      // TODO: Check for deserialization errors.
      case NonFatal(e) => s"Error deserializing raw event: ${e.getMessage}".invalidNel
    }

  /**
   * Converts the source string into a ValidatedMaybeCollectorPayload.
   * Assumes that the byte array is a serialized CollectorPayload, version 1.
   * @param line A serialized Thrift object Byte array mapped to a String. The method calling this
   * should encode the serialized object with`snowplowRawEventBytes.map(_.toChar)`.
   * Reference: http://stackoverflow.com/questions/5250324/
   * @return either a set of validation errors or an Option-boxed CanonicalInput object, wrapped in
   * a Scalaz ValidatioNel.
   */
  private def convertSchema1(line: Array[Byte]): ValidatedNel[String, Option[CollectorPayload]] = {
    var collectorPayload = new CollectorPayload1
    this.synchronized {
      thriftDeserializer.deserialize(
        collectorPayload,
        line
      )
    }

    val querystring = parseQuerystring(
      Option(collectorPayload.querystring),
      Charset.forName(collectorPayload.encoding)
    )

    val hostname = Option(collectorPayload.hostname)
    val userAgent = Option(collectorPayload.userAgent)
    val refererUri = Option(collectorPayload.refererUri)
    val networkUserId = Option(collectorPayload.networkUserId)

    val headers = Option(collectorPayload.headers).map(_.toList).getOrElse(Nil)

    val ip = IpAddressExtractor.extractIpAddress(headers, collectorPayload.ipAddress).some // Required

    val api = Option(collectorPayload.path) match {
      case None => "Request does not contain a path".invalidNel
      case Some(p) => CollectorApi.parse(p).toValidatedNel
    }

    (querystring.toValidatedNel, api).mapN { (q, a) =>
      CollectorPayload(
        q,
        collectorPayload.collector,
        collectorPayload.encoding,
        hostname,
        Some(new DateTime(collectorPayload.timestamp, DateTimeZone.UTC)),
        ip,
        userAgent,
        refererUri,
        headers,
        networkUserId,
        a,
        Option(collectorPayload.contentType),
        Option(collectorPayload.body)
      ).some
    }
  }

  /**
   * Converts the source string into a ValidatedMaybeCollectorPayload. Assumes that the byte array
   * is an old serialized SnowplowRawEvent which is not self-describing.
   * @param line A serialized Thrift object Byte array mapped to a String. The method calling this
   * should encode the serialized object with `snowplowRawEventBytes.map(_.toChar)`.
   * Reference: http://stackoverflow.com/questions/5250324/
   * @return either a set of validation errors or an Option-boxed CanonicalInput object, wrapped in
   * a Scalaz ValidatioNel.
   */
  private def convertOldSchema(
    line: Array[Byte]
  ): ValidatedNel[String, Option[CollectorPayload]] = {
    var snowplowRawEvent = new SnowplowRawEvent
    this.synchronized {
      thriftDeserializer.deserialize(
        snowplowRawEvent,
        line
      )
    }

    val querystring = parseQuerystring(
      Option(snowplowRawEvent.payload.data),
      Charset.forName(snowplowRawEvent.encoding)
    )

    val hostname = Option(snowplowRawEvent.hostname)
    val userAgent = Option(snowplowRawEvent.userAgent)
    val refererUri = Option(snowplowRawEvent.refererUri)
    val networkUserId = Option(snowplowRawEvent.networkUserId)

    val headers = Option(snowplowRawEvent.headers).map(_.toList).getOrElse(Nil)

    val ip = IpAddressExtractor.extractIpAddress(headers, snowplowRawEvent.ipAddress).some // Required

    (querystring.toValidatedNel) map { (q: List[NameValuePair]) =>
      Some(
        CollectorPayload(
          q,
          snowplowRawEvent.collector,
          snowplowRawEvent.encoding,
          hostname,
          Some(new DateTime(snowplowRawEvent.timestamp, DateTimeZone.UTC)),
          ip,
          userAgent,
          refererUri,
          headers,
          networkUserId,
          CollectorApi.SnowplowTp1, // No way of storing API vendor/version in Thrift yet, assume Snowplow TP1
          None, // No way of storing content type in Thrift yet
          None // No way of storing request body in Thrift yet
        )
      )
    }
  }
}
