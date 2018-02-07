package core.dynamo

object ClockComparison extends Enumeration {
  type ClockComparison = Value
  val Bigger, Smaller, Incomparable = Value
}

/**
 * Dynamo object context, contains a vectorial clock for concurrency detection
 *  rpl : list of replicas of the object this context belongs to
 */
class DynamoContext(rpl:List[String]) {
  
  // Vector clock
  var clk = rpl map (replica => replica.replace("/user/", "") -> 0) toMap
  
  def incrementClock(ix:String) = {
    
    clk get ix match {
          case bucket if (bucket isEmpty) =>
            throw new Exception("Vector clock has no reference to the specified index!")
          case bucket =>
            clk += (ix -> (bucket.get + 1))
    }
    
    this
  }
  
  def compareClock(that:DynamoContext) = {
    
    val mine = clk.values toList
    val theirs = that.clk.values toList
    
    if (mine.size != theirs.size)
      throw new Exception("Vector clocks have different sizes!")
    
    mine.corresponds(theirs) { _ >= _ } match {
      case true => 
        ClockComparison.Bigger
      case false if theirs.corresponds(mine) { _ >= _ } => 
        ClockComparison.Smaller
      case false =>
        ClockComparison.Incomparable
    }
    
  }
  
  def mergeClock(that:DynamoContext) = {
    
    var newCtx = new DynamoContext(rpl)
    
    rpl isEmpty match {
      case true =>
        newCtx = that
      case false => 
        newCtx.clk = (this.clk zip that.clk).map{ 
          case (mine, theirs) => mine._1 -> Math.max(mine._2, theirs._2)
        }
    }
    
    newCtx
  }
}