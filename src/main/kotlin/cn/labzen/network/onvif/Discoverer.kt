package cn.labzen.network.onvif

import cn.labzen.tool.feature.SystemClock
import cn.labzen.tool.util.Randoms
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class Discoverer(private val timeout: Int) {

  private var mode = DiscoveryMode.HIK_VISION
  private var startedListener: DiscoveryStartedListener? = null
  private var discoveredAllDevicesListener: DiscoveredAllDevicesListener? = null
  private var discoveredHostDevicesListener: DiscoveredHostDevicesListener? = null
  private var discoveryFinishedListener: DiscoveryFinishedListener? = null
  private var latch: CountDownLatch? = null

  private val foundDevices = CopyOnWriteArrayList<Device>()
  private val foundCount = AtomicInteger()

  fun mode(mode: DiscoveryMode): Discoverer {
    this.mode = mode
    return this
  }

  fun listen(startedListener: DiscoveryStartedListener): Discoverer {
    this.startedListener = startedListener
    return this
  }

  fun listen(discoveredAllDevicesListener: DiscoveredAllDevicesListener): Discoverer {
    this.discoveredAllDevicesListener = discoveredAllDevicesListener
    return this
  }

  fun listen(discoveredHostDevicesListener: DiscoveredHostDevicesListener): Discoverer {
    this.discoveredHostDevicesListener = discoveredHostDevicesListener
    return this
  }

  fun listen(discoveryFinishedListener: DiscoveryFinishedListener): Discoverer {
    this.discoveryFinishedListener = discoveryFinishedListener
    return this
  }

  fun discovery() {
    val interfaceAddresses = Onvif.interfaceAddresses()
    val runners = interfaceAddresses.map { discoveryRunner(it) }

    latch = CountDownLatch(interfaceAddresses.size)
    startedListener?.started()

    //Create a new cached thread pool and a monitor service
    val executorService = Executors.newCachedThreadPool()
    val monitor = Executors.newSingleThreadExecutor()

    //Execute a new thread for every probe that should be sent.
    monitor.submit {
      runners.forEach { executorService.submit(it) }

      //Stop accepting new tasks and shuts down threads as they finish
      try {
        executorService.shutdown()
        latch!!.await(timeout.toLong(), TimeUnit.MILLISECONDS)
        val cleanShutdown = executorService.awaitTermination(timeout.toLong(), TimeUnit.MILLISECONDS)
        if (!cleanShutdown) {
          executorService.shutdownNow()
        }
        discoveredAllDevicesListener?.found(foundDevices)
        discoveryFinishedListener?.finished(foundCount.get())
      } catch (e: InterruptedException) {
        // ignore this exception
      }
    }
    monitor.shutdown()
  }

  private fun discoveryRunner(address: InetAddress) =
    Runnable {
      val port = Randoms.intNumber(20000, 65535)
      val client = DatagramSocket(port, address).apply {
        this.broadcast = true
        this.soTimeout = timeout
      }

      val message = Packet(UUID.randomUUID().toString(), mode).toData()
      thread {
        try {
          val timerStarted = SystemClock.now()
          send(client, address, message)
          while (SystemClock.now() - timerStarted < timeout) {
            val packet = DatagramPacket(ByteArray(8192), 8192)
            client.receive(packet)
            // todo 这里解析设备信息的部分，因为是在单独的线程中执行，所以存在还有未完的线程，但整个大的runner已结束，造成实际上返回的所有设备不全的现象
            thread {
              val response = String(packet.data, 0, packet.length)
              val hostname = packet.address.hostName

              val devices = when (mode) {
                DiscoveryMode.ONVIF -> OnvifMessageParser(hostname, response).parse()
                DiscoveryMode.UPNP -> UpnpMessageParser(hostname, response).parse()
                DiscoveryMode.HIK_VISION -> HikVisionMessageParser(hostname, response).parse()
              }
              foundCount.addAndGet(devices.size)
              discoveredHostDevicesListener?.also {
                it.found(hostname, devices)
              }

              discoveredAllDevicesListener?.also {
                foundDevices.addAll(devices)
              }
            }
          }
        } catch (e: IOException) {
          // ignore this exception
        } finally {
          client.close()
          latch!!.countDown()
        }
      }
    }

  private fun send(client: DatagramSocket, address: InetAddress, message: String) {
    val addressByName =
      if (address is Inet4Address) InetAddress.getByName(MULTICAST_ADDRESS_IPV4)
      else InetAddress.getByName(MULTICAST_ADDRESS_IPV6)

    try {
      val data = message.toByteArray()
      client.send(DatagramPacket(data, data.size, addressByName, mode.port))
    } catch (ignored: IOException) {
      // ignore this exception
    }
  }

  companion object {
    private const val MULTICAST_ADDRESS_IPV4 = "239.255.255.250" // Simple Service Discovery Protocol
    private const val MULTICAST_ADDRESS_IPV6 = "[FF02::C]"
  }
}
