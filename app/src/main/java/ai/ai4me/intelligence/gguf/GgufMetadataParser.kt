package ai.ai4me.intelligence.gguf

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GgufHeaderInfo(
    val isValidGguf: Boolean,
    val version: Int,
    val tensorCount: Long,
    val metadataKvCount: Long,
    val architecture: String? = null,
    val contextLength: Int? = null,
    val chatTemplate: String? = null,
    val validationMessage: String = "OK"
)

/**
 * Binary GGUF Header Parser (Document 2 §3.6)
 * Validates GGUF magic bytes (0x46554747) and parses architecture, context,
 * and tokenizer metadata to ensure genuine model compatibility before loading.
 */
object GgufMetadataParser {

    private const val GGUF_MAGIC = 0x46554747 // "GGUF" in little-endian

    // GGUF Metadata Value Types
    private const val GGUF_TYPE_UINT8 = 0
    private const val GGUF_TYPE_INT8 = 1
    private const val GGUF_TYPE_UINT16 = 2
    private const val GGUF_TYPE_INT16 = 3
    private const val GGUF_TYPE_UINT32 = 4
    private const val GGUF_TYPE_INT32 = 5
    private const val GGUF_TYPE_FLOAT32 = 6
    private const val GGUF_TYPE_BOOL = 7
    private const val GGUF_TYPE_STRING = 8
    private const val GGUF_TYPE_ARRAY = 9
    private const val GGUF_TYPE_UINT64 = 10
    private const val GGUF_TYPE_INT64 = 11
    private const val GGUF_TYPE_FLOAT64 = 12

    fun parse(file: File): GgufHeaderInfo {
        if (!file.exists() || file.length() < 24) {
            return GgufHeaderInfo(
                isValidGguf = false,
                version = 0,
                tensorCount = 0,
                metadataKvCount = 0,
                validationMessage = "File does not exist or is too small to contain a valid GGUF header."
            )
        }

        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(24)
                raf.readFully(headerBytes)
                val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

                val magic = buffer.int
                if (magic != GGUF_MAGIC) {
                    return GgufHeaderInfo(
                        isValidGguf = false,
                        version = 0,
                        tensorCount = 0,
                        metadataKvCount = 0,
                        validationMessage = "Invalid magic bytes: 0x${Integer.toHexString(magic).uppercase()} (Expected 'GGUF')"
                    )
                }

                val version = buffer.int
                if (version < 1 || version > 3) {
                    return GgufHeaderInfo(
                        isValidGguf = false,
                        version = version,
                        tensorCount = 0,
                        metadataKvCount = 0,
                        validationMessage = "Unsupported GGUF version: $version"
                    )
                }

                val tensorCount = buffer.long
                val metadataKvCount = buffer.long

                var architecture: String? = null
                var contextLength: Int? = null
                var chatTemplate: String? = null

                // Parse key-value pairs up to first 100 metadata items or EOF
                val maxKvToRead = minOf(metadataKvCount, 120L)
                for (i in 0 until maxKvToRead) {
                    if (raf.filePointer >= raf.length() - 8) break

                    val key = readGgufString(raf) ?: break
                    val valueType = readInt32(raf) ?: break

                    when {
                        key == "general.architecture" && valueType == GGUF_TYPE_STRING -> {
                            architecture = readGgufString(raf)
                        }
                        key.endsWith(".context_length") && (valueType == GGUF_TYPE_UINT32 || valueType == GGUF_TYPE_INT32) -> {
                            contextLength = readInt32(raf)
                        }
                        key == "tokenizer.chat_template" && valueType == GGUF_TYPE_STRING -> {
                            chatTemplate = readGgufString(raf)
                        }
                        else -> {
                            // Skip value
                            skipValue(raf, valueType)
                        }
                    }
                }

                return GgufHeaderInfo(
                    isValidGguf = true,
                    version = version,
                    tensorCount = tensorCount,
                    metadataKvCount = metadataKvCount,
                    architecture = architecture,
                    contextLength = contextLength,
                    chatTemplate = chatTemplate,
                    validationMessage = "Valid GGUF (v$version, arch: ${architecture ?: "unknown"}, tensors: $tensorCount)"
                )
            }
        } catch (e: Exception) {
            return GgufHeaderInfo(
                isValidGguf = false,
                version = 0,
                tensorCount = 0,
                metadataKvCount = 0,
                validationMessage = "Header parse error: ${e.message}"
            )
        }
    }

    private fun readGgufString(raf: RandomAccessFile): String? {
        val len = readInt64(raf) ?: return null
        if (len < 0 || len > 1024 * 64) return null // Guard against runaway strings
        val bytes = ByteArray(len.toInt())
        raf.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readInt32(raf: RandomAccessFile): Int? {
        val bytes = ByteArray(4)
        if (raf.read(bytes) != 4) return null
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readInt64(raf: RandomAccessFile): Long? {
        val bytes = ByteArray(8)
        if (raf.read(bytes) != 8) return null
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun skipValue(raf: RandomAccessFile, type: Int) {
        when (type) {
            GGUF_TYPE_UINT8, GGUF_TYPE_INT8, GGUF_TYPE_BOOL -> raf.skipBytes(1)
            GGUF_TYPE_UINT16, GGUF_TYPE_INT16 -> raf.skipBytes(2)
            GGUF_TYPE_UINT32, GGUF_TYPE_INT32, GGUF_TYPE_FLOAT32 -> raf.skipBytes(4)
            GGUF_TYPE_UINT64, GGUF_TYPE_INT64, GGUF_TYPE_FLOAT64 -> raf.skipBytes(8)
            GGUF_TYPE_STRING -> {
                val len = readInt64(raf) ?: 0L
                if (len > 0) raf.skipBytes(len.toInt())
            }
            GGUF_TYPE_ARRAY -> {
                val elemType = readInt32(raf) ?: return
                val count = readInt64(raf) ?: return
                val safeCount = minOf(count, 5000L)
                for (j in 0 until safeCount) {
                    skipValue(raf, elemType)
                }
            }
        }
    }
}
