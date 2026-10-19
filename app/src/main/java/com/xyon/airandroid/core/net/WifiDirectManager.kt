package com.xyon.airandroid.core.net

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.xyon.airandroid.model.PeerDevice

class WifiDirectManager(
    private val context: Context,
    private val onEvent: (Event) -> Unit,
) {
    sealed interface Event {
        data object Unsupported : Event
        data class EnabledChanged(val enabled: Boolean) : Event
        data class PeersChanged(val peers: List<PeerDevice>) : Event
        data class Connected(val info: WifiP2pInfo) : Event
        data object Disconnected : Event
        data class GroupCreated(val ssid: String?) : Event
        data class Error(val message: String) : Event
    }

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null
    private var registered = false

    fun start() {
        if (manager == null) {
            onEvent(Event.Unsupported)
            return
        }
        if (channel == null) {
            channel = manager.initialize(context, context.mainLooper, null)
        }
        if (!registered) {
            val filter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                            val state = intent.getIntExtra(
                                WifiP2pManager.EXTRA_WIFI_STATE,
                                WifiP2pManager.WIFI_P2P_STATE_DISABLED,
                            )
                            onEvent(
                                Event.EnabledChanged(
                                    state == WifiP2pManager.WIFI_P2P_STATE_ENABLED,
                                ),
                            )
                        }

                        WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                        WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION ->
                            requestConnectionInfo()

                        WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> Unit
                    }
                }
            }.also {
                ContextCompat.registerReceiver(
                    context,
                    it,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED,
                )
                registered = true
            }
        }
    }

    fun stop() {
        if (registered) {
            runCatching { context.unregisterReceiver(receiver) }
            registered = false
        }
        receiver = null
    }

    fun hasSupport(): Boolean = manager != null

    fun hasPermissions(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        val mgr = manager ?: return
        val chan = channel ?: return
        if (!hasPermissions()) {
            onEvent(Event.Error("Concede el permiso de dispositivos cercanos para buscar"))
            return
        }
        removeGroupInternal {
            mgr.discoverPeers(chan, object : WifiP2pManager.ActionListener {
                override fun onSuccess() = Unit
                override fun onFailure(reason: Int) {
                    onEvent(Event.Error("No se pudo iniciar la búsqueda (código $reason)"))
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        val mgr = manager ?: return
        val chan = channel ?: return
        if (!hasPermissions()) {
            onEvent(Event.Error("Concede el permiso de dispositivos cercanos para recibir"))
            return
        }
        removeGroupInternal {
            mgr.createGroup(chan, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    mgr.requestGroupInfo(chan) { group ->
                        onEvent(Event.GroupCreated(group?.networkName))
                    }
                }

                override fun onFailure(reason: Int) {
                    onEvent(Event.Error("No se pudo crear el grupo Wi-Fi Direct (código $reason)"))
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: PeerDevice) {
        val mgr = manager ?: return
        val chan = channel ?: return
        if (!hasPermissions()) {
            onEvent(Event.Error("Concede el permiso de dispositivos cercanos para conectar"))
            return
        }
        val config = WifiP2pConfig().apply {
            deviceAddress = device.address
            groupOwnerIntent = 0
        }
        mgr.connect(chan, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = Unit
            override fun onFailure(reason: Int) {
                onEvent(Event.Error("No se pudo conectar con ${device.displayName} (código $reason)"))
            }
        })
    }

    fun removeGroup() {
        removeGroupInternal {}
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        val mgr = manager ?: return
        val chan = channel ?: return
        if (!hasPermissions()) return
        mgr.requestPeers(chan) { deviceList ->
            val peers = deviceList?.deviceList.orEmpty()
                .map {
                    PeerDevice(
                        name = it.deviceName.orEmpty(),
                        address = it.deviceAddress.orEmpty(),
                        status = it.status,
                        isGroupOwner = it.isGroupOwner,
                    )
                }
                .filter { it.address.isNotBlank() }
                .sortedWith(
                    compareBy<PeerDevice> { it.status != WifiP2pDevice.AVAILABLE }
                        .thenBy { it.displayName.lowercase() },
                )
            onEvent(Event.PeersChanged(peers))
        }
    }

    private fun requestConnectionInfo() {
        val mgr = manager ?: return
        val chan = channel ?: return
        mgr.requestConnectionInfo(chan) { info ->
            if (info == null) {
                onEvent(Event.Disconnected)
            } else if (info.groupFormed) {
                onEvent(Event.Connected(info))
            } else {
                onEvent(Event.Disconnected)
            }
        }
    }

    private fun removeGroupInternal(then: () -> Unit) {
        val mgr = manager ?: run {
            then()
            return
        }
        val chan = channel ?: run {
            then()
            return
        }
        mgr.removeGroup(chan, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = then()
            override fun onFailure(reason: Int) = then()
        })
    }
}
