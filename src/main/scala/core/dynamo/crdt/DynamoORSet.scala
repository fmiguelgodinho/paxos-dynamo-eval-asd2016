package core.dynamo.crdt

import java.util.UUID

/**
 *  State-based OR-Set CDRT
 *  
 *  Follows the pseudo-code below.
 *  
 *  add	(a)	->	add(	(a,uid a ))
 *		– S.A	:=	S	U	{(a,uid a )}
 *  remove	(a)	->	remove(	old uids )	:	old uids ={u:(a,u)	 ∈ S}
 *		– S.A	:=	S.A	\ {(a,u):	u	 ∈ old uids }	;	S.R	=	S.R	U	old uids
 * 	lookup()	:	returns	{a:(a,_)	 ∈ S.A}
 *  merge (	S1,	S2)	=	S3:
 * 		– S3.R	=	S1.R	U	S2.R
 * 		– S3.A	=	(S1.A	U	S2.A)	\ {(a,uid}:	uid ∈ S3.R}
 */
class DynamoORSet extends DynamoCRDT {
  
  var A : Set[(String, UUID)] = Set[(String, UUID)]()
  var R : Set[UUID] = Set[UUID]()
  
  def lookup(elem : String) =
    A exists (pair => pair._1 equals elem)
  
  def add(elem : String) = {
    val tagged = (elem, UUID.randomUUID)
    A = A + tagged
    this
  }
  
  def remove(elem : String) = {
    val oldUIDs = A collect {
        case pair if pair._1 equals elem => pair._2
    }
    A = A filter (pair => !(oldUIDs contains pair._2))
    R = R ++ oldUIDs
    this
  }
  
  @deprecated
  def compare(that : DynamoCRDT) = {
    // not needed
    false
  }
  
  def merge(that : DynamoCRDT) = {
    val theirs = that.asInstanceOf[DynamoORSet];
    R = R ++ theirs.R
    A = (A ++ theirs.A) filter (p => !(R contains p._2))
    this
  }
  
  def get() = A collect {
      case pair if (!(R contains pair._2)) => pair._1
  }
  
}