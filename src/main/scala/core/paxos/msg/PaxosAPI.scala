package core.paxos.msg

import akka.actor.ActorRef
import core.paxos.PaxosSequenceNumbers.ProposalSequenceNumber

/**
 *  key : key to associate an object with
 *  set : the object itself (or a list of objects)
 *  seq : PAXOS sequence number
 */

case class Read(op:Object)

case class Write(key:String, set:Set[String], op:Object)

case class Propose(key:String, set:Set[String])

case class Consensus(key:String)

case class Prepare(key:String, tag:ProposalSequenceNumber)

case class PrepareOK(key:String, tag:ProposalSequenceNumber, set:Set[String])

//case class PrepareNOK(key:String)

case class Accept(key:String, tag:ProposalSequenceNumber, set:Set[String])

case class AcceptOK(key:String, tag:ProposalSequenceNumber, set:Set[String])