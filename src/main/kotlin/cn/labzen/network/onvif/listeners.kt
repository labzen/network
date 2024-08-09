package cn.labzen.network.onvif

/**
 * Onvif发现执行开始触发
 */
interface DiscoveryStartedListener {

  fun started()
}

/**
 * Onvif发现总时长完成后出发，将所有发现的设备返回
 */
interface DiscoveredAllDevicesListener {

  fun found(devices: List<Device>)
}

/**
 * 在单一的host上发现设备时，即时触发返回
 */
interface DiscoveredHostDevicesListener {

  fun found(hostname: String, devices: List<Device>)
}

interface DiscoveryFinishedListener {

  fun finished(deviceCount: Int)
}
