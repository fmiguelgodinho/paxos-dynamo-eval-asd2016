package core.monitor


/**
 * Monitor agent configuration object.
 *  serverNodes 				: list of server paths to monitor
 *  serverPrefix				: monitored servers prefix
 *  numClients    			: total number of clients in the system
 *  numOpsPerClient			: number of operations performed by each client
 *  numReplicaAdditions	: number of replicas to add during execution
 *  numOpsPerClient			: number of replicas to remove during execution
 */

case class MonitoringAgentConfiguration(
    serverNodes         : List[String],
    serverPrefix        : String,
    numClients          : Int, 
    numOpsPerClient     : Int, 
    numReplicaAdditions : Int = 0, 
    numReplicaRemovals  : Int = 0
)