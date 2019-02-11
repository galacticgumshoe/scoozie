package org.antipathy.scoozie.configuration

import org.antipathy.scoozie.exception.ConfigurationMissingException
import org.antipathy.scoozie.properties.OozieProperties
import org.antipathy.scoozie.xml.XmlSerializable

import scala.collection.immutable._
import scala.xml.Elem

/**
  * Oozie configuration definition
  *
  * @param configProperties the properties defined in this configuration
  */
private[scoozie] case class Configuration(configProperties: Seq[Property])
    extends XmlSerializable
    with OozieProperties {

  /**
    * Copy this configuration substituting the values for property names
    * @param actionName the name of the action calling this method
    * @return a copy of the configuration and its properties
    */
  private[scoozie] def withActionProperties(actionName: String): ActionProperties[Configuration] = {
    val mappedProps = configProperties.sortBy(_.name).zipWithIndex.map {
      case (Property(name, value), index) =>
        val p = formatProperty(s"${actionName}_property$index")
        ActionProperties[Property](Property(name, p), Map(p -> value))
      case _ =>
        throw new ConfigurationMissingException("Unknown error occurred. Please raise an issue with the developer")
    }
    ActionProperties(this.copy(mappedProps.map(_.mappedType)), mappedProps.flatMap(_.properties).toMap)
  }

  /**
    * The XML for this node
    */
  override def toXML: Elem = <configuration>
    {configProperties.map(_.toXML)}
  </configuration>

  /**
    * Get the Oozie properties for this object
    */
  override def properties: Map[String, String] =
    configProperties.map {
      case Property(name, value) => name -> value
      case _ =>
        throw new ConfigurationMissingException("Unknown error occurred while mapping properties")
    }.toMap
}
