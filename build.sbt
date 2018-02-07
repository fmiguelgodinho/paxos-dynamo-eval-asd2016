name := "paxos-dynamo-repo"
 
version := "1.0"
 
scalaVersion := "2.11.8"
 
libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.4.10"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"