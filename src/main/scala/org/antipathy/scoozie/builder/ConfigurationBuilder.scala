package org.antipathy.scoozie.builder

import com.typesafe.config.Config
import org.antipathy.scoozie.Scoozie
import org.antipathy.scoozie.configuration.Configuration
import scala.collection.JavaConverters._
import org.antipathy.scoozie.configuration.{Credential, Credentials}
import com.typesafe.config.ConfigException
import org.antipathy.scoozie.exception.ConfigurationMissingException
import scala.util.control.NonFatal

/**
  *  Object for building configuration objects from Hocon
  */
private[scoozie] object ConfigurationBuilder {

  /**
    * Build a configuration object from the passed in config file
    * @param config the configuration to build from
    * @return a configuration object
    */
  def buildConfiguration(config: Config): Configuration =
    if (config.hasPath("configuration")) {
      Scoozie.Configuration.configuration(
        config
          .getConfig("configuration")
          .entrySet()
          .asScala
          .toSeq
          .sortBy(_.getKey)
          .map { i =>
            i.getKey -> i.getValue.render()
          }
          .toMap
      )
    } else Scoozie.Configuration.emptyConfiguration

  /**
    * Build a credentials object from the passed in config file
    * @param config the config to build from
    * @return an optional credentials object
    */
  def buildCredentials(config: Config): Option[Credentials] =
    if (config.hasPath("credentials")) {
      val credentialsConfig = config.getConfig("credentials")
      Some(
        Credentials(
          Credential(getStringValue(credentialsConfig, "name"),
                     getStringValue(credentialsConfig, "type"),
                     buildConfiguration(credentialsConfig).configProperties)
        )
      )
    } else None

  /**
    * wrap missing keys with a more helpful message
    */
  private def getStringValue(config: Config, path: String): String =
    try {
      config.getString(path)
    } catch {
      case c: ConfigException => throw new ConfigurationMissingException(s"${c.getMessage} in credentials")
      case NonFatal(unknown)  => throw unknown
    }
}