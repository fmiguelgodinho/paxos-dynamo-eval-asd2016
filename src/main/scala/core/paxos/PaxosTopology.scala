package core.paxos

import scala.util.Random

class PaxosTopology(siblings:List[String], numberOfReplicas:Int) {
  
  val electionSeed = Random.nextInt // seed that allows repeatable random indexes for leader election
  
  var _siblings           : Vector[String] = siblings toVector
  var _numberOfReplicas   : Int            = numberOfReplicas
  var _leader             : Option[String] = None
  
  def getAllNodes = 
    _siblings
  
  def getNumberOfReplicas = 
    _numberOfReplicas
  
  def getLeader = {
    
    if (_leader isEmpty)
      electLeader

    _leader.get
  }
  
  def getReplicas(seed : String) = {
    // set index to find first replica
    val index = Math.abs(seed ##) % _siblings.size
    // iterate to find the next ones
    var replicas = List[String]()
    for (i <- index to index + _numberOfReplicas-1)
      replicas ::= _siblings(index % _siblings.size)
    // return
    replicas
  }
  
  def addNode(name:String) = {
    val fullName = "/user/" + name 
    
    if (!(_siblings contains fullName))
      _siblings :+ fullName
      
    if (_numberOfReplicas < numberOfReplicas && _numberOfReplicas+1 != _siblings.size)
      _numberOfReplicas += 1
  }
  
  def removeNode(name:String) = {
    val fullName = "/user/" + name
    _siblings = _siblings filter (_ != fullName)
    
    if (_numberOfReplicas + 1 == _siblings.size)
      _numberOfReplicas -= 1
      
    if (_leader.get equals fullName)
      electLeader
  } 
  
  def electLeader = {
     // same seed will generate the same leader
     val seeded = new Random(electionSeed)
     // take leader out of all nodes
     _leader = Some(_siblings(seeded nextInt (_siblings size)))
  }
    
  
}