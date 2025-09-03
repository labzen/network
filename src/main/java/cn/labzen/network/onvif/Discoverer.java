package cn.labzen.network.onvif;

import cn.labzen.network.exception.OnvifException;
import cn.labzen.network.onvif.device.Device;
import cn.labzen.network.onvif.listener.DiscoveredAllDevicesListener;
import cn.labzen.network.onvif.listener.DiscoveredHostDevicesListener;
import cn.labzen.network.onvif.listener.DiscoveryFinishedListener;
import cn.labzen.network.onvif.listener.DiscoveryStartedListener;
import cn.labzen.network.onvif.parse.HikVisionMessageParser;
import cn.labzen.network.onvif.parse.OnvifMessageParser;
import cn.labzen.network.onvif.parse.UpnpMessageParser;
import cn.labzen.tool.feature.id.SystemClock;
import cn.labzen.tool.util.Randoms;

import java.net.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Discoverer {

  private static final String MULTICAST_ADDRESS_IPV4 = "239.255.255.250"; // Simple Service Discovery Protocol
  private static final String MULTICAST_ADDRESS_IPV6 = "[FF02::C]";

  private DiscoveryMode mode = DiscoveryMode.HIK_VISION;
  private DiscoveryStartedListener startedListener;
  private DiscoveredAllDevicesListener discoveredAllDevicesListener;
  private DiscoveredHostDevicesListener discoveredHostDevicesListener;
  private DiscoveryFinishedListener discoveryFinishedListener;
  private CountDownLatch latch;

  private final List<Device> foundDevices = new CopyOnWriteArrayList<>();
  private final AtomicInteger foundCount = new AtomicInteger(0);
  //private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1,
  //    5,
  //    20,
  //    TimeUnit.SECONDS,
  //    new ArrayBlockingQueue<>(10));

  private final int timeout;

  Discoverer(int timeout) {
    this.timeout = timeout;
  }

  public Discoverer setMode(DiscoveryMode mode) {
    this.mode = mode;
    return this;
  }

  public Discoverer listen(DiscoveryStartedListener startedListener) {
    this.startedListener = startedListener;
    return this;
  }

  public Discoverer listen(DiscoveredAllDevicesListener discoveredAllDevicesListener) {
    this.discoveredAllDevicesListener = discoveredAllDevicesListener;
    return this;
  }

  public Discoverer listen(DiscoveredHostDevicesListener discoveredHostDevicesListener) {
    this.discoveredHostDevicesListener = discoveredHostDevicesListener;
    return this;
  }

  public Discoverer listen(DiscoveryFinishedListener discoveryFinishedListener) {
    this.discoveryFinishedListener = discoveryFinishedListener;
    return this;
  }

  public void discovery() {
    List<InetAddress> inetAddresses = Onvif.interfaceAddresses();
    assert inetAddresses != null;
    List<Runnable> runners = inetAddresses.stream().map(this::discoveryRunner).toList();

    latch = new CountDownLatch(inetAddresses.size());
    if (startedListener != null) {
      startedListener.started();
    }

    //Create a new cached thread pool and a monitor service
    try (ExecutorService executorService = Executors.newCachedThreadPool();
         ExecutorService monitor = Executors.newSingleThreadExecutor();) {
      //Execute a new thread for every probe that should be sent.
      monitor.submit(() -> {
        for (Runnable runner : runners) {
          executorService.submit(runner);
        }

        try {
          executorService.shutdown();
          //noinspection ResultOfMethodCallIgnored
          latch.await(timeout, TimeUnit.MILLISECONDS);
          boolean cleanShutdown = executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
          if (!cleanShutdown) {
            executorService.shutdownNow();
          }

          if (discoveredAllDevicesListener != null) {
            discoveredAllDevicesListener.found(foundDevices);
          }
          if (discoveryFinishedListener != null) {
            discoveryFinishedListener.finished(foundCount.get());
          }
        } catch (InterruptedException e) {
          // ignore this exception
        }
      });
      monitor.shutdown();
    }
  }

  private Runnable discoveryRunner(InetAddress address) {
    return () -> {
      int port = Randoms.intNumber(20000, 65530);
      try (DatagramSocket datagramSocket = new DatagramSocket(port, address)) {
        datagramSocket.setBroadcast(true);
        datagramSocket.setSoTimeout(timeout);

        Packet packet = new Packet(UUID.randomUUID().toString(), mode);
        String message = packet.toData();

        new Thread(() -> {
          long timerStarted = SystemClock.now();
          send(datagramSocket, address, message);

          try {
            while (SystemClock.now() - timerStarted < timeout) {
              DatagramPacket dp = new DatagramPacket(new byte[8192], 8192);
              datagramSocket.receive(dp);

              String response = new String(dp.getData(), 0, dp.getLength());
              String hostname = dp.getAddress().getHostName();
              List<? extends Device> devices = switch (mode) {
                case ONVIF -> new OnvifMessageParser(hostname, response).parse();
                case UPNP -> new UpnpMessageParser(hostname, response).parse();
                case HIK_VISION -> new HikVisionMessageParser(hostname, response).parse();
              };

              foundCount.addAndGet(devices.size());

              if (discoveredHostDevicesListener != null) {
                discoveredHostDevicesListener.found(hostname, devices);
              }
              if (discoveredAllDevicesListener != null) {
                discoveredAllDevicesListener.found(devices);
              }
            }
          } catch (Exception e) {
            throw new OnvifException(e);
          } finally {
            latch.countDown();
          }
        }).start();
      } catch (SocketException e) {
        throw new OnvifException(e);
      }
    };
  }

  private void send(DatagramSocket socket, InetAddress address, String message) {
    try {
      InetAddress addressByName;
      if (address instanceof Inet4Address) {
        addressByName = InetAddress.getByName(MULTICAST_ADDRESS_IPV4);
      } else {
        addressByName = InetAddress.getByName(MULTICAST_ADDRESS_IPV6);
      }

      byte[] data = message.getBytes();
      DatagramPacket datagramPacket = new DatagramPacket(data, data.length, addressByName, mode.getPort());
      socket.send(datagramPacket);
    } catch (Exception e) {
      throw new OnvifException(e);
    }
  }
}
