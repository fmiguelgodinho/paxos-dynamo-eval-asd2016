package io

import scala.util.Random
import core.msg._

class InstructionSet {
  
  private var _instructionSet : List[Instruction] = List.empty
  private var _current : Int = 0
  
  def push = (ins : Instruction) => {
     ins sequence = _instructionSet.size
     _instructionSet ::= ins
  }
  
  def rewind = {
    _current = 0
    this
  }
  
  def next = {
    val ix = _current
    _current += 1
    _instructionSet apply ix
  } 
  
  def hasNext = _current < _instructionSet.length 
  
  def generate = {
    
    val totalInsertOps     = Math.round(InputParameters.NUMBER_OF_OPS * InputParameters.PROPORTION_INSERT)
    val totalRemoveOps     = Math.round(InputParameters.NUMBER_OF_OPS * InputParameters.PROPORTION_REMOVE)
    val totalIsElementOps  = Math.round(InputParameters.NUMBER_OF_OPS * InputParameters.PROPORTION_ISELEMENT)
    val totalListOps       = Math.round(InputParameters.NUMBER_OF_OPS * InputParameters.PROPORTION_LIST)
    
    val partialPath = "/user/" + InputParameters.REPO_SERVER_PREFIX
    
    // enqueue insert ops
    for (i <- 1 to totalInsertOps) {
			  
    			// Choose random key and value to store in the repository
  		  	val key = "key_" + Random.nextInt(InputParameters.NUMBER_OF_KEYS)
    			val value = Random.nextInt(InputParameters.RANGE_OF_VALUES.length).toString
    			
    			// Choose random server to send operation to
    			val fullPath = partialPath + (Random.nextInt(InputParameters.SERVER_POOL_SIZE) + 1)
			  
			    var op = new Instruction
			    op.operation = insert(key, value)
			    this push op 
		}
    
    // enqueue remove ops
    for (i <- 1 to totalRemoveOps) {
			  
    			// Choose random key and value to remove from the repository
  		  	val key = "key_" + Random.nextInt(InputParameters.NUMBER_OF_KEYS)
    			val value = Random.nextInt(InputParameters.RANGE_OF_VALUES.length).toString
    			
    			// Choose random server to send operation to
    			val fullPath = partialPath + (Random.nextInt(InputParameters.SERVER_POOL_SIZE) + 1)
			  
			    var op = new Instruction
			    op.operation = remove(key, value)
			    this push op 
		}
    
    // enqueue iselement ops
    for (i <- 1 to totalIsElementOps) {
			  
    			// Choose random key and value to check if exists in repository
  		  	val key = "key_" + Random.nextInt(InputParameters.NUMBER_OF_KEYS)
    			val value = Random.nextInt(InputParameters.RANGE_OF_VALUES.length).toString
    			
    			// Choose random server to send operation to
    			val fullPath = partialPath + (Random.nextInt(InputParameters.SERVER_POOL_SIZE) + 1)
			  
			    var op = new Instruction
			    op.operation = isElement(key, value)
			    this push op 
		}
    
    // enqueue list ops
    for (i <- 1 to totalListOps) {
      
    			// Choose random key to list associated values from the repository
  		  	val key = "key_" + Random.nextInt(InputParameters.NUMBER_OF_KEYS)
			  
  		  	// Choose random server to send operation to
    			val fullPath = partialPath + (Random.nextInt(InputParameters.SERVER_POOL_SIZE) + 1)
			  
			    var op = new Instruction
			    op.operation = list(key)
			    this push op 
		}
    
    // Finally, shuffle instructions in order to have random io
    _instructionSet = Random.shuffle(_instructionSet)
    
    // return itself
    this
  } 
  
}