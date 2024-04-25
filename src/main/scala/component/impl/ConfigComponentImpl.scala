package component.impl

import com.typesafe.config.{Config, ConfigFactory}
import component.ConfigComponent

trait ConfigComponentImpl extends ConfigComponent {
  override def config: Config = ConfigFactory.load()
}
