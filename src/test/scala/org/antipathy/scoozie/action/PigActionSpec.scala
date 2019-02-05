package org.antipathy.scoozie.action

import org.scalatest.{FlatSpec, Matchers}
import org.antipathy.scoozie.configuration.YarnConfig
import org.antipathy.scoozie.configuration.Credentials
import scala.xml
import scala.collection.immutable._
import org.antipathy.scoozie.Scoozie

class PigActionSpec extends FlatSpec with Matchers {

  behavior of "PigAction"

  it should "generate valid XML" in {

    implicit val credentialsOption: Option[Credentials] = None

    val result = PigAction(name = "pigAction",
                           script = "/path/to/script",
                           params = Seq(),
                           configuration = Scoozie.Config.emptyConfiguration,
                           yarnConfig = YarnConfig(jobTracker = "jobTracker", nameNode = "nameNode")).action

    xml.Utility.trim(result.toXML) should be(xml.Utility.trim(<pig>
          <job-tracker>{"${jobTracker}"}</job-tracker>
          <name-node>{"${nameNode}"}</name-node>
          <script>{"${pigAction_script}"}</script>
        </pig>))

    result.properties should be(Map("${pigAction_script}" -> "/path/to/script"))
  }

  it should "generate valid XML with script arguments" in {

    implicit val credentialsOption: Option[Credentials] = None

    val result = PigAction(name = "pigAction",
                           script = "/path/to/script",
                           params = Seq("one", "two"),
                           configuration = Scoozie.Config.emptyConfiguration,
                           yarnConfig = YarnConfig(jobTracker = "jobTracker", nameNode = "nameNode")).action

    xml.Utility.trim(result.toXML) should be(xml.Utility.trim(<pig>
          <job-tracker>{"${jobTracker}"}</job-tracker>
          <name-node>{"${nameNode}"}</name-node>
          <script>{"${pigAction_script}"}</script>
          <argument>-param</argument>
          <argument>{"${pigAction_param0}"}</argument>
          <argument>-param</argument>
          <argument>{"${pigAction_param1}"}</argument>
        </pig>))

    result.properties should be(
      Map("${pigAction_param0}" -> "one", "${pigAction_script}" -> "/path/to/script", "${pigAction_param1}" -> "two")
    )
  }
}
