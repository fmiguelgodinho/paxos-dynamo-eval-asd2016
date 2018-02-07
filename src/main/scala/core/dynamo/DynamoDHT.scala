package core.dynamo

import scala.collection.immutable.HashMap
import java.security.MessageDigest
import java.util.{TreeMap, NavigableMap}

/**
 * Dynamo's DHT, used to find which K replicas are responsible 
 * for a single key by following the consistent hashing pattern.
 *  nodeReferences 	:		dynamo nodes in the system
 *  numReplicas			:		the number of replicas responsible for each key
 */
class DynamoDHT(nodesReferences:List[String], numReplicas:Int) {
  
  val circularMap: NavigableMap[String, String] = new TreeMap[String, String]()
  
  // insert all dynamo nodes in their corresponding positions with an md5 hash
  for (ref <- nodesReferences) {
    circularMap put(##(ref), ref)
  }
  
  // md5 hashing function
  def ##(key: String) = {
    MessageDigest.getInstance("MD5").digest(key.getBytes).map("%02x".format(_)).mkString
  }
  
  def get(key: String) = {
    
    var hash = ##(key)
    var refs = List[String]()
    
    if (!(circularMap isEmpty)) {
     
      // obtain a portion of the map from the hash onwards and it's iterator
      val portion = circularMap.tailMap(hash, true)
      var it = portion.navigableKeySet.iterator
      
      // if the iterator has nothing for us, go back to circular map head
      if (!it.hasNext) {
          it = circularMap.navigableKeySet.iterator
      }
      
      // keep filling refs in clockwise fashion until it's full
      while (it.hasNext && refs.length < numReplicas) {
          refs ::= circularMap.get(it.next)
          if (!it.hasNext) {
              it = circularMap.navigableKeySet.iterator
          }
      }
      
    }
    
    refs
  } 
  
}
