name := "DarkyenusBuild"

version := "0.1-SNAPSHOT"

resolvers += "spigot-repo" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += ("org.spigotmc" % "spigot-api" % "1.10.2-R0.1-SNAPSHOT" % "provided")

libraryDependencies += "net.sf.trove4j" % "trove4j" % "3.0.3"

autoScalaLibrary := false

crossPaths := false
