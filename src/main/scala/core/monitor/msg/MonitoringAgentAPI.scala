package core.monitor.msg

import akka.actor.ActorRef

/**
 * Starts operation runtime timer
 * seq : the operation sequence number, a unique identifier
 */
case class startTimer(seq:Int)

/**
 * Stops operation runtime timer
 * seq : the operation sequence number, a unique identifier
 */
case class stopTimer(seq:Int)

/**
 * Enables/disables interfering with number of replicas in the system
 * flg : flag that represents whether replicas can be added and removed
 */
case class interfere(flg:Boolean)