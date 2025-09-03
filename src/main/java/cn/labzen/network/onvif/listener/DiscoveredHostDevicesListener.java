package cn.labzen.network.onvif.listener;

import cn.labzen.network.onvif.device.Device;

import java.util.List;

/**
 * 在单一的host上发现设备时，即时触发返回
 */
public interface DiscoveredHostDevicesListener {

  void found(String hostname, List<? extends Device> devices);
}
