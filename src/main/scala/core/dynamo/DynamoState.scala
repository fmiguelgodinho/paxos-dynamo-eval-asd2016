package core.dynamo

import akka.actor.ActorRef

class DynamoState(clt:ActorRef, op:Object) {
  
  var client : ActorRef = clt
  var operation : Object = op
  var replicas : List[String] = _
  
  var readQuorumSize : Int = _
  var writeQuorumSize : Int = _
  
  var readQuorum : List[(ActorRef, DynamoStoredValue)] = List[(ActorRef, DynamoStoredValue)]()
  var writeQuorum : List[ActorRef] = List[ActorRef]()
  
}