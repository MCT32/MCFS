package xyz.mct32.mcfs.fs

import xyz.mct32.mcfs.to48BitUByteArray
import xyz.mct32.mcfs.toUByteArray

data class DirectoryFlags (
    val directory: Boolean,
) {
    fun toUByte(): UByte {
        var buffer: UByte = 0u

        if (directory) buffer = buffer or 0b10000000u.toUByte()

        return buffer
    }
}

data class DirectoryTable (
    val name: String,
    val creation: ULong,
    val modification: ULong,
    val access: ULong,
    val startCluster: UInt,
    val fileSize: ULong,   // 48-bit
    val flags: DirectoryFlags
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    fun toUByteArray(): UByteArray {
        val stringBytes = name.toByteArray().asUByteArray()

        assert(stringBytes.size <= 32)

        var buffer = UByteArray(0)

        buffer += stringBytes
        buffer += creation.toUByteArray()
        buffer += modification.toUByteArray()
        buffer += access.toUByteArray()
        buffer += startCluster.toUByteArray()
        buffer += fileSize.to48BitUByteArray()

        return buffer
    }
}
