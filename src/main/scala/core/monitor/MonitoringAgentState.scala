package core.monitor

class MonitoringAgentState(init:Long) {

  var iniTime   : Long = init
  var endTime   : Long = Long.MinValue
  var runTime   : Long = Long.MinValue
}
