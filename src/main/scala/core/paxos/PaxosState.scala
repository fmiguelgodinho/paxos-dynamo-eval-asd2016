package core.paxos

import akka.actor.ActorRef
import scala.collection.mutable.Queue
import core.paxos.PaxosSequenceNumbers.ProposalSequenceNumber

class PaxosState(set : Set[String]) {
  
  /* Acceptor state: np (highest prepare), na, va (highest accept) */
  var highestPrepareN : ProposalSequenceNumber = new ProposalSequenceNumber
  var highestAcceptN : ProposalSequenceNumber = new ProposalSequenceNumber
  var highestAcceptV : Set[String] = Set[String]()
  
  /* Proposer state: pn, pv (original value), hn (highest n seen so far) */
  var proposedN : ProposalSequenceNumber = new ProposalSequenceNumber
  var proposedV : Set[String] = Set[String]()
  var highestSeenN : Int = 0
  
  /* quorum collectors */
  var AcceptOkSet : Set[ActorRef] = Set[ActorRef]()
  var PrepareOkSet : Set[ActorRef] = Set[ActorRef]()
  
  /* control list of highest values for prepare_ok */
  var highestAcceptedProposals : List[(ProposalSequenceNumber, Set[String])] = List[(ProposalSequenceNumber, Set[String])]()
  
  /* learner decision */
  var storedValue : Set[String] = set
  var clients : Queue[(ActorRef, Object)] = Queue[(ActorRef, Object)]()
  
}