package core.dynamo

import core.dynamo.crdt._

/**
 * Dynamo stored value with some internal logic (context)
 *  obj : stored value, a CRDT
 *  ctx : context associated to the set
 */

case class DynamoStoredValue(obj : DynamoCRDT, ctx : DynamoContext)
