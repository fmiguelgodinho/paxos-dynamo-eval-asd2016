package core.dynamo

/**
 *  Dynamo configuration object.
 *   R    	: minimum quorum size for read ops
 *   W		  : minimum quorum size for write ops
 *   CRDT	: type of crdt set to use
 */

object CRDTType extends Enumeration {
  type CRDTType = Value
  val ORSET, TWOPSET = Value
}

case class DynamoConfiguration(R:Int, W:Int, CRDT:String) {
  
  def getCRDTType = {
    CRDTType withName CRDT.toUpperCase
  }
  
}