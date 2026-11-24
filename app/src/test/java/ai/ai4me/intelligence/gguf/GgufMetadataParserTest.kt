package ai.ai4me.intelligence.gguf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GgufMetadataParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testNonExistentFile_ReturnsInvalid() {
        val file = File(tempFolder.root, "non_existent.gguf")
        val info = GgufMetadataParser.parse(file)
        assertFalse(info.isValidGguf)
    }

    @Test
    fun testCorruptMagicBytes_Rejected() {
        val file = tempFolder.newFile("corrupt.gguf")
        FileOutputStream(file).use { fos ->
            fos.write("NOT_GGUF_MAGIC_BYTES_CORRUPTED_32B".toByteArray())
        }
        val info = GgufMetadataParser.parse(file)
        assertFalse(info.isValidGguf)
        assertTrue(info.validationMessage.contains("Invalid magic bytes"))
    }

    @Test
    fun testSyntheticValidGgufHeader_ParsedSuccessfully() {
        val file = tempFolder.newFile("valid_header.gguf")
        FileOutputStream(file).use { fos ->
            val buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(0x46554747) // Magic GGUF
            buffer.putInt(3)          // Version 3
            buffer.putLong(128L)      // 128 tensors
            buffer.putLong(0L)        // 0 metadata KV items
            fos.write(buffer.array())
        }

        val info = GgufMetadataParser.parse(file)
        assertTrue(info.isValidGguf)
        assertEquals(3, info.version)
        assertEquals(128L, info.tensorCount)
        assertEquals(0L, info.metadataKvCount)
    }
}
