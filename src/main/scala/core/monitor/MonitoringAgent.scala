package core.monitor

import akka.actor.{Actor, ActorRef, PoisonPill}
import scala.util.Random
import scala.collection.mutable.PriorityQueue
import core.msg._
import core.monitor.msg._

class MonitoringAgent(cfg:MonitoringAgentConfiguration) extends Actor {
  
  // client and respective operations monitored sessions
  var sessions = Map[ActorRef, Array[MonitoringAgentState]]()
  // total operations timed so far, replicas added and removed to the server system
  var operationsTimed = 0
  
  // list of active server nodes being monitored
  var activeNodes = cfg.serverNodes
  // replica addition and removal enabler
  var interferenceFlag = false
  // replica addition and removal position markers
  var addReplicaMarkers, removeReplicaMarkers = PriorityQueue.empty[Int](implicitly[Ordering[Int]] reverse)
  
  
  override def preStart = 
    defineReplicaMarkers
    
  def receive = {
    
    case startTimer(seq) =>
      
        val states = sessions get sender match {
          case bucket if (bucket isEmpty) =>
            Array.ofDim[MonitoringAgentState](cfg.numOpsPerClient)
          case bucket =>
            bucket.get
        }
        
        states(seq) = new MonitoringAgentState(System.nanoTime)
        sessions += (sender -> states) 
   
    case stopTimer(seq) =>
      
        val states = sessions get sender match {
          case bucket if (bucket isEmpty) =>
            throw new Exception("Asked monitor agent to stop timing an inexistent operation!")
          case bucket =>
            bucket.get
        }
        
        states(seq).endTime = System.nanoTime
  	    states(seq).runTime = states(seq).endTime - states(seq).iniTime
        sessions += (sender -> states) 
    	  
  	    operationsTimed += 1
  	    
  	    // add or remove replicas if needed
  	    if (interferenceFlag) {
  	      
    	    if (addReplicaMarkers.nonEmpty && operationsTimed == addReplicaMarkers.head) {
            addReplicaMarkers.dequeue
            addReplica
          }
    	    
          if (removeReplicaMarkers.nonEmpty && operationsTimed == removeReplicaMarkers.head) {
            removeReplicaMarkers.dequeue
            removeReplica
          }
          
  	    }
        
        // check if we've finished
  	    if (operationsTimed == cfg.numClients * cfg.numOpsPerClient) {
           statistics
           reset
           println("\nAnother run? [PAXOS/Dynamo]")
        }
  	    
    case interfere(flg) =>
      interferenceFlag = flg
      
    case hi() =>
      activeNodes ::= "/user/" + sender.path.name
      println("\nReplica " + sender.path.name + " added to system.")
      
    case bye() =>
      activeNodes = activeNodes filter (_ != "/user/" + sender.path.name)
      println("\nReplica " + sender.path.name + " removed from system.")
        
  }
  
  def reset = {
      sessions = sessions.empty
      operationsTimed = 0
      // eliminates server actors so we can start a new instance
      for (path <- activeNodes) 
        context.actorSelection(path) ! PoisonPill
      // resets active nodes to config default
      activeNodes = cfg.serverNodes
      defineReplicaMarkers
  }
  
  def defineReplicaMarkers = {
    
    for (i <- 1 to cfg.numReplicaAdditions)
        addReplicaMarkers enqueue Random.nextInt(cfg.numClients * cfg.numOpsPerClient)
    
    for (i <- 1 to cfg.numReplicaRemovals)
        removeReplicaMarkers enqueue Random.nextInt(cfg.numClients * cfg.numOpsPerClient)
  }
  
  def addReplica = {
      // choose random server
      val olderBrother = activeNodes(Random.nextInt(activeNodes size))
      // ask it to create a sibling (add an r to the name to avoid conflicts with older servers)
      val name = cfg.serverPrefix + "r" + (addReplicaMarkers.size + 1)
      context.actorSelection(olderBrother) ! replicate(name)
  }
  
  def removeReplica = {
      // choose random server to remove
      val victim = activeNodes(Random.nextInt(activeNodes size))
      // kill it
      context.actorSelection(victim) ! die()
  }
  
  def statistics = {

     var fullRunStartingTime = Double.PositiveInfinity
     var fullRunFinishTime = Double.NegativeInfinity
     var shortestOperation = Double.PositiveInfinity
     var longestOperation = Double.NegativeInfinity
     var sumOfOperationTimes = 0.0

     for (states <- sessions.values) {

       for (state <- states) {

         if (state.iniTime < fullRunStartingTime) {
           fullRunStartingTime = state.iniTime
         }
  
         if (state.endTime > fullRunFinishTime) {
           fullRunFinishTime = state.endTime
         }
  
         if (state.runTime < shortestOperation) {
           shortestOperation = state.runTime
         }
  
         if (state.runTime > longestOperation) {
           longestOperation = state.runTime
         }
  
         sumOfOperationTimes += state.runTime
       }
     }

     val totalRuntime = fullRunFinishTime - fullRunStartingTime
     val averageOperation = sumOfOperationTimes/(cfg.numClients * cfg.numOpsPerClient)
     val opsPerSecond = cfg.numOpsPerClient/nanoToSeconds(totalRuntime)

     println("\nRun finished! Measured the following statistics:")
     println("\ttotal runtime = "    + nanoToSeconds(totalRuntime) + " s")
     println("\t#ops/sec = "         + opsPerSecond + " flops")
     println("\tavg(op runtime) = "   + nanoToMillis(averageOperation) + " ms")
     println("\tmin(op runtime) = "   + nanoToMillis(shortestOperation) + " ms")
     println("\tmax(op runtime) = "   + nanoToMillis(longestOperation) + " ms")

  }
  
  def nanoToMillis = (nano:Double) => nano/Math.pow(10,6)
  def nanoToSeconds = (nano:Double) => nano/Math.pow(10,9)
}