// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.10")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("com.localytics" % "sbt-dynamodb" % "1.5.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.6")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.7")

libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts (Artifact("jdeb", "jar", "jar"))
