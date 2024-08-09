package cn.labzen.network.onvif

enum class DiscoveryMode(val port: Int) {

  ONVIF(3702),
  UPNP(1900),
  HIK_VISION(37020)
}
