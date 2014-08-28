package zzz.akka.avionics

import com.typesafe.config.ConfigFactory

object RemoteConfig {

  val planeConfig  = ConfigFactory.load.getConfig("plane-remote")
  val airportHost = planeConfig.getString("zzz.akka.avionics.airport-host")
  val airportPort = planeConfig.getString("zzz.akka.avionics.airport-port")

  val airportConfig = ConfigFactory.load.getConfig("airport-remote")
  val planeHost = airportConfig.getString("zzz.akka.avionics.plane-host")
  val planePort = airportConfig.getString("zzz.akka.avionics.plane-port")

}
