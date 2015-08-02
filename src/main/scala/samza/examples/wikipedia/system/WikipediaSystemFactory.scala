package samza.examples.wikipedia.system

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import akka.actor.ActorSystem
import akka.actor.Props
import org.apache.samza.SamzaException
import org.apache.samza.config.Config
import org.apache.samza.metrics.MetricsRegistry
import org.apache.samza.system.SystemAdmin
import org.apache.samza.system.SystemConsumer
import org.apache.samza.system.SystemFactory
import org.apache.samza.system.SystemProducer
import org.apache.samza.util.SinglePartitionWithoutOffsetsSystemAdmin

class WikipediaSystemFactory extends SystemFactory {
  override def getAdmin(systemName: String, config: Config):SystemAdmin = {
    return new SinglePartitionWithoutOffsetsSystemAdmin()
  }

  override def getConsumer(systemName: String, config: Config, registry: MetricsRegistry):SystemConsumer  = {
    val host = config.get("systems." + systemName + ".host")
    val port = config.getInt("systems." + systemName + ".port")
    implicit val actorSystem = ActorSystem("Wikipedia")
    val feed = actorSystem.actorOf(Props(new WikipediaFeed(host, port)))

    new WikipediaConsumer(feed, systemName, registry)
  }

  override def getProducer(systemName: String, config: Config, registry: MetricsRegistry):SystemProducer = {
    throw new SamzaException("You can't produce to a Wikipedia feed! How about making some edits to a Wiki, instead?")
  }
}
