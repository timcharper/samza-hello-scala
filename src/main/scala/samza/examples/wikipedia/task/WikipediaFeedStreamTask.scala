package samza.examples.wikipedia.task

import java.util.Map
import org.apache.samza.system.IncomingMessageEnvelope
import org.apache.samza.system.OutgoingMessageEnvelope
import org.apache.samza.system.SystemStream
import org.apache.samza.task.MessageCollector
import org.apache.samza.task.StreamTask
import org.apache.samza.task.TaskCoordinator
import samza.examples.wikipedia.system.WikipediaFeedEvent

/**
 * This task is very simple. All it does is take messages that it receives, and
 * sends them to a Kafka topic called wikipedia-raw.
 */
class WikipediaFeedStreamTask extends StreamTask {
  override def process(envelope: IncomingMessageEnvelope, collector: MessageCollector, coordinator: TaskCoordinator):Unit = {
    val outgoingMap = WikipediaFeedEvent.toMap( envelope.getMessage().asInstanceOf[WikipediaFeedEvent])
    collector.send(new OutgoingMessageEnvelope(WikipediaFeedStreamTask.OUTPUT_STREAM, outgoingMap))
  }
}

object WikipediaFeedStreamTask {
  val OUTPUT_STREAM = new SystemStream("kafka", "wikipedia-raw")
}
