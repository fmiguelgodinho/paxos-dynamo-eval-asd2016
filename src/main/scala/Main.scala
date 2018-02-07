import akka.actor.{ActorSystem, ActorRef, Actor, Props}
import scala.util.Random
import scala.io.StdIn
import io._
import core.msg._
import core._
import core.monitor._
import core.monitor.msg._
import core.dynamo._
import core.paxos._

object Main {
  
  val TERMINATE_CMD = "EXIT"
  
  object RepositoryVersion extends Enumeration {
    type RepositoryVersion = Value
    val PAXOS, DYNAMO = Value
  }

  def main(args: Array[String]): Unit = {
      
      // Create the actor system that will spawn our actors
			val actorSystem = ActorSystem("akka-repository")
    
      println("Key/Value repository started. To stop the repository type EXIT.\n")
	  	println("Reading input parameters...")
      println("\t#clients = "         + InputParameters.CLIENT_POOL_SIZE)
			println("\t#servers = "         + InputParameters.SERVER_POOL_SIZE)
	  	println("\t#replications = "    + InputParameters.NUMBER_OF_REPLICAS)
	  	println("\t#operations = "      + InputParameters.NUMBER_OF_OPS + " over #distinct keys = " + InputParameters.NUMBER_OF_KEYS + "\n")
	  	
	    println("Generating instruction sets with the following proportions:")
	  	println("\tp(insert) = "       + InputParameters.PROPORTION_INSERT*100 + "%")
	  	println("\tp(remove) = "       + InputParameters.PROPORTION_REMOVE*100 + "%")
	  	println("\tp(isElement) = "    + InputParameters.PROPORTION_ISELEMENT*100 + "%")
	  	println("\tp(list) = "         + InputParameters.PROPORTION_LIST*100 + "%\n")
	  	
	  	
	  	// Generate our constant client and server names, it'll be useful for later
			var serverRefs = List[String]()
      for(i <- 1 to InputParameters.SERVER_POOL_SIZE)
          serverRefs ::= "/user/" + InputParameters.REPO_SERVER_PREFIX + i
	  	
	  	// Create client pool and generate instruction sets
	  	val clientCfg = ClientConfiguration(
	  	    serverRefs,
	  	    "/user/" + InputParameters.REPO_MONITOR_AGENT,
	  	    InputParameters.REPO_SERVER_PREFIX
	  	)
			var clientPool = List[(ActorRef, InstructionSet)]()
			for(i <- 1 to InputParameters.CLIENT_POOL_SIZE) {
					clientPool ::= (
					    actorSystem.actorOf(Props(new Client(clientCfg)), InputParameters.REPO_CLIENT_PREFIX + i),
					    new InstructionSet().generate
					)
			}
			
		  // Create monitor agent to supervise and manage servers
			val monitorCfg = MonitoringAgentConfiguration(
			    serverRefs, 
			    InputParameters.REPO_SERVER_PREFIX,
			    InputParameters.CLIENT_POOL_SIZE, 
			    InputParameters.NUMBER_OF_OPS,
			    InputParameters.PAXOS_REPLICA_ADDITIONS,
			    InputParameters.PAXOS_REPLICA_REMOVALS
			)
			val monitorAgent = actorSystem.actorOf(Props(new MonitoringAgent(monitorCfg)), InputParameters.REPO_MONITOR_AGENT)
			
			// ask for user input on which algorithm to run
      println("Which version do you want to execute? [PAXOS/Dynamo]")
	  	
			while (true) {
			  
			  // this will keep cycling in order to accept further 
			  // algorithm runs with the same instruction sets
			
        var opt : Option[RepositoryVersion.Value] = None
        while (opt isEmpty) {
          try {
            val input = StdIn.readLine.trim.toUpperCase
            
            if (input equals TERMINATE_CMD) {
              println("Stopping...")
              actorSystem.terminate
              return
            }
    	  	  opt = Some(RepositoryVersion withName input)
          } catch {
            case e : NoSuchElementException => println ("Unknown option. Please type either one of the following: [PAXOS/Dynamo]")
          }
        }
        
        // Create either PAXOS or Dynamo server nodes
        opt match {

            case Some(RepositoryVersion.PAXOS) => 
              
                println("\nPaxos-specific parameters:")
                println("\timplementation = Multi-Paxos")
                println("\t#replicas to add = "         + InputParameters.PAXOS_REPLICA_ADDITIONS)
			          println("\t#replicas to remove = "      + InputParameters.PAXOS_REPLICA_REMOVALS)
              
                // tell monitor to interfere with replicas pool size
                monitorAgent ! interfere(true)
                // setup topology
                val cfg = new PaxosTopology(
                    serverRefs, 
                    InputParameters.NUMBER_OF_REPLICAS
                )
                // create paxos nodes
                for(i <- 1 to InputParameters.SERVER_POOL_SIZE)
                    actorSystem.actorOf(Props(new PaxosNode(cfg)), InputParameters.REPO_SERVER_PREFIX + i)
                
            case Some(RepositoryVersion.DYNAMO) => 
              
                println("\nDynamo-specific parameters:")
                println("\tsize(read quorum) = "        + InputParameters.DYNAMO_R_QUORUM)
			          println("\tsize(write quorum) = "       + InputParameters.DYNAMO_W_QUORUM)
			          println("\tcrdt-set = "                 + InputParameters.DYNAMO_CRDT_TYPE)
              
                // tell monitor NOT to interfere with replicas pool size
                monitorAgent ! interfere(false)
                // setup dht and configuration
                val dht = new DynamoDHT(
                    serverRefs, 
                    InputParameters.NUMBER_OF_REPLICAS
                )
                val cfg = DynamoConfiguration(
                    InputParameters.DYNAMO_R_QUORUM, 
                    InputParameters.DYNAMO_W_QUORUM,
                    InputParameters.DYNAMO_CRDT_TYPE
                )
                // create dynamo nodes 
                for(i <- 1 to InputParameters.SERVER_POOL_SIZE)
                    actorSystem.actorOf(Props(new DynamoNode(dht, cfg)), InputParameters.REPO_SERVER_PREFIX + i)
          
        }
        
        println("\nRunning...")

    		// Tell each client to execute the respective instruction set
  			for ((client, instructionSet) <- clientPool) {
  			    client ! digest(instructionSet.rewind)
  			}
  			
			}
			

  }
}