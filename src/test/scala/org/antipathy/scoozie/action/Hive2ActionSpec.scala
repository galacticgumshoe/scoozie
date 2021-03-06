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
package org.antipathy.scoozie.action

import org.antipathy.scoozie.Scoozie
import org.antipathy.scoozie.configuration.{Configuration, Credentials, Property, YarnConfig}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable._

class Hive2ActionSpec extends FlatSpec with Matchers {

  behavior of "Hive2Action"

  it should "generate valid XML with no config and no parameters" in {

    implicit val credentialsOption: Option[Credentials] = None

    val result = Hive2Action(name = "SomeAction",
                             scriptName = "someScript.hql",
                             scriptLocation = "/path/to/someScript.hql",
                             jdbcUrl = "jdbc://server:port",
                             parameters = Seq(),
                             jobXmlOption = Some("/path/to/settings.xml"),
                             files = Seq(),
                             prepareOption = None,
                             configuration = Scoozie.Configuration.emptyConfig,
                             yarnConfig = YarnConfig(jobTracker = "jobTracker", nameNode = "nameNode")).action

    scala.xml.Utility.trim(result.toXML) should be(scala.xml.Utility.trim(<hive2 xmlns="uri:oozie:hive2-action:0.1">
          <job-tracker>{"${jobTracker}"}</job-tracker>
          <name-node>{"${nameNode}"}</name-node>
          <job-xml>{"${SomeAction_jobXml}"}</job-xml>
          <jdbc-url>{"${SomeAction_jdbcUrl}"}</jdbc-url>
          <script>{"${SomeAction_scriptName}"}</script>
          <file>{"${SomeAction_scriptLocation}"}</file>
        </hive2>))

    result.properties should be(
      Map("${nameNode}" -> "nameNode",
          "${jobTracker}" -> "jobTracker",
          "${SomeAction_jdbcUrl}" -> "jdbc://server:port",
          "${SomeAction_scriptLocation}" -> "/path/to/someScript.hql",
          "${SomeAction_scriptName}" -> "someScript.hql",
          "${SomeAction_jobXml}" -> "/path/to/settings.xml")
    )
  }

  it should "generate valid XML with parameters" in {

    implicit val credentialsOption: Option[Credentials] = None

    val result = Hive2Action(name = "SomeAction",
                             jobXmlOption = Some("/path/to/settings.xml"),
                             files = Seq(),
                             scriptName = "someScript.hql",
                             scriptLocation = "/path/to/someScript.hql",
                             jdbcUrl = "jdbc://server:port",
                             parameters = Seq("tableName=\"SomeTable\"", "date=\"2019-01-13\""),
                             prepareOption = None,
                             configuration = Scoozie.Configuration.emptyConfig,
                             yarnConfig = YarnConfig(jobTracker = "jobTracker", nameNode = "nameNode")).action

    scala.xml.Utility.trim(result.toXML) should be(scala.xml.Utility.trim(<hive2 xmlns="uri:oozie:hive2-action:0.1">
          <job-tracker>{"${jobTracker}"}</job-tracker>
          <name-node>{"${nameNode}"}</name-node>
          <job-xml>{"${SomeAction_jobXml}"}</job-xml>
          <jdbc-url>{"${SomeAction_jdbcUrl}"}</jdbc-url>
          <script>{"${SomeAction_scriptName}"}</script>
          <param>{"${SomeAction_parameter0}"}</param>
          <param>{"${SomeAction_parameter1}"}</param>
          <file>{"${SomeAction_scriptLocation}"}</file>
        </hive2>))

    result.properties should be(
      Map("${nameNode}" -> "nameNode",
          "${jobTracker}" -> "jobTracker",
          "${SomeAction_parameter0}" -> "tableName=\"SomeTable\"",
          "${SomeAction_scriptLocation}" -> "/path/to/someScript.hql",
          "${SomeAction_scriptName}" -> "someScript.hql",
          "${SomeAction_jdbcUrl}" -> "jdbc://server:port",
          "${SomeAction_parameter1}" -> "date=\"2019-01-13\"",
          "${SomeAction_jobXml}" -> "/path/to/settings.xml")
    )
  }

  it should "generate valid XML with config" in {

    implicit val credentialsOption: Option[Credentials] = None

    val result = Hive2Action(name = "SomeAction",
                             jobXmlOption = Some("/path/to/settings.xml"),
                             files = Seq(),
                             scriptName = "someScript.hql",
                             scriptLocation = "/path/to/someScript.hql",
                             jdbcUrl = "jdbc://server:port",
                             parameters = Seq(),
                             prepareOption = None,
                             configuration = Configuration(
                               Seq(Property("someProp1", "someValue2"), Property("someProp2", "someValue2"))
                             ),
                             yarnConfig = YarnConfig(jobTracker = "jobTracker", nameNode = "nameNode")).action

    scala.xml.Utility.trim(result.toXML) should be(scala.xml.Utility.trim(<hive2 xmlns="uri:oozie:hive2-action:0.1">
            <job-tracker>{"${jobTracker}"}</job-tracker>
            <name-node>{"${nameNode}"}</name-node>
            <job-xml>{"${SomeAction_jobXml}"}</job-xml>
            <configuration>
              <property>
                <name>someProp1</name>
                <value>{"${SomeAction_property0}"}</value>
              </property>
              <property>
                <name>someProp2</name>
                <value>{"${SomeAction_property1}"}</value>
              </property>
            </configuration>
            <jdbc-url>{"${SomeAction_jdbcUrl}"}</jdbc-url>
            <script>{"${SomeAction_scriptName}"}</script>
            <file>{"${SomeAction_scriptLocation}"}</file>
          </hive2>))

    result.properties should be(
      Map("${SomeAction_property1}" -> "someValue2",
          "${nameNode}" -> "nameNode",
          "${jobTracker}" -> "jobTracker",
          "${SomeAction_scriptLocation}" -> "/path/to/someScript.hql",
          "${SomeAction_scriptName}" -> "someScript.hql",
          "${SomeAction_jdbcUrl}" -> "jdbc://server:port",
          "${SomeAction_jobXml}" -> "/path/to/settings.xml",
          "${SomeAction_property0}" -> "someValue2")
    )
  }

  it should "generate valid XML with config and parameters" in {

    implicit val credentialsOption: Option[Credentials] = None

    val result = Hive2Action(name = "SomeAction",
                             jobXmlOption = Some("/path/to/settings.xml"),
                             files = Seq(),
                             scriptName = "someScript.hql",
                             scriptLocation = "/path/to/someScript.hql",
                             jdbcUrl = "jdbc://server:port",
                             parameters = Seq("tableName=\"SomeTable\"", "date=\"2019-01-13\""),
                             prepareOption = None,
                             configuration = Configuration(
                               Seq(Property("someProp1", "someValue2"), Property("someProp2", "someValue2"))
                             ),
                             yarnConfig = YarnConfig(jobTracker = "jobTracker", nameNode = "nameNode")).action

    scala.xml.Utility.trim(result.toXML) should be(scala.xml.Utility.trim(<hive2 xmlns="uri:oozie:hive2-action:0.1">
          <job-tracker>{"${jobTracker}"}</job-tracker>
          <name-node>{"${nameNode}"}</name-node>
          <job-xml>{"${SomeAction_jobXml}"}</job-xml>
          <configuration>
            <property>
              <name>someProp1</name>
              <value>{"${SomeAction_property0}"}</value>
            </property>
            <property>
              <name>someProp2</name>
              <value>{"${SomeAction_property1}"}</value>
            </property>
          </configuration>
          <jdbc-url>{"${SomeAction_jdbcUrl}"}</jdbc-url>
          <script>{"${SomeAction_scriptName}"}</script>
          <param>{"${SomeAction_parameter0}"}</param>
          <param>{"${SomeAction_parameter1}"}</param>
          <file>{"${SomeAction_scriptLocation}"}</file>
        </hive2>))

    result.properties should be(
      Map("${SomeAction_property1}" -> "someValue2",
          "${nameNode}" -> "nameNode",
          "${jobTracker}" -> "jobTracker",
          "${SomeAction_parameter0}" -> "tableName=\"SomeTable\"",
          "${SomeAction_scriptLocation}" -> "/path/to/someScript.hql",
          "${SomeAction_scriptName}" -> "someScript.hql",
          "${SomeAction_jdbcUrl}" -> "jdbc://server:port",
          "${SomeAction_parameter1}" -> "date=\"2019-01-13\"",
          "${SomeAction_jobXml}" -> "/path/to/settings.xml",
          "${SomeAction_property0}" -> "someValue2")
    )
  }
}
