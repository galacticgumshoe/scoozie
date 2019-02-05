package org.antipathy.scoozie.action

import org.scalatest.{FlatSpec, Matchers}
import org.antipathy.scoozie.Scoozie
import org.antipathy.scoozie.configuration.Credentials
import scala.collection.immutable._
import scala.xml

class SqoopActionSpec extends FlatSpec with Matchers {

  behavior of "SqoopAction"

  it should "generate valid XML with a command" in {

    implicit val credentials: Option[Credentials] = Scoozie.Config.emptyCredentials

    val result = Scoozie.Action
      .sqoopAction(name = "sqoopAction",
                   command = "sqoopCommand",
                   files = Seq("one", "two"),
                   configuration = Scoozie.Config.configuration(Map("key" -> "value")),
                   yarnConfig = Scoozie.Config.yarnConfiguration("someJT", "someNN"),
                   prepareOption = None)
      .action

    xml.Utility.trim(result.toXML) should be(xml.Utility.trim(<sqoop xmlns="uri:oozie:sqoop-action:0.3">
        <job-tracker>{"${jobTracker}"}</job-tracker>
        <name-node>{"${nameNode}"}</name-node>
        <configuration>
          <property>
            <name>key</name>
            <value>{"${sqoopAction_property0}"}</value>
          </property>
        </configuration>
        <command>{"${sqoopAction_command}"}</command>
        <file>{"${sqoopAction_files0}"}</file>
        <file>{"${sqoopAction_files1}"}</file>
      </sqoop>))

    result.properties should be(
      Map("${sqoopAction_files0}" -> "one",
          "${sqoopAction_files1}" -> "two",
          "${sqoopAction_property0}" -> "value",
          "${sqoopAction_command}" -> "sqoopCommand")
    )
  }

  it should "generate valid XML with arguments" in {

    implicit val credentials: Option[Credentials] = Scoozie.Config.emptyCredentials

    val result = Scoozie.Action
      .sqoopAction(name = "sqoopAction",
                   args = Seq("arg1", "arg2"),
                   files = Seq("one", "two"),
                   configuration = Scoozie.Config.configuration(Map("key" -> "value")),
                   yarnConfig = Scoozie.Config.yarnConfiguration("someJT", "someNN"),
                   prepareOption = None)
      .action

    xml.Utility.trim(result.toXML) should be(xml.Utility.trim(<sqoop xmlns="uri:oozie:sqoop-action:0.3">
      <job-tracker>{"${jobTracker}"}</job-tracker>
      <name-node>{"${nameNode}"}</name-node>
      <configuration>
        <property>
          <name>key</name>
          <value>{"${sqoopAction_property0}"}</value>
        </property>
      </configuration>
      <arg>{"${sqoopAction_arguments0}"}</arg>
      <arg>{"${sqoopAction_arguments1}"}</arg>
      <file>{"${sqoopAction_files0}"}</file>
      <file>{"${sqoopAction_files1}"}</file>
    </sqoop>))

    result.properties should be(
      Map("${sqoopAction_files1}" -> "two",
          "${sqoopAction_arguments0}" -> "arg1",
          "${sqoopAction_arguments1}" -> "arg2",
          "${sqoopAction_property0}" -> "value",
          "${sqoopAction_files0}" -> "one")
    )
  }

}