package cn.labzen.network.onvif.listener;

import cn.labzen.network.onvif.device.Device;

import java.util.List;

/**
 * Onvif发现总时长完成后出发，将所有发现的设备返回
 */
public interface DiscoveredAllDevicesListener {

  void found(List<? extends Device> devices);
}
