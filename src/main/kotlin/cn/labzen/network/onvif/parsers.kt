package cn.labzen.network.onvif

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.xml.XmlMapper


abstract class MessageParser(val hostname: String, val message: String) {

  protected val parsedDevices = mutableListOf<Device>()

  abstract fun parse(): List<Device>

  companion object {
    internal val mapper: ObjectMapper = XmlMapper().apply {
      this.propertyNamingStrategy = PropertyNamingStrategies.UPPER_CAMEL_CASE
      this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
  }
}

class OnvifMessageParser(hostname: String, message: String) : MessageParser(hostname, message) {

  override fun parse(): List<Device> {
    val xml = message.trim().replace(Regex(">(\\s*)<"), "><")
    val onvifEnvelope = mapper.readValue(xml, InternalOnvifEnvelope::class.java)
    return onvifEnvelope.body?.probes?.filter {
      it.type?.contains("NetworkVideoTransmitter") == true
    }?.map {
      OnvifDevice(message, hostname, it.addr!!)
    } ?: listOf()
  }
}

class UpnpMessageParser(hostname: String, message: String) : MessageParser(hostname, message) {

  override fun parse(): List<Device> {
    parsedDevices.add(
      UpnpDevice(
        message,
        hostname,
        search("LOCATION: "),
        search("SERVER: "),
        search("USN: "),
        search("ST: ")
      )
    )
    return parsedDevices
  }

  private fun search(target: String) =
    message.indexOf(target).let { idx ->
      if (idx >= 0) {
        val start = idx + target.length
        val end = message.indexOf("\r\n", start)
        message.substring(start, end)
      } else ""
    }
}

class HikVisionMessageParser(hostname: String, message: String) : MessageParser(hostname, message) {

  override fun parse(): List<Device> {
    val xml = message.trim().replace(Regex(">(\\s*)<"), "><")
    val values = mapper.readValue(xml, object : TypeReference<Map<String, String>>() {})
    parsedDevices.add(
      HikVisionDevice(
        message,
        hostname,
        values["DeviceType"] ?: "",
        values["DeviceDescription"] ?: "",
        values["DeviceSN"] ?: "",
        values["IPv4Address"] ?: "",
        values["IPv4Gateway"] ?: "",
        values["CommandPort"]?.toInt() ?: -1,
        values["MAC"] ?: "",
        values["SoftwareVersion"] ?: "",
        values["BootTime"] ?: "",
        values["SafeCode"] ?: ""
      )
    )
    return parsedDevices
  }
}
