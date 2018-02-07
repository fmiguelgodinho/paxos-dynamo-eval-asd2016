package core.dynamo.crdt

/**
 * CRDT interface 
 */

trait DynamoCRDT {
  
    def lookup(elem : String)        : Boolean
    def add(elem : String)           : DynamoCRDT
    def remove(elem : String)        : DynamoCRDT
    def compare(theirs : DynamoCRDT) : Boolean
    def merge(theirs : DynamoCRDT)   : DynamoCRDT
    def get()                        : Set[String]
  
}