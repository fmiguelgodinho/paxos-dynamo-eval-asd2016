package core.dynamo

import scala.collection.mutable.Buffer
import akka.actor.{Actor, ActorRef}
import core._
import core.msg._
import core.dynamo.msg._
import core.dynamo.crdt._

class DynamoNode(dht:DynamoDHT, cfg:DynamoConfiguration) extends Actor {
  
  // objects stored in this replica
  private var storage = Map[String, DynamoStoredValue]()
  
  // register of operations and system state
  // this is essential to keep track of client requests as well
  // as to maintain execution logic when processing the request
  private var register = Vector[DynamoState]()
  
  
  def receive = {
    
    case marco() => sender ! polo()

    // Interface implementation ----------------------------------------------------------
    
    case insert(key, obj) =>
      val state = new DynamoState(sender, insert(key,obj))
      state.readQuorumSize = cfg.W
      state.writeQuorumSize = cfg.W
      register = register :+ state
      val seq = register.size-1
    
      self ! get(seq, key)
      
    case remove(key, obj) =>
      val state = new DynamoState(sender, remove(key, obj))
      state.readQuorumSize = cfg.W
      state.writeQuorumSize = cfg.W
      register = register :+ state
      val seq = register.size-1
      
      self ! get(seq, key)
    
    case isElement(key, obj) =>
      val state = new DynamoState(sender, isElement(key, obj))
      state.readQuorumSize = cfg.R
      register = register :+ state
      val seq = register.size-1
      
      self ! get(seq, key)
      
    case list(key)   =>
      val state = new DynamoState(sender, list(key))
      state.readQuorumSize = cfg.R
      register = register :+ state
      val seq = register.size-1
      
      self ! get(seq, key)
      
    case getResponse(seq, lst, ctx) =>
      
       // reconcile versions obtained from dynamo
       var reconciled = if (cfg.getCRDTType equals CRDTType.ORSET) 
         new DynamoORSet 
       else 
         new Dynamo2PSet
         
       for (version <- lst) reconciled = reconciled.merge(version)
       
       val state = register(seq)
       
       state.operation match {
          
          case insert(key, obj) =>
            self ! put(seq, key, reconciled.add(obj), ctx)
            
          case remove(key, obj) =>
            var crdt = new Dynamo2PSet
            self ! put(seq, key, reconciled.remove(obj), ctx)
            
          case isElement(key, obj) =>
            state.client ! isElementResponse(reconciled lookup obj)
            
          case list(key) =>
            state.client ! listResponse(reconciled.get toList)
        } 
       
       
    case putResponse(seq) =>
      
      val state = register(seq)
      state.operation match {
          
          case insert(key, obj) =>
            state.client ! insertResponse()
            
          case remove(key, obj) =>
            state.client ! removeResponse()
      }

    // Dynamo implementation ----------------------------------------------------------
      
    // GET
    case get(seq, key) =>

      val rpl = dht.get(key)
      
      register(seq) replicas = rpl
      
      for (ref <- rpl) {
        context.actorSelection(ref) ! inquire(seq, key, rpl)
      }

    // get propagation
    case inquire(seq, key, rpl) =>
      
      val value = storage get key match {
        
          case bucket if (bucket isEmpty) =>
            
            val crdt = if (cfg.getCRDTType equals CRDTType.ORSET) 
               new DynamoORSet 
            else 
               new Dynamo2PSet
            
            val empty = DynamoStoredValue(crdt, new DynamoContext(rpl))
            storage += (key -> empty)
            empty
            
          case bucket =>
            bucket.get
      }
      
      sender ! inquireResponse(seq, value.obj, value.ctx)
      
    // get propagation response
    case inquireResponse(seq, obj, ctx) =>
      
      val state = register(seq)
      state.readQuorum ::= (sender, DynamoStoredValue(obj, ctx))
      
      if (state.readQuorum.size == state.readQuorumSize) {

        var versionSet = List[DynamoCRDT]()
        var mergedCtx = new DynamoContext(state replicas)
        
        for ((node, stored) <- state.readQuorum) {
          // enqueue lists of values if they're diff
          
          stored.ctx.compareClock(mergedCtx) match {
            
            case ClockComparison.Bigger =>
              // a newer version was found, replace object and context
              versionSet = List(stored.obj)
              mergedCtx = stored.ctx
              
            case ClockComparison.Smaller =>
              // context stays the same (mergedCtx = mergedCtx)
              // the newer version of the object is already on the list
              
            case ClockComparison.Incomparable =>
              // concurrent updates happened!
              // enqueue the conflicting version in the list
              versionSet ::= stored.obj
              mergedCtx = mergedCtx.mergeClock(stored.ctx)
          }    
        }
        
        self ! getResponse(seq, versionSet, mergedCtx)
      }
      
      
    // PUT
    case put(seq, key, obj, ctx) =>

      val state = register(seq)
      
      for ((node, stored) <- state.readQuorum) {
        context.actorSelection(node.path) ! announce(seq, key, obj, ctx)
      }
      
      
    case announce(seq, key, obj, ctx) =>
      
      storage += (key -> DynamoStoredValue(obj, ctx.incrementClock(self.path.name)))
      
      sender ! announceResponse(seq)

      
    case announceResponse(seq) =>
      
      val state = register(seq)
      state.writeQuorum ::= sender
      
      if (state.writeQuorum.size == state.writeQuorumSize) {
         self ! putResponse(seq) 
      }
      
  }
}