package core.paxos

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import scala.util.Random
import core._
import core.msg._
import core.paxos._
import core.paxos.msg._
import core.paxos.PaxosSequenceNumbers.ProposalSequenceNumber
import io.InputParameters

class PaxosNode(cfg:PaxosTopology) extends Actor {
  
  // objects stored in this replica as well as PAXOS logic
  private var instances : Map[String, PaxosState] = Map[String, PaxosState]()
  
  // used only by the multipaxos leader to skip rounds
  private var firstRound : Boolean = true
  
  
  def getSet(key : String) = {
    val set = instances get key match {
      case Some(instance) =>
        instance.storedValue
      case None =>
        val newState = new PaxosState(Set())
        instances += key -> newState
        newState.storedValue
    }
    set
  }
  
  def receive = {
    
    case marco() => sender ! polo()
    
    // Interface implementation ----------------------------------------------------------
    
    case insert(key, obj) =>
      val replicas = cfg.getReplicas(key)
      context.actorSelection(replicas(Random.nextInt(replicas.size))) forward Read(insert(key,obj))
      
    case remove(key, obj) =>
      val replicas = cfg.getReplicas(key)
      context.actorSelection(replicas(Random.nextInt(replicas.size))) forward Read(remove(key,obj))
    
    case isElement(key, obj) =>
      val replicas = cfg.getReplicas(key)
      context.actorSelection(replicas(Random.nextInt(replicas.size))) forward Read(isElement(key,obj))
    
    case list(key) =>
      val replicas = cfg.getReplicas(key)
      context.actorSelection(replicas(Random.nextInt(replicas.size))) forward Read(list(key))
      

    // Legacy interface implementation ----------------------------------------------------------
    case Read(op) => 
      
      op match {
        
        case insert(key, obj) =>
          val set = getSet(key)
          context.actorSelection(cfg.getLeader) forward Write(key, set + obj, insert(key,obj))
          
        case remove(key, obj) =>
          val set = getSet(key)
          context.actorSelection(cfg.getLeader) forward Write(key, set - obj, remove(key,obj))
          
        case isElement(key, obj) =>
          val set = getSet(key)
          sender ! isElementResponse(set contains obj)
          
        case list(key) =>
          val set = getSet(key)
          sender ! listResponse(set toList)
     }
      
    case Write(key, set, op) => 
      
     instances get key match {
       
       case Some(oldInstance) =>
          oldInstance.clients enqueue ((sender, op))
          instances += key -> oldInstance
       
       case None => 
          val newInstance = new PaxosState(Set())
          newInstance.clients enqueue ((sender, op))
          instances += key -> newInstance
     }
     
     self ! Propose(key, set)
     
     
   case Consensus(key) =>
     
     val bucket = instances get key
     
     if (bucket nonEmpty) {
       var instance = bucket get
       
       while (!instance.clients.isEmpty) {
         val (clt, op) = instance.clients.dequeue
         op match {
           case insert(key,obj) => 
             clt ! insertResponse()
           case remove(key,obj) => 
             clt ! removeResponse()
         }
       }
     }
     
    // Replica addition/removal implementation -------------------------------------------
      
    case hi() =>
      cfg.addNode(sender.path.name)
      
    case bye() =>
      cfg.removeNode(sender.path.name)
      
    case replicate(name) =>
      // sum +1 to avoid naming conflicts with older servers
      val youngerSibling = context.system.actorOf(Props(new PaxosNode(cfg)), name)
      cfg.addNode(youngerSibling.path.name)
      
      for (ref <- cfg.getAllNodes)
        context.actorSelection(ref).tell(hi(), youngerSibling)
        
      sender.tell(hi(), youngerSibling)
      
    case die() =>
      for (ref <- cfg.getAllNodes) 
        context.actorSelection(ref) ! bye()
        
      sender ! bye()
      self ! PoisonPill
     

    // MultiPAXOS implementation ----------------------------------------------------------
      
    // From: Outside To: Proposer
    case Propose(key, set) => 
      
      //println ("1. Proposer - Propose (k, v) = (" + k + ", " + v + "), self is " + self.path.name + ", sender is " + sender.path.name)  // DEBUG
      var instance = instances get key get
      
      // save originally proposed v for later use (see prepare_ok), 
      // choose higher n ever seen and send prepare to all
      instance.highestSeenN += 1
      instance.proposedN = (new ProposalSequenceNumber).generate(instance.highestSeenN)
      instance.proposedV = set
      instances += (key -> instance)
      
      if (firstRound) {
        firstRound = false
        for (ref <- cfg.getAllNodes) 
           context.actorSelection(ref) ! Prepare(key, instance.proposedN)
      } else {
        for (ref <- cfg.getAllNodes) 
           context.actorSelection(ref) ! Accept(key, instance.proposedN, set) 
      }
      
     
    // From: Proposer To: Acceptor
      
    case Prepare(key, tag) =>
      
      //println ("2. Acceptor - Prepare (k, n) = (" + k + ", (" + n.number + ", " + n.clock + ")), self is " + self.path.name + ", sender is " + sender.path.name)  // DEBUG
      
      var bucket = instances get key 
      
      if (bucket isEmpty)
        bucket = Some(new PaxosState(Set()))
        
      var instance = bucket get 
      
      if (tag.number <= instance.highestPrepareN.number) {
        // send nack back, lost consensus
        //sender ! PrepareNOK(seq, key) 
        
      } else if (tag.clock > instance.highestPrepareN.clock) {
        
        // update internal np and reply prepare_ok to proposer
        instance.highestPrepareN set tag
        instances += (key -> instance)
        sender ! PrepareOK(key, instance.highestAcceptN, instance.highestAcceptV)
      }

      
    // From: Acceptor To: Proposer
    case PrepareOK(key, tag, set) =>
      
       //println ("3. Proposer - PrepareOK (k, n, v) = (" + k + ", (" + n.number + ", " + n.clock + "), " + v + "), self is " + self.path.name + ", sender is " + sender.path.name)  // DEBUG
      
      var instance = instances get key get
      
      if (instance.highestSeenN < tag.number) instance.highestSeenN = tag.number
      
      // increment prepare_ok request counter, save received na and va, and check if we've received majority
      instance.PrepareOkSet += sender
      instance.highestAcceptedProposals ::= (tag, set) 
      
      if (instance.PrepareOkSet.size > Math.floor(cfg.getAllNodes.size/2f)) {
        
        // find the va from the highest na, or use the originally proposed pv
        val hpair = instance.highestAcceptedProposals.highestTag
        
        var hva = if (hpair._2.isEmpty) 
                    instance.proposedV 
                  else hpair._2 
      	
      	// broadcast accept to all with the highest va
        for (ref <- cfg.getAllNodes) context.actorSelection(ref) ! Accept(key, instance.proposedN, hva)
      }
      
      instances += (key -> instance)
        
    // From: Proposer To: Acceptor
    case Accept(key, tag, set) => 
      
      //println ("4. Acceptor - Accept (k, n, v) = (" + key + ", (" + tag.number + ", " + tag.clock + "), " + set + "), self is " + self.path.name + ", sender is " + sender.path.name)  // DEBUG
      
      var bucket = instances get key 
      
      if (bucket isEmpty)
        bucket = Some(new PaxosState(Set()))
                
      var instance = bucket get
      
      if (tag.number < instance.highestPrepareN.number) {
        // do nothing
      } else if (tag.clock >= instance.highestPrepareN.clock) {
        // update na and va and reply accept_ok to proposer
        instance.highestAcceptN set tag  
        instance.highestAcceptV = set
        
        instances += (key -> instance)
        // finally, broadcast accepted to leaners
        for (ref <- cfg.getAllNodes) context.actorSelection(ref) ! AcceptOK(key, instance.highestAcceptN, instance.highestAcceptV)
      }
      // else ignore (do not accept)
      
     
    // From: Acceptor To: Learner
    case AcceptOK(key, tag, set) =>
      
      //println ("6. Learner - Accepted (k, n, v) = (" + key + ", (" + tag.number + ", " + tag.clock + "), " + set + "), self is " + self.path.name + ", sender is " + sender.path.name)  // DEBUG
      val bucket = instances get key
      
      if (bucket nonEmpty) {
        
        var instance = bucket get
        
        if (tag.number < instance.highestAcceptN.number) {
          // do nothing
        } else {
          if (tag.number > instance.highestAcceptN.number || tag.clock > instance.highestAcceptN.clock) {
            instance.highestAcceptN set tag
            instance.highestAcceptV = set
            instance.AcceptOkSet = Set()
          }
          
          instance.AcceptOkSet += sender
          
          if (instance.AcceptOkSet.size > Math.floor(cfg.getAllNodes.size/2f)) {
            
            if (cfg.getReplicas(key) contains self.path.name) {
                instance.storedValue = instance.highestAcceptV
            }
            //println ("6. Learner - Accepted (k, n, v) = (" + key + ", (" + tag.number + ", " + tag.clock + "), " + set + "), self is " + self.path.name + ", sender is " + sender.path.name)  // DEBUG
          
            for (ref <- cfg.getAllNodes) context.actorSelection(ref) ! Consensus(key)
          }
          
          instances += key -> instance
        }
      }
  }
}