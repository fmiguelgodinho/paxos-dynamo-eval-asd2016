package core.dynamo.crdt

/**
 *  State-based 2P-Set CDRT
 *  
 *  Follows the pseudo-code below.
 *  
 *  payload set A, set R									-- A : added; R : removed
 *		initial ∅, ∅
 *	query lookup (element e) : boolean b
 *		let b = (e ∈ A ∧ e � R)
 * 	update add (element e)
 * 		A := A ∪ {e}
 *	update remove (element e)
 * 		pre lookup(e)
 *		R := R ∪ {e}
 * 	compare (S , T ) : boolean b
 * 		let b = (S .A ⊆ T.A ∧ S .R ⊆ T.R)
 * 	merge (S , T ) : payload U
 *		let U.A = S .A ∪ T.A
 * 		let U.R = S .R ∪ T.R
 */
class Dynamo2PSet extends DynamoCRDT {
  
  var A : Set[String] = Set[String]()
  var R : Set[String] = Set[String]()
  
  def lookup(elem : String) =
    (A contains elem) && !(R contains elem)
  
  def add(elem : String) = {
    A = A + elem
    this
  }
  
  def remove(elem : String) = {
    if (lookup(elem)) R = R + elem
    this
  }
  
  @deprecated
  def compare(that : DynamoCRDT) = {
    // not needed
    val theirs = that.asInstanceOf[Dynamo2PSet]
    (A subsetOf theirs.A) && (R subsetOf theirs.R)
  }
  
  def merge(that : DynamoCRDT) = {
    val theirs = that.asInstanceOf[Dynamo2PSet]
    A = theirs.A ++ A
    R = theirs.R ++ R
    this
  }
  
  def get() = A -- R
  
}