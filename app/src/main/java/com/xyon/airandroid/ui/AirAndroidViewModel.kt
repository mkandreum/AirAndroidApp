package com.xyon.airandroid.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xyon.airandroid.core.crypto.Crypto
import com.xyon.airandroid.core.net.FileTransferClient
import com.xyon.airandroid.core.net.FileTransferServer
import com.xyon.airandroid.core.net.TransferProtocol
import com.xyon.airandroid.core.net.WifiDirectManager
import com.xyon.airandroid.core.nfc.NfcController
import com.xyon.airandroid.core.nfc.NfcPairingStore
import com.xyon.airandroid.data.HistoryRepository
import com.xyon.airandroid.data.ReceivedFileStore
import com.xyon.airandroid.data.SettingsRepository
import com.xyon.airandroid.model.AppSettings
import com.xyon.airandroid.model.ConnectionState
import com.xyon.airandroid.model.HistoryItem
import com.xyon.airandroid.model.NfcPairing
import com.xyon.airandroid.model.PeerDevice
import com.xyon.airandroid.model.ReceiveUiState
import com.xyon.airandroid.model.Role
import com.xyon.airandroid.model.SelectedFile
import com.xyon.airandroid.model.TransferDirection
import com.xyon.airandroid.model.TransferStatus
import com.xyon.airandroid.model.TransferUiState
import com.xyon.airandroid.model.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.UUID

class AirAndroidViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val settingsRepository = SettingsRepository(context)
    private val historyRepository = HistoryRepository(context)
    private val receivedFileStore = ReceivedFileStore(context)
    private val transferClient = FileTransferClient(context)
    private val transferServer = FileTransferServer(receivedFileStore)
    private val nfcController = NfcController(context)
    private val wifiDirect = WifiDirectManager(context, ::onWifiEvent)

    private val _settings = MutableStateFlow(settingsRepository.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _history = MutableStateFlow(historyRepository.load())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _connection = MutableStateFlow(
        ConnectionState(
            supported = wifiDirect.hasSupport(),
            nfcAvailable = nfcController.hasNfc(),
            nfcEnabled = nfcController.isEnabled(),
        ),
    )
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _peers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peers: StateFlow<List<PeerDevice>> = _peers.asStateFlow()

    private val _selectedPeer = MutableStateFlow<PeerDevice?>(null)
    val selectedPeer: StateFlow<PeerDevice?> = _selectedPeer.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _files = MutableStateFlow<List<SelectedFile>>(emptyList())
    val files: StateFlow<List<SelectedFile>> = _files.asStateFlow()

    private val _sendPin = MutableStateFlow("")
    val sendPin: StateFlow<String> = _sendPin.asStateFlow()

    private val _transfer = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val transfer: StateFlow<TransferUiState> = _transfer.asStateFlow()

    private val _receive = MutableStateFlow(ReceiveUiState())
    val receive: StateFlow<ReceiveUiState> = _receive.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 12)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var role = Role.IDLE
    private var currentGroupSsid: String? = null
    private var serverRunning = false
    private var groupOwnerAddress: InetAddress? = null
    private var requestedPeerAddress: String? = null
    private var connectedPeerAddress: String? = null
    private var pendingSendPin: String? = null
    private var nfcTargetName: String? = null
    private var sendJob: Job? = null
    private var cancelRequested = false

    init {
        wifiDirect.start()
        refreshNfcState()
    }

    fun onAppResumed() {
        refreshNfcState()
        updateNfcPairing()
    }

    fun onAppPaused(activity: Activity) {
        nfcController.stopReader()
        NfcPairingStore.update(null)
        _connection.update { it.copy(nfcReaderActive = false) }
        refreshNfcState()
    }

    fun refreshNfcState() {
        _connection.update {
            it.copy(
                nfcAvailable = nfcController.hasNfc(),
                nfcEnabled = nfcController.isEnabled(),
                nfcHceReady = nfcController.isHceReady(),
            )
        }
    }

    fun startSend() {
        if (role == Role.RECEIVER) stopReceive()
        role = Role.SENDER
        cancelRequested = false
        sendJob?.cancel()
        transferClient.cancel()
        pendingSendPin = null
        groupOwnerAddress = null
        requestedPeerAddress = null
        connectedPeerAddress = null
        _selectedPeer.value = null
        _peers.value = emptyList()
        _transfer.value = TransferUiState.Idle
        _connection.update {
            it.copy(
                role = Role.SENDER,
                groupReady = false,
                connectedPeerName = null,
                message = null,
            )
        }
        _scanning.value = true
        wifiDirect.start()
        wifiDirect.discoverPeers()
    }

    fun scanPeers() {
        if (role != Role.SENDER) {
            startSend()
            return
        }
        _scanning.value = true
        wifiDirect.discoverPeers()
    }

    fun selectPeer(peer: PeerDevice) {
        if (_selectedPeer.value?.address != peer.address) {
            groupOwnerAddress = null
            requestedPeerAddress = null
            connectedPeerAddress = null
            _connection.update { it.copy(groupReady = false, connectedPeerName = null) }
        }
        _selectedPeer.value = peer
    }

    fun setSendPin(value: String) {
        _sendPin.value = value.filter(Char::isDigit).take(6)
    }

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val added = mutableListOf<SelectedFile>()
            var problem: String? = null
            uris.forEach { uri ->
                runCatching {
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val metadata = runCatching { resolveMetadata(uri) }.getOrNull()
                if (metadata == null) {
                    problem = "No se pudo leer uno de los archivos"
                    return@forEach
                }
                added.add(metadata)
            }

            val merged = (_files.value + added)
                .distinctBy { it.uri.toString() }
                .take(TransferProtocol.MAX_FILES)
            val total = merged.sumOf { it.size }
            if (total > TransferProtocol.MAX_FILE_BYTES * 2) {
                emit(UiEvent.Message("Los archivos seleccionados son demasiado grandes"))
                return@launch
            }
            _files.value = merged
            problem?.let { emit(UiEvent.Message(it)) }
        }
    }

    fun removeFile(uri: Uri) {
        _files.value = _files.value.filterNot { it.uri == uri }
    }

    fun clearFiles() {
        _files.value = emptyList()
    }

    fun sendSelectedFiles() {
        val filesSnapshot = _files.value
        val peer = _selectedPeer.value
        val pin = _sendPin.value
        if (filesSnapshot.isEmpty()) {
            emit(UiEvent.Message("Selecciona al menos un archivo"))
            return
        }
        if (peer == null) {
            emit(UiEvent.Message("Selecciona el móvil que va a recibir"))
            return
        }
        if (!pin.matches(Regex("\\d{6}"))) {
            emit(UiEvent.Message("Introduce el código de 6 dígitos del receptor"))
            return
        }

        pendingSendPin = pin
        requestedPeerAddress = peer.address

        val host = groupOwnerAddress
        if (host != null && connectedPeerAddress == peer.address &&
            _connection.value.groupReady
        ) {
            launchSend(host, pin, filesSnapshot)
        } else {
            _transfer.value = TransferUiState.Connecting("Conectando con ${peer.displayName}…")
            runCatching {
                wifiDirect.connect(peer)
            }.onFailure { error ->
                val message = error.message ?: "No se pudo iniciar la conexión"
                _transfer.value = TransferUiState.Failed(message)
                emit(UiEvent.Message(message))
            }
        }
    }

    fun cancelTransfer() {
        cancelRequested = true
        sendJob?.cancel()
        sendJob = null
        transferClient.cancel()
        transferServer.cancelActive()
        pendingSendPin = null
        _transfer.value = TransferUiState.Idle
        emit(UiEvent.Message("Transferencia cancelada"))
    }

    fun startReceive() {
        if (role == Role.SENDER) {
            sendJob?.cancel()
            transferClient.cancel()
        }
        transferServer.stop()
        role = Role.RECEIVER
        cancelRequested = false
        serverRunning = false
        val pin = Crypto.randomPin()
        currentGroupSsid = null
        groupOwnerAddress = null
        requestedPeerAddress = null
        pendingSendPin = null
        connectedPeerAddress = null
        _receive.value = ReceiveUiState(active = true, pin = pin, waiting = true)
        _transfer.value = TransferUiState.Idle
        _connection.update {
            it.copy(
                role = Role.RECEIVER,
                groupReady = false,
                connectedPeerName = null,
                message = null,
            )
        }
        _peers.value = emptyList()
        _selectedPeer.value = null
        _scanning.value = false
        updateNfcPairing()
        startServer()
        wifiDirect.start()
        wifiDirect.createGroup()
    }

    fun stopReceive() {
        transferServer.stop()
        serverRunning = false
        wifiDirect.removeGroup()
        role = Role.IDLE
        currentGroupSsid = null
        groupOwnerAddress = null
        connectedPeerAddress = null
        _receive.value = ReceiveUiState()
        _transfer.value = TransferUiState.Idle
        _connection.update {
            it.copy(
                role = Role.IDLE,
                groupReady = false,
                connectedPeerName = null,
                message = null,
            )
        }
        NfcPairingStore.update(null)
    }

    fun regeneratePin() {
        if (!_receive.value.active) return
        var pin = Crypto.randomPin()
        if (pin == _receive.value.pin) pin = Crypto.randomPin()
        _receive.update {
            it.copy(pin = pin, badPinAttempts = 0, waiting = true, peerName = null)
        }
        updateNfcPairing()
        emit(UiEvent.Message("Nuevo código generado"))
    }

    fun startNfcRead(activity: Activity) {
        refreshNfcState()
        if (!nfcController.hasNfc()) {
            emit(UiEvent.Message("Este móvil no tiene NFC"))
            return
        }
        if (!nfcController.isEnabled()) {
            emit(UiEvent.Message("Activa NFC en los ajustes del móvil"))
            return
        }
        _connection.update { it.copy(nfcReaderActive = true) }
        nfcController.startReader(
            activity = activity,
            mode = NfcController.Mode.READ,
            onPairing = { pairing ->
                _connection.update { it.copy(nfcReaderActive = false) }
                handleNfcPairing(pairing)
            },
            onWritten = {},
            onError = { message ->
                _connection.update { it.copy(nfcReaderActive = false) }
                emit(UiEvent.Message(message))
            },
        )
    }

    fun startNfcWrite(activity: Activity) {
        val state = _receive.value
        val pin = state.pin
        if (!state.active || pin == null) {
            emit(UiEvent.Message("Activa la recepción antes de grabar un tag"))
            return
        }
        refreshNfcState()
        if (!nfcController.hasNfc()) {
            emit(UiEvent.Message("Este móvil no tiene NFC"))
            return
        }
        if (!nfcController.isEnabled()) {
            emit(UiEvent.Message("Activa NFC en los ajustes del móvil"))
            return
        }
        val pairing = NfcPairing(
            ssid = currentGroupSsid.orEmpty(),
            pin = pin,
            deviceName = _settings.value.deviceName,
        )
        _connection.update { it.copy(nfcReaderActive = true) }
        nfcController.startReader(
            activity = activity,
            mode = NfcController.Mode.WRITE,
            pairingToWrite = pairing,
            onPairing = {},
            onWritten = {
                _connection.update { it.copy(nfcReaderActive = false) }
                emit(UiEvent.Message("Tag NFC grabado"))
            },
            onError = { message ->
                _connection.update { it.copy(nfcReaderActive = false) }
                emit(UiEvent.Message(message))
            },
        )
    }

    fun stopNfcReader() {
        nfcController.stopReader()
        _connection.update { it.copy(nfcReaderActive = false) }
    }

    fun saveDeviceName(name: String) {
        settingsRepository.saveDeviceName(name)
        val clean = settingsRepository.load()
        _settings.value = clean
        updateNfcPairing()
    }

    fun clearHistory() {
        historyRepository.clear()
        _history.value = emptyList()
    }

    fun dismissTransferStatus() {
        val state = _transfer.value
        if (state is TransferUiState.Completed ||
            state is TransferUiState.Failed ||
            state is TransferUiState.Cancelled
        ) {
            _transfer.value = TransferUiState.Idle
        }
    }

    override fun onCleared() {
        sendJob?.cancel()
        transferClient.cancel()
        transferServer.destroy()
        wifiDirect.removeGroup()
        wifiDirect.stop()
        nfcController.stopReader()
        NfcPairingStore.update(null)
        super.onCleared()
    }

    private fun handleNfcPairing(pairing: NfcPairing) {
        setSendPin(pairing.pin)
        nfcTargetName = pairing.deviceName
        if (role != Role.SENDER) {
            startSend()
        } else {
            _scanning.value = true
            wifiDirect.discoverPeers()
        }
        val match = _peers.value.firstOrNull { namesMatch(it.displayName, pairing.deviceName) }
        if (match != null) {
            val updated = match.copy(name = pairing.deviceName)
            _peers.value = _peers.value.map {
                if (it.address == match.address) updated else it
            }
            selectPeer(updated)
            nfcTargetName = null
        }
        emit(UiEvent.Message("Código leído por NFC. Elige el móvil y pulsa Enviar."))
    }

    private fun launchSend(host: InetAddress, pin: String, files: List<SelectedFile>) {
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            try {
                _transfer.value = TransferUiState.Connecting("Conectado. Preparando envío…")
                val result = transferClient.send(
                host = host,
                pin = pin,
                deviceName = _settings.value.deviceName,
                files = files,
                onConnected = { peerName ->
                    _connection.update { it.copy(connectedPeerName = peerName) }
                    val selected = _selectedPeer.value
                    if (selected != null) {
                        val updated = selected.copy(name = peerName)
                        _selectedPeer.value = updated
                        _peers.value = _peers.value.map {
                            if (it.address == selected.address) updated else it
                        }
                    }
                },
                onProgress = { progress ->
                    _transfer.value = TransferUiState.Sending(progress)
                },
            )
            pendingSendPin = null
            if (result.success) {
                val now = System.currentTimeMillis()
                val target = _connection.value.connectedPeerName
                    ?: _selectedPeer.value?.displayName
                    ?: "Android"
                files.forEach { file ->
                    val item = HistoryItem(
                        id = UUID.randomUUID().toString(),
                        fileName = file.name,
                        size = file.size,
                        direction = TransferDirection.SEND,
                        deviceName = target,
                        status = TransferStatus.COMPLETED,
                        timestamp = now,
                        uri = file.uri.toString(),
                    )
                    _history.value = historyRepository.add(item)
                }
                _transfer.value = TransferUiState.Completed(
                    direction = TransferDirection.SEND,
                    fileCount = files.size,
                    totalBytes = result.bytesSent,
                )
                _files.value = emptyList()
                emit(UiEvent.TransferCompleted)
                } else {
                    val message = result.message ?: "No se pudo completar el envío"
                    _transfer.value = TransferUiState.Failed(message)
                    emit(UiEvent.Message(message))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = error.message ?: "Error inesperado durante el envío"
                _transfer.value = TransferUiState.Failed(message)
                emit(UiEvent.Message(message))
            }
        }
    }

    private fun startServer() {
        if (serverRunning) return
        serverRunning = true
        transferServer.start(
            pinProvider = { _receive.value.pin.orEmpty() },
            deviceNameProvider = { _settings.value.deviceName },
            callbacks = FileTransferServer.Callbacks(
                onState = ::onServerState,
                onFile = { item ->
                    _history.value = historyRepository.add(item)
                    _receive.update {
                        it.copy(receivedFiles = listOf(item) + it.receivedFiles)
                    }
                },
                onBadPin = { attempts ->
                    _receive.update { it.copy(badPinAttempts = attempts) }
                    if (attempts >= 3) regeneratePin()
                },
                onSessionEnd = {
                    _receive.update { it.copy(peerName = null, waiting = true) }
                    _connection.update { it.copy(connectedPeerName = null) }
                },
            ),
        )
    }

    private fun onServerState(state: FileTransferServer.State) {
        if (cancelRequested && (state is FileTransferServer.State.Failed ||
                state is FileTransferServer.State.Waiting)
        ) {
            if (state is FileTransferServer.State.Waiting) {
                _receive.update { it.copy(waiting = true, peerName = null) }
                _connection.update { it.copy(connectedPeerName = null) }
                cancelRequested = false
            }
            return
        }
        when (state) {
            FileTransferServer.State.Idle -> Unit
            is FileTransferServer.State.Waiting -> {
                _receive.update { it.copy(waiting = true, peerName = null) }
                _connection.update { it.copy(connectedPeerName = null) }
                if (_transfer.value !is TransferUiState.Completed &&
                    _transfer.value !is TransferUiState.Failed &&
                    _transfer.value !is TransferUiState.Cancelled
                ) {
                    _transfer.value = TransferUiState.Idle
                }
            }

            is FileTransferServer.State.Connected -> {
                _receive.update { it.copy(waiting = false, peerName = state.peerName) }
                _connection.update { it.copy(connectedPeerName = state.peerName) }
                _transfer.value = TransferUiState.Connecting("Conectado con ${state.peerName}")
            }

            is FileTransferServer.State.Transferring -> {
                _transfer.value = TransferUiState.Receiving(state.progress)
            }

            is FileTransferServer.State.Completed -> {
                _receive.update { it.copy(waiting = true) }
                _transfer.value = TransferUiState.Completed(
                    direction = TransferDirection.RECEIVE,
                    fileCount = state.fileCount,
                    totalBytes = state.totalBytes,
                )
                emit(UiEvent.TransferCompleted)
            }

            is FileTransferServer.State.Failed -> {
                _receive.update { it.copy(waiting = true) }
                _transfer.value = TransferUiState.Failed(state.message)
            }
        }
    }

    private fun onWifiEvent(event: WifiDirectManager.Event) {
        when (event) {
            WifiDirectManager.Event.Unsupported -> {
                _connection.update {
                    it.copy(
                        supported = false,
                        message = "Este móvil no admite Wi-Fi Direct",
                    )
                }
            }

            is WifiDirectManager.Event.EnabledChanged -> {
                _connection.update { it.copy(wifiP2pEnabled = event.enabled) }
                if (!event.enabled) {
                    _scanning.value = false
                    _connection.update {
                        it.copy(message = "Activa Wi-Fi para buscar dispositivos")
                    }
                }
            }

            is WifiDirectManager.Event.PeersChanged -> {
                _peers.value = event.peers
                _scanning.value = false
                val selected = _selectedPeer.value
                if (selected != null &&
                    event.peers.none { it.address == selected.address }
                ) {
                    _selectedPeer.value = null
                }
                val target = nfcTargetName
                if (target != null) {
                    val match = event.peers.firstOrNull {
                        namesMatch(it.displayName, target)
                    }
                    if (match != null) {
                        selectPeer(match)
                        nfcTargetName = null
                    }
                }
            }

            is WifiDirectManager.Event.Connected -> handleConnected(event.info)
            WifiDirectManager.Event.Disconnected -> handleDisconnected()
            is WifiDirectManager.Event.GroupCreated -> {
                currentGroupSsid = event.ssid
                updateNfcPairing()
            }

            is WifiDirectManager.Event.Error -> {
                _scanning.value = false
                _connection.update { it.copy(message = event.message) }
                if (_transfer.value is TransferUiState.Connecting) {
                    _transfer.value = TransferUiState.Failed(event.message)
                }
                emit(UiEvent.Message(event.message))
            }
        }
    }

    private fun handleConnected(info: android.net.wifi.p2p.WifiP2pInfo) {
        if (!info.groupFormed) return
        if (info.isGroupOwner) {
            if (role == Role.RECEIVER) {
                _connection.update { it.copy(groupReady = true, message = null) }
                startServer()
            } else if (role == Role.SENDER) {
                val message = "El otro móvil debe activar Recibir primero"
                _connection.update { it.copy(message = message) }
                _transfer.value = TransferUiState.Failed(message)
                emit(UiEvent.Message(message))
                wifiDirect.removeGroup()
            }
            return
        }

        if (role == Role.SENDER) {
            val host = info.groupOwnerAddress
            if (host == null) {
                _transfer.value = TransferUiState.Failed("No se pudo obtener la IP del receptor")
                return
            }
            groupOwnerAddress = host
            connectedPeerAddress = requestedPeerAddress ?: _selectedPeer.value?.address
            _connection.update { it.copy(groupReady = true, message = null) }
            val pin = pendingSendPin
            val filesSnapshot = _files.value
            if (pin != null && filesSnapshot.isNotEmpty() &&
                connectedPeerAddress == _selectedPeer.value?.address
            ) {
                pendingSendPin = null
                launchSend(host, pin, filesSnapshot)
            }
        } else if (role == Role.RECEIVER) {
            _receive.update { it.copy(waiting = false) }
            _transfer.value = TransferUiState.Failed(
                "No se pudo crear el grupo como receptor. Inténtalo de nuevo.",
            )
        }
    }

    private fun handleDisconnected() {
        _connection.update { it.copy(groupReady = false, connectedPeerName = null) }
        groupOwnerAddress = null
        connectedPeerAddress = null
        if (cancelRequested) {
            cancelRequested = false
            return
        }
        if (_transfer.value is TransferUiState.Sending ||
            _transfer.value is TransferUiState.Connecting
        ) {
            transferClient.cancel()
            _transfer.value = TransferUiState.Failed("Se perdió la conexión Wi-Fi Direct")
        }
        if (role == Role.RECEIVER && _receive.value.active) {
            _receive.update { it.copy(waiting = true, peerName = null) }
            _connection.update {
                it.copy(message = "Conexión perdida. Vuelve a activar Recibir.")
            }
        }
    }

    private fun resolveMetadata(uri: Uri): SelectedFile? {
        val resolver = context.contentResolver
        var name: String? = null
        var size = -1L
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }
        if (size < 0L) {
            size = runCatching {
                resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }.getOrDefault(-1L)
        }
        if (size < 0L) return null
        val resolvedName = name
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "archivo"
        val mime = resolver.getType(uri)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(resolvedName.substringAfterLast('.', ""))
            ?: "application/octet-stream"
        return SelectedFile(
            uri = uri,
            name = resolvedName.take(TransferProtocol.MAX_NAME_CHARS),
            size = size,
            mimeType = mime,
        )
    }

    private fun updateNfcPairing() {
        val pin = _receive.value.pin
        if (!_receive.value.active || pin == null) {
            NfcPairingStore.update(null)
            return
        }
        NfcPairingStore.update(
            NfcPairing(
                ssid = currentGroupSsid.orEmpty(),
                pin = pin,
                deviceName = _settings.value.deviceName,
            ),
        )
    }

    private fun namesMatch(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank() || a.length < 3 || b.length < 3) return false
        return a.contains(b, ignoreCase = true) || b.contains(a, ignoreCase = true)
    }

    private fun emit(event: UiEvent) {
        _events.tryEmit(event)
    }
}
