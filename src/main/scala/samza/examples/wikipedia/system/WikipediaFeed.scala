package samza.examples.wikipedia.system

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import java.io.IOException
import org.schwering.irc.lib.{IRCConnection, IRCEventListener, IRCModeParser, IRCUser}
import org.slf4j.{Logger, LoggerFactory}
import scala.util.Random
import scala.collection.mutable

case class WikipediaFeedEvent(time: Long, channel: String, source: String, rawEvent: String) {
  override def hashCode() = {
    val prime = 31;
    var result: Int = 1;

    result = prime * result + Option(channel).map(_.hashCode).getOrElse(0)
    result = prime * result + Option(channel).map(_.hashCode).getOrElse(0)
    result = prime * result + Option(source).map(_.hashCode).getOrElse(0)
    result = prime * result + (time ^ (time >>> 32)).toInt
    result
  }
}
object WikipediaFeedEvent {
  def toMap(e: WikipediaFeedEvent): java.util.Map[String, Object] = {
    import scala.collection.JavaConversions.mapAsJavaMap
    Map(
      "time"     -> e.time.asInstanceOf[java.lang.Long],
      "channel"  -> e.channel,
      "source"   -> e.source,
      "rawEvent" -> e.rawEvent
    )
  }
}

class WikipediaFeedIrcListener(onEvent: WikipediaFeedEvent => Unit) extends IRCEventListener {
  private val log = LoggerFactory.getLogger(getClass);
  def onRegistered(): Unit =
    log.info("Connected")

  def onDisconnected(): Unit =
    log.info("Disconnected")

  def onError(msg: String): Unit =
    log.info(s"Error: ${msg}")

  def onError(num: Int, msg: String): Unit =
    log.info(s"Error #{num}: ${msg}")

  def onInvite(chan: String, u: IRCUser, nickPass: String): Unit =
    log.info(s"${chan}> ${u.getNick} invites ${nickPass}")

  def onJoin(chan: String, u: IRCUser): Unit =
    log.info(s"${chan}> ${u.getNick} joins")

  def onKick(chan: String, u: IRCUser, nickPass: String, msg: String): Unit =
    log.info(s"${chan}> ${u.getNick} kicks ${nickPass}")

  def onMode(u: IRCUser, nickPass: String, mode: String): Unit =
    log.info(s"Mode: ${u.getNick} sets modes ${mode} ${nickPass}")

  def onMode(chan: String, u: IRCUser, mp: IRCModeParser): Unit =
    log.info(s"${chan}> ${u.getNick} sets mode: ${mp.getLine}")

  def onNick(u: IRCUser, nickNew: String): Unit =
    log.info(s"Nick: ${u.getNick} is now known as ${nickNew}")

  def onNotice(target: String, u: IRCUser, msg: String): Unit =
    log.info(s"${target}> ${u.getNick} (notice): ${msg}")

  def onPart(chan: String, u: IRCUser, msg: String): Unit =
    log.info(s"${chan}> ${u.getNick} parts")

  def onPrivmsg(chan: String, u: IRCUser, msg: String): Unit = {
    onEvent(WikipediaFeedEvent(System.currentTimeMillis, chan, u.getNick, msg))
    log.debug(chan + "> " + u.getNick() + ": " + msg);
  }

  def onQuit(u: IRCUser, msg: String): Unit =
    log.info(s"Quit: ${u.getNick}")

  def onReply(num: Int, value: String, msg: String): Unit =
    log.info(s"Reply #${num}: ${value} ${msg}")

  def onTopic(chan: String, u: IRCUser, topic: String): Unit =
    log.info(s"${chan}> ${u.getNick} changes topic into: ${topic}")

  def onPing(p: String): Unit = ()

  def unknown(a: String, b: String, c: String, d: String): Unit =
    log.warn(s"UNKNOWN: ${a} ${b} ${c} ${d}")
}

object WikipediaFeed {
  trait Command
  case class Join(channel: String) extends Command
  case class Leave(channel: String) extends Command
  case class Listen(channel: String, ref: Option[ActorRef] = None) extends Command
  case class Unlisten(channel: String, ref: Option[ActorRef] = None) extends Command
}

class WikipediaFeed(host: String, port: Int) extends Actor {
  import WikipediaFeed._
  private val log = LoggerFactory.getLogger(getClass);

  val nick = "samza-bot-" + Math.abs(Random.nextInt());
  val conn = new IRCConnection(host, Array(port), "", nick, nick, nick)
  var listeners = mutable.Map.empty[String, Set[ActorRef]].withDefault { _ => Set.empty }

  def receive = {
    case Join(channel) =>
      conn.send(s"JOIN ${channel}")
    case Leave(channel) =>
      conn.send("PART ${channel}")
    case Listen(channel, actorRef) =>
      listeners(channel) = listeners(channel) + (actorRef getOrElse sender)
      self ! Join(channel)
    case Unlisten(channel, actorRef) =>
      listeners(channel) = listeners(channel) - (actorRef getOrElse sender)
      if (listeners(channel).isEmpty)
        self ! Leave(channel)
    case ev: WikipediaFeedEvent =>
      listeners(ev.channel).foreach { a => a ! ev }
  }

  override def preStart: Unit = {
    conn.addIRCEventListener(new WikipediaFeedIrcListener({ event =>
      self ! event
    }))
    conn.setEncoding("UTF-8");
    conn.setPong(true);
    conn.setColors(false);

    try conn.connect()
    catch {
      case e: IOException =>
        throw new RuntimeException("Unable to connect to " + host + ":" + port + ".", e);
    }
  }
}


object Main extends App {
  implicit val system = ActorSystem("wikipedia")
  val feed = system.actorOf(Props { new WikipediaFeed("irc.wikimedia.org", 6667) })
  val me = system.actorOf(Props { new Actor {
    def receive = {
      case v => println(s"received ${v}")
    }

    override def preStart =
      feed ! WikipediaFeed.Listen("#en.wikipedia")
  }})
  println("hi3")

  Thread.sleep(5000)
}
