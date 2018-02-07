package io

object InputParameters {
  
  // General
  val REPO_SERVER_PREFIX      = "server-node-"
  val REPO_CLIENT_PREFIX      = "client-"
  val REPO_MONITOR_AGENT      = "monitor-agent"
  val CLIENT_POOL_SIZE        = 3                 // default = 3
  val SERVER_POOL_SIZE        = 7                 // default = 7
  
  // Operation execution
  val NUMBER_OF_REPLICAS      = 3                 // default = 3, has to be < SERVER_POOL_SIZE
  val NUMBER_OF_OPS           = 10000             // default = 10000
  val NUMBER_OF_KEYS          = 100               // default = 100
  val RANGE_OF_VALUES         = 0 to 100  
  val PROPORTION_INSERT       = 0.4f              // default = 0.4f
  val PROPORTION_REMOVE       = 0.1f              // default = 0.1f
  val PROPORTION_ISELEMENT    = 0.4f              // default = 0.4f
  val PROPORTION_LIST         = 0.1f              // default = 0.1f
  
  // MultiPaxos-specific
  val PAXOS_REPLICA_ADDITIONS = 1
  val PAXOS_REPLICA_REMOVALS  = 1
  
  // Dynamo-specific
  val DYNAMO_R_QUORUM         = 2                 // has to be < NUMBER_OF_REPLICAS - DYNAMO_W_QUORUM
  val DYNAMO_W_QUORUM         = 1                 // has to be < NUMBER_OF_REPLICAS - DYNAMO_R_QUORUM
  val DYNAMO_CRDT_TYPE        = "ORSet"           // [ORSet,TwoPSet]
}
