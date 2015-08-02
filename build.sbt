scalaVersion := "2.11.7"

name := "scamza"

description := """
Hello Samza, implemented in Scala
"""

val SAMZA_SHELL_VERSION = "0.9.1"
val SAMZA_VERSION = "0.10.0-SNAPSHOT"
val KAFKA_VERSION = "0.8.2.1"
val HADOOP_VERSION = "2.4.1"
val SLF4J_VERSION = "1.7.7"
val akkaVersion = "2.3.12"

version := "0.1"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

Revolver.settings

libraryDependencies ++= Seq(
  "org.apache.samza" % "samza-shell" % SAMZA_VERSION,

  // compile(group: 'org.codehaus.jackson', name: 'jackson-jaxrs', version: '1.8.5')
  "org.slf4j" % "slf4j-api" % SLF4J_VERSION,
  "org.slf4j" % "slf4j-log4j12" % SLF4J_VERSION,
  "org.schwering" % "irclib" % "1.10",
  "org.apache.samza" % "samza-api" % SAMZA_VERSION,

  "org.apache.samza" %% "samza-kv" % SAMZA_VERSION,
  "org.apache.samza" %% "samza-core" % SAMZA_VERSION,
  "org.apache.samza" %% "samza-yarn" % SAMZA_VERSION,
  "org.apache.samza" %% "samza-kv-rocksdb" % SAMZA_VERSION,
  "org.apache.samza" %% "samza-kafka" % SAMZA_VERSION,
  "org.apache.kafka" %% "kafka" % KAFKA_VERSION,

  "org.apache.samza" % "samza-log4j" % SAMZA_VERSION,
  "org.apache.samza" % "samza-shell" % SAMZA_VERSION,
  "org.apache.hadoop" % "hadoop-hdfs" % HADOOP_VERSION,

  "com.typesafe.akka" %% "akka-actor" % akkaVersion
)

packAutoSettings

val compileConfigTask = taskKey[Unit]("Compile le config")

compileConfigTask := {
  val s: TaskStreams = streams.value
  val packPath = pack.value
  for (f <- ((sourceDirectory in Compile).value / "config" * "*.properties").get) yield {
    val output = packPath / "config" / f.getName
    val template = IO.read(f)

    IO.write(
      output,
      template.
        replace("${target}", (target in Compile).value.getCanonicalPath).
        replace("${project.artifactId}", name.value).
        replace("${pom.version}", version.value)
    )
    s.log.info(s"Generated ${output} from ${f}")
  }
}

val getSamzaShell = taskKey[File]("Grabs samza-shell artifact")

getSamzaShell := {
  val samzaShellFile = (target in Compile).value / s"samza-shell-${SAMZA_SHELL_VERSION}-dist.tgz"
  if (! samzaShellFile.exists)
    url(s"http://repo1.maven.org/maven2/org/apache/samza/samza-shell/${SAMZA_SHELL_VERSION}/${samzaShellFile.name}") #> samzaShellFile !

  samzaShellFile
}

val extractSamzaShell = taskKey[Unit]("Extracts samza shell")

extractSamzaShell := {
  val log = streams.value.log
  val packPath = pack.value
  val shellPath = getSamzaShell.value
  val binPath = packPath / "bin"

  binPath.mkdir()

  log.info(s"Extracting ${shellPath} to ${binPath}")
  shellPath #> s"tar xz -C ${binPath}" ! log
}

val produceTgz = taskKey[File]("Produces a tar ball artifact")

produceTgz := {
  compileConfigTask.value
  extractSamzaShell.value
  val packPath = pack.value
  val output = (target in Compile).value / s"${name.value}-${version.value}-dist.tar.gz"
  // yarn.package.path=file://${target}/${project.artifactId}-${pom.version}-dist.tar.gz

  s"tar zc -C ${packPath.getCanonicalPath} ./" #> output !

  output
}
