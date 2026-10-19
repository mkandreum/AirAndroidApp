package com.xyon.airandroid.core.net

object TransferProtocol {
    const val VERSION = 1
    const val PORT = 8988
    const val P2P_PREFIX = "192.168.49."
    const val CHUNK_BYTES = 64 * 1024
    const val MAX_FILE_BYTES = 8L * 1024 * 1024 * 1024
    const val MAX_FILES = 100
    const val MAX_NAME_CHARS = 180
    const val MIN_FREE_BYTES = 100L * 1024 * 1024

    const val TYPE_AUTH = "auth"
    const val TYPE_READY = "ready"
    const val TYPE_FILE = "file"
    const val TYPE_DONE = "done"
    const val TYPE_OK = "ok"
    const val TYPE_ERROR = "error"
}
