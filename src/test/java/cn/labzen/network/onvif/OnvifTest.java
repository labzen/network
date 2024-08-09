package cn.labzen.network.onvif;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.time.LocalDateTime;

public class OnvifTest {

  private Logger logger = LoggerFactory.getLogger(OnvifTest.class);

  @Test
  void testDiscovery() {
    long timeout = 20000;

    logger.info(() -> "broadcast addresses: " + Onvif.INSTANCE.broadcastAddresses());
    logger.info(() -> "interface addresses: " + Onvif.INSTANCE.interfaceAddresses());

    logger.info(() -> "========================================================");

    Onvif.create((int) timeout)
         .mode(DiscoveryMode.HIK_VISION)
         .listen(() -> logger.info(() -> "started at " + LocalDateTime.now()))
         .listen((hostname, devices) -> {
           logger.info(() -> "===> found single host <<" + hostname + ">> devices....");
           devices.forEach(this::print);
         })
         .listen((DiscoveredAllDevicesListener) (devices) -> {
           logger.info(() -> "discovered all devices at " + LocalDateTime.now());
           devices.forEach(this::print);
         })
         .listen((DiscoveryFinishedListener) (count) -> {
           logger.info(() -> "finished with " + count + " device discovered at " + LocalDateTime.now());
         })
         .discovery();

    try {
      Thread.sleep(timeout + 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void print(Device device) {
    HikVisionDevice hk = (HikVisionDevice) device;
    logger.info(() -> "''" + hk.getDescription() + "'' at |" + hk.getIpv4() + "|, <" + hk.getMac() + ">");
  }
}
