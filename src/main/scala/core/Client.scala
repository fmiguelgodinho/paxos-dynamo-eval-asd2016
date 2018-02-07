package core;

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import scala.util.Random
import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.concurrent.TimeoutException
import core.msg._
import core.monitor.msg._
import io._

class Client(cfg:ClientConfiguration) extends Actor {
  
  var activeServers = cfg.servers
  
  
  def getTarget = {
    try {
        // check if we can communicate with some server
        if (activeServers isEmpty) {
          implicit val timeout = Timeout(1000 milliseconds)
          val future = context.actorSelection("/user/" + cfg.serverPrefix + "*") ? marco() 
          Await.result(future, timeout.duration) match {
            case polo() => activeServers ::= "/user/" + sender.path.name
          }
        }
    } catch {
      case ex: TimeoutException => System.err.println("There are no available servers.")
    }
          
    activeServers(Random.nextInt(activeServers size))
  }
  
  
  def receive = {
    
    case digest(ops) => 
      
      while (ops.hasNext) {
        
        // consume instruction set
        var op = ops next
        var target = getTarget
        
        // interpret what we're sending
        // this is need due to the ask pattern
        val request = op.operation match {
          
          case insert(key, obj) =>
            op.operation.asInstanceOf[insert]
            
          case remove(key, obj) =>
            op.operation.asInstanceOf[remove]
            
          case isElement(key, obj) =>
            op.operation.asInstanceOf[isElement]
            
          case list(key) =>
            op.operation.asInstanceOf[list]
        } 
        
        var success = false
        // keep trying to execute operation
        while (!success) {
          try {
              implicit val timeout = Timeout(1000 milliseconds)
              val future = context.actorSelection(target) ? request
              
              // ask monitor to start timer on this op
              context.actorSelection(cfg.monitorPath) ! startTimer(op.sequence)
            
              // block and await response
              Await.result(future, timeout.duration) match {
                
                case insertResponse() =>         // wrote new value to repository
                  //println("Got ack insert.")
                  context.actorSelection(cfg.monitorPath) ! stopTimer(op.sequence)
                case removeResponse() =>         // removed value from repository
                  //println("Got ack remove.")
                  context.actorSelection(cfg.monitorPath) ! stopTimer(op.sequence)
                case isElementResponse(res) =>   // got result on whether element exists
                  //println("Got response isElement: " + res)
                  context.actorSelection(cfg.monitorPath) ! stopTimer(op.sequence)
                case listResponse(lst) =>        // list of values
                  //println("Got response list: " + lst)
                  context.actorSelection(cfg.monitorPath) ! stopTimer(op.sequence)
                
                case _ => System.err.println("Unexpected response from server.")
              }
              
              // message was successfully delivered and expected response was received
              success = true
              
          } catch {
              case ex: TimeoutException => 
                //System.err.println("Server took too long to respond.")
                // remove inactive server, choose another target
                activeServers = activeServers filter (_ != target)
                target = getTarget
          }
        }
      }
      
      activeServers = cfg.servers
  }
}