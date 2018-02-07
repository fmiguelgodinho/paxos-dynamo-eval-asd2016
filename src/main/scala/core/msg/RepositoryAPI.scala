package core.msg

/**
 *  REQUESTS
 *  key : key to associate an object with
 *  obj : the object itself
 */

case class insert(key:String, obj:String)

case class remove(key:String, obj:String)

case class isElement(key:String, obj:String)

case class list(key:String)

/**
 *  RESPONSES
 *  res : true if the element with the sent key is in set, false otherwise
 *  lst : list of objects associated with the sent key
 */

case class insertResponse()

case class removeResponse()

case class isElementResponse(res:Boolean)

case class listResponse(lst:List[String])