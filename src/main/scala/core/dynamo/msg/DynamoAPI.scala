package core.dynamo.msg

import akka.actor.ActorRef
import core.dynamo._
import core.dynamo.crdt._

/* GET ---------------------------------------------------------------- */

/** 
 * Dynamo GET request
 * seq : the sequence number that uniquely identifies a request
 * key : key to associate an object with
 */
case class get(seq:Int, key:String)

/** 
 * Propagates GET request internally to a set of replicas
 * seq : the sequence number that uniquely identifies a request
 * key : key to associate an object with
 * rpl : replicas that are responsible for the key
 */
case class inquire(seq:Int, key:String, rpl:List[String])                                         

/** 
 * Response from replicas to coordinator node after inquire
 * seq : the sequence number that uniquely identifies a request
 * obj : object associated to the requested key
 * ctx : context of the object
 */
case class inquireResponse(seq:Int, obj:DynamoCRDT, ctx:DynamoContext)

/** 
 * Dynamo GET response
 * seq : the sequence number that uniquely identifies a request
 * lst : list of diff versions (of the stored object) acquired from replicas
 * ctx : context of the list (merged from object versions)
 */
case class getResponse(seq:Int, lst:List[DynamoCRDT], ctx:DynamoContext)    



/* PUT ---------------------------------------------------------------- */

/** 
 * Dynamo PUT request
 * seq : the sequence number that uniquely identifies a request
 * key : key to associate an object with
 * obj : the stored object
 * ctx : the context associated to the object
 */
case class put(seq:Int, key:String, obj:DynamoCRDT, ctx:DynamoContext)

/** 
 * Propagates PUT request internally to a set of replicas
 * seq : the sequence number that uniquely identifies a request
 * key : key to associate an object with
 * obj : the stored object
 * ctx : context of the object
 */
case class announce(seq:Int, key:String, obj:DynamoCRDT, ctx:DynamoContext)

/** 
 * Response from replicas to coordinator node after announce
 * seq : the sequence number that uniquely identifies a request
 */
case class announceResponse(seq:Int)

/** 
 * Dynamo PUT response
 * seq : the sequence number that uniquely identifies a request
 */
case class putResponse(seq:Int)