package core

case class ClientConfiguration(
    servers      : List[String], 
    monitorPath  : String, 
    serverPrefix : String
)