/*
 * Copyright (c) 2012-2014 Snowplow Analytics Ltd. All rights reserved.
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
package inputs

// Joda-Time
import org.joda.time.DateTime

// Scalaz
import scalaz._
import Scalaz._

// Snowplow
import LoaderSpecHelpers._

// Specs2
import org.specs2.{Specification, ScalaCheck}
import org.specs2.matcher.DataTables
import org.specs2.scalaz.ValidationMatchers

// ScalaCheck
import org.scalacheck._
import org.scalacheck.Arbitrary._

class CloudfrontLoaderSpec extends Specification with DataTables with ValidationMatchers with ScalaCheck { def is =

  "This is a specification to test the CloudfrontLoader functionality"                                      ^
                                                                                                           p^
  "toCanonicalInput should return a CanonicalInput for a valid CloudFront log record"                       ! e1^
  "toCanonicalInput should return a None for a CloudFront log record not representing a Snowplow raw event" ! e2^
  "toCanonicalInput should return a Validation Failure for an invalid or corrupted CloudFront log record"   ! e3^
                                                                                                            end

  object Expected {
    val encoding = "UTF-8"
    val source   = InputSource("cloudfront", None)
  }

  // TODO: add more specs into this data table
  def e1 =
    "SPEC NAME"                 || "RAW" | "EXP. TIMESTAMP"                         | "EXP. PAYLOAD" | "EXP. IP ADDRESS"      | "EXP. USER AGENT"                                                                    | "EXP. REFERER URI"                                       | "EXP. HEADERS" | "EXP. USER ID" |
    "CloudFront #1"             !! "2013-08-29  00:18:48  LAX3  830 255.255.255.255 GET d3v6ndkyapxc2w.cloudfront.net /i  200 http://snowplowanalytics.com/analytics/index.html Mozilla/5.0%20(Windows%20NT%205.1;%20rv:23.0)%20Gecko/20100101%20Firefox/23.0 e=pv&page=Introduction%20-%20Snowplow%20Analytics%25&dtm=1377735557970&tid=567074&vp=1024x635&ds=1024x635&vid=1&duid=7969620089de36eb&p=web&tv=js-0.12.0&fp=308909339&aid=snowplowweb&lang=en-US&cs=UTF-8&tz=America%2FLos_Angeles&refr=http%3A%2F%2Fwww.metacrawler.com%2Fsearch%2Fweb%3Ffcoid%3D417%26fcop%3Dtopnav%26fpid%3D27%26q%3Dsnowplow%2Banalytics%26ql%3D&f_pdf=1&f_qt=1&f_realp=0&f_wma=1&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=0&res=1024x768&cd=24&cookie=1&url=http%3A%2F%2Fsnowplowanalytics.com%2Fanalytics%2Findex.html - Hit wQ1OBZtQlGgfM_tPEJ-lIQLsdra0U-lXgmfJfwja2KAV_SfTdT3lZg==" !
                                           DateTime.parse("2013-08-29T00:18:48.000+00:00") ! toPayload("e" -> "pv", "page" -> "Introduction - Snowplow Analytics%", "dtm" -> "1377735557970", "tid" -> "567074", "vp" -> "1024x635", "ds" -> "1024x635", "vid" -> "1", "duid" -> "7969620089de36eb", "p" -> "web", "tv" -> "js-0.12.0", "fp" -> "308909339", "aid" -> "snowplowweb", "lang" -> "en-US", "cs" -> "UTF-8", "tz" -> "America/Los_Angeles", "refr" -> "http://www.metacrawler.com/search/web?fcoid=417&fcop=topnav&fpid=27&q=snowplow+analytics&ql=", "f_pdf" -> "1", "f_qt" -> "1", "f_realp" -> "0", "f_wma" -> "1", "f_dir" -> "0", "f_fla" -> "1", "f_java" -> "1", "f_gears" -> "0", "f_ag" -> "0", "res" -> "1024x768", "cd" -> "24", "cookie" -> "1", "url" -> "http://snowplowanalytics.com/analytics/index.html") !
                                                                                                       "255.255.255.255".some ! "Mozilla/5.0%20(Windows%20NT%205.1;%20rv:23.0)%20Gecko/20100101%20Firefox/23.0".some ! "http://snowplowanalytics.com/analytics/index.html".some ! Nil            ! None           |> {
      (_, raw, timestamp, payload, ipAddress, userAgent, refererUri, headers, userId) => {

        val canonicalEvent = CloudfrontLoader
          .toCanonicalInput(raw)

        val expected = new CanonicalInput(
          timestamp  = timestamp,
          payload    = payload,
          source     = Expected.source,
          encoding   = Expected.encoding,
          ipAddress  = ipAddress,
          userAgent  = userAgent,
          refererUri = refererUri,
          headers    = headers,
          userId     = userId
          )
    
        canonicalEvent must beSuccessful(expected.some)
      }
    }

  def e2 = foreach(Seq(
    "#Version: 1.0",
    "#Fields: date time x-edge-location sc-bytes c-ip cs-method cs(Host) cs-uri-stem sc-status cs(Referer) cs(User-Agent) cs-uri-query",
    "2012-05-24  11:35:53  DFW3  3343  99.116.172.58 GET d3gs014xn8p70.cloudfront.net  /not-ice.png  200 http://www.psychicbazaar.com/2-tarot-cards/genre/all/type/all?p=5 Mozilla/5.0%20(Windows%20NT%206.1;%20WOW64;%20rv:12.0)%20Gecko/20100101%20Firefox/12.0  e=pv&page=Tarot%2520cards%2520-%2520Psychic%2520Bazaar&tid=344260&uid=288112e0a5003be2&vid=1&lang=en-US&refr=http%253A%252F%252Fwww.psychicbazaar.com%252F2-tarot-cards%252Fgenre%252Fall%252Ftype%252Fall%253Fp%253D4&f_pdf=1&f_qt=0&f_realp=0&f_wma=0&f_dir=0&f_fla=1&f_java=1&f_gears=0&f_ag=1&res=1366x768&cookie=1"
    )) { raw =>
      CloudfrontLoader.toCanonicalInput(raw) must beSuccessful(None)
    }

  // A bit of fun: the chances of generating a valid CloudFront row at random are
  // so low that we can just use ScalaCheck here
  def e3 =
    check { (raw: String) => CloudfrontLoader.toCanonicalInput(raw) must beFailing(NonEmptyList("Line does not match CloudFront header or data row formats")) }
}
