/**
  *    Copyright (C) 2019 Antipathy.org <support@antipathy.org>
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
package org.antipathy.scoozie.xml.formatter

import org.antipathy.scoozie.Scoozie
import org.antipathy.scoozie.action.control._
import org.antipathy.scoozie.action.{EmailAction, HiveAction, ShellAction, SparkAction}
import org.antipathy.scoozie.configuration.{Credential, Credentials, Property, YarnConfig}
import org.antipathy.scoozie.workflow.Workflow
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable._

class OozieXmlFormatterSpec extends FlatSpec with Matchers {

  behavior of "OozieXmlFormatter"

  it should "format xml documents" in {

    implicit val credentialsOption: Option[Credentials] = Some(
      Credentials(
        Credential(name = "hive-credentials",
                   credentialsType = "hive",
                   properties = Seq(Property(name = "name", value = "value")))
      )
    )

    val yarnConfig =
      YarnConfig(jobTracker = "jobTracker", nameNode = "nameNode")

    val kill = Kill("workflow failed")

    val emailAction = EmailAction(name = "emailAction",
                                  to = Seq("a@a.com", "b@b.com"),
                                  cc = Seq.empty,
                                  subject = "message subject",
                                  body = "message body",
                                  contentTypeOption = None)
      .okTo(kill)
      .errorTo(kill)

    val shellAction = ShellAction(name = "shellAction",
                                  scriptName = "script.sh",
                                  scriptLocation = "/path/to/script.sh",
                                  commandLineArgs = Seq(),
                                  envVars = Seq(),
                                  files = Seq(),
                                  captureOutput = true,
                                  jobXmlOption = None,
                                  prepareOption = None,
                                  configuration = Scoozie.Configuration.emptyConfig,
                                  yarnConfig = yarnConfig)
      .okTo(End())
      .errorTo(emailAction)

    val join = Join("mainJoin", shellAction)

    val sparkAction = SparkAction(name = "sparkAction",
                                  sparkMasterURL = "masterURL",
                                  sparkMode = "mode",
                                  sparkJobName = "JobName",
                                  mainClass = "org.antipathy.Main",
                                  sparkJar = "/path/to/jar",
                                  sparkOptions = "spark options",
                                  commandLineArgs = Seq(),
                                  jobXmlOption = Some("/path/to/spark/settings"),
                                  prepareOption = None,
                                  configuration = Scoozie.Configuration.emptyConfig,
                                  yarnConfig = yarnConfig)
      .okTo(join)
      .errorTo(emailAction)

    val hiveAction = HiveAction(name = "hiveAction",
                                jobXmlOption = Some("/path/to/settings.xml"),
                                files = Seq(),
                                scriptName = "someScript.hql",
                                scriptLocation = "/path/to/someScript.hql",
                                parameters = Seq(),
                                prepareOption = None,
                                configuration = Scoozie.Configuration.emptyConfig,
                                yarnConfig = yarnConfig)
      .okTo(join)
      .errorTo(emailAction)

    val fork = Fork(name = "mainFork", Seq(sparkAction, hiveAction))

    val workflow = Workflow(name = "sampleWorkflow",
                            path = "",
                            transitions = Start().okTo(fork),
                            jobXmlOption = None,
                            configuration = Scoozie.Configuration.emptyConfig,
                            yarnConfig = yarnConfig)

    val formatter = new OozieXmlFormatter(width = 80, step = 4)

    val result = formatter.format(workflow)

    result should be(
      s"<workflow-app ${System.lineSeparator()}" +
      """name="sampleWorkflow" xmlns:sla="uri:oozie:sla:0.2" xmlns="uri:oozie:workflow:0.5">
        |    <global>
        |        <job-tracker>${jobTracker}</job-tracker>
        |        <name-node>${nameNode}</name-node>
        |    </global>
        |    <credentials>
        |        <credential name="hive-credentials" type="hive">
        |            <property>
        |                <name>name</name>
        |                <value>${sampleWorkflow_credentialProperty0}</value>
        |            </property>
        |        </credential>
        |    </credentials>
        |    <start to="mainFork"/>
        |    <fork name="mainFork">
        |        <path start="sparkAction"/>
        |        <path start="hiveAction"/>
        |    </fork>
        |    <action name="sparkAction" cred="hive-credentials">
        |        <spark xmlns="uri:oozie:spark-action:0.1">
        |            <job-tracker>${jobTracker}</job-tracker>
        |            <name-node>${nameNode}</name-node>
        |            <job-xml>${sparkAction_jobXml}</job-xml>
        |            <master>${sparkAction_sparkMasterURL}</master>
        |            <mode>${sparkAction_sparkMode}</mode>
        |            <name>${sparkAction_sparkJobName}</name>
        |            <class>${sparkAction_mainClass}</class>
        |            <jar>${sparkAction_sparkJar}</jar>
        |            <spark-opts>${sparkAction_sparkOptions}</spark-opts>
        |        </spark>
        |        <ok to="mainJoin"/>
        |        <error to="emailAction"/>
        |    </action>
        |    <action name="hiveAction" cred="hive-credentials">
        |        <hive xmlns="uri:oozie:hive-action:0.5">
        |            <job-tracker>${jobTracker}</job-tracker>
        |            <name-node>${nameNode}</name-node>
        |            <job-xml>${hiveAction_jobXml}</job-xml>
        |            <script>${hiveAction_scriptName}</script>
        |            <file>${hiveAction_scriptLocation}</file>
        |        </hive>
        |        <ok to="mainJoin"/>
        |        <error to="emailAction"/>
        |    </action>
        |    <join name="mainJoin" to="shellAction"/>
        |    <action name="shellAction" cred="hive-credentials">
        |        <shell xmlns="uri:oozie:shell-action:0.2">
        |            <job-tracker>${jobTracker}</job-tracker>
        |            <name-node>${nameNode}</name-node>
        |            <exec>${shellAction_scriptName}</exec>
        |            <file>${shellAction_scriptLocation}#${shellAction_scriptName}</file>
        |            <capture-output/>
        |        </shell>
        |        <ok to="end"/>
        |        <error to="emailAction"/>
        |    </action>
        |    <action name="emailAction">
        |        <email xmlns="uri:oozie:email-action:0.2">
        |            <to>${emailAction_to}</to>
        |            <subject>${emailAction_subject}</subject>
        |            <body>${emailAction_body}</body>
        |        </email>
        |        <ok to="kill"/>
        |        <error to="kill"/>
        |    </action>
        |    <kill name="kill">
        |        <message>workflow failed</message>
        |    </kill>
        |    <end name="end"/>
        |</workflow-app>
        |""".stripMargin
    )

  }
}
