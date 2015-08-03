package samza.examples.wikipedia.system

import akka.actor.Props
import akka.actor.{Actor, ActorRef, ActorRefFactory}
import org.apache.samza.Partition
import org.apache.samza.metrics.MetricsRegistry
import org.apache.samza.system.{IncomingMessageEnvelope, SystemStreamPartition}
import org.apache.samza.util.BlockingEnvelopeMap
import org.slf4j.LoggerFactory

class WikipediaConsumer(feed: ActorRef, systemName: String, registry: MetricsRegistry)(implicit actorRefFactory: ActorRefFactory) extends BlockingEnvelopeMap {
  val log = LoggerFactory.getLogger(getClass)
  case object Start
  case object Stop
  case class Register(channel: String)

  val actor = actorRefFactory.actorOf(Props(new Actor {
    var channels = Set.empty[String]

    val stopped: Receive = {
      case Start =>
        channels.foreach { c => feed ! WikipediaFeed.Listen(c, Some(self)) }
        context.become(started)
      case Stop =>
        ()
      case Register(channel) =>
        channels = channels + channel

    }

    val started: Receive = {
      case Start =>
        ()
      case Stop =>
        channels.foreach { c => feed ! WikipediaFeed.Unlisten(c, Some(self)) }
      case event: WikipediaFeedEvent =>
        val systemStreamPartition = new SystemStreamPartition(systemName, event.channel, new Partition(0));

        try {
          log.info(s"le event ${event}")
          put(systemStreamPartition, new IncomingMessageEnvelope(systemStreamPartition, null, null, event))
        } catch { case e: Exception =>
          System.err.println(e);
        }
      case Register(channel) =>
        channels = channels + channel
        feed ! WikipediaFeed.Listen(channel, Some(self))
    }

    def receive = stopped
  }))

  override def start: Unit =
    actor ! Start
  override def stop: Unit =
    actor ! Stop

  override def register(systemStreamPartition: SystemStreamPartition, startingOffset: String):Unit = {
    log.info(s"le register ${systemStreamPartition.getStream()}")
    super.register(systemStreamPartition, startingOffset);
    actor ! Register(systemStreamPartition.getStream())
  }
}
