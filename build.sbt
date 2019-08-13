name := "ticketing"
version := "0.1"
scalaVersion := "2.12.9"
val akkaVersion = "2.5.23"
val commonsVersion = "1.5.2"

resolvers += "Artifactory Realm" at "https://artifactory.linkedstore.com/artifactory/libs-release/"
credentials += Credentials("Artifactory Realm", "artifactory.linkedstore.com", "reader", "X+7Ahj&#F;{?%3LU")

libraryDependencies += "com.tiendanube" %% "commons-scala" % commonsVersion
libraryDependencies += "com.tiendanube" %% "commons-scala-testing" % commonsVersion % Test

libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-discovery" % akkaVersion

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.1" % Provided

libraryDependencies += "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "2.2.0" % Provided
libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.16.1" % Provided

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test

