package component.impl

import akka.actor.ActorSystem
import component.{ActorSystemComponent, ConfigComponent}
import utils.Logging

import scala.concurrent.ExecutionContext

trait ActorSystemComponentImpl extends ActorSystemComponent {
  this: ConfigComponent with Logging =>

  private val actorSystemName: String = config.getString("akka.system.name")

  implicit override val actorSystem: ActorSystem = ActorSystem(actorSystemName)

  implicit override val executionContext: ExecutionContext =
    actorSystem.dispatcher

  log.info("{} ActorSystem started", actorSystemName)

  actorSystem.registerOnTermination {
    log.info("Terminating {} ActorSystem", actorSystemName)
  }
}
