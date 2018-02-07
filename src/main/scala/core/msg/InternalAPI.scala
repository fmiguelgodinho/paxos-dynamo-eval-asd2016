package core.msg

import io.InstructionSet

/** 
 * Orders a client to execute/digest a full instruction set
 * ops : instruction set to be executed by the client
 */
case class digest(ops:InstructionSet)

/**
 * Message that triggers death on a server node
 */
case class die()

/**
 * Message that notifies death to other server nodes
 */
case class bye()

/**
 * Message that triggers sibling creation on a server node
 * name : the name of the new server node
 */
case class replicate(name:String)

/**
 * Message that notifies the presence of an extra node to other server nodes
 */
case class hi()

/**
 * Message broadcasted by clients to hearbeat servers, if needed
 */
case class marco()

/**
 * Message sent back by servers to client after marco() is received
 */
case class polo()