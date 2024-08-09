@file:Suppress("unused")

package cn.labzen.network.onvif

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

abstract class Device(val message: String, val host: String)

class OnvifDevice(
  message: String,
  host: String,
  val address: String
) : Device(message, host)

internal class InternalOnvifEnvelope {
  @JacksonXmlProperty(localName = "Body")
  var body: InternalOnvifBody? = null
}

internal class InternalOnvifBody {
  @JacksonXmlElementWrapper(localName = "ProbeMatches")
  @JacksonXmlProperty(localName = "ProbeMatch")
  var probes: List<InternalOnvifProbeMatch>? = null
}

internal class InternalOnvifProbeMatch {

  @JacksonXmlProperty(localName = "Types")
  var type: String? = null

  @JacksonXmlProperty(localName = "XAddrs")
  var addr: String? = null
}

// ===================================================================

class UpnpDevice(
  message: String,
  host: String,
  val location: String,
  val server: String,
  val usn: String,
  val st: String
) : Device(message, host)

class HikVisionDevice(
  message: String,
  host: String,
  val type: String,
  val description: String,
  val sn: String,
  val ipv4: String,
  val gateway: String,
  val port: Int,
  val mac: String,
  val version: String,
  val bootTime: String,
  val safeCode: String
) : Device(message, host)
