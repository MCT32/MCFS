package xyz.mct32.mcfs.fs

import kotlin.toUByte

enum class ChunkAddressingFormat {
    ZCURVE;

    fun toUByte(): UByte {
        return when (this) {
            ChunkAddressingFormat.ZCURVE    -> 0x01u
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun truncate32BitSignedTo24BitSigned(input: Int): UByteArray {
    val masked = input and 0xFFFFFF
    val i24Signed = if ((masked and 0x800000) != 0) {
        masked or -0x1000000
    } else {
        masked
    }

    return listOf<UByte>(
        ((i24Signed and 0xFF0000) shr 16).toUByte(),
        ((i24Signed and 0xFF00) shr 8).toUByte(),
        (i24Signed and 0xFF).toUByte(),
    ).toUByteArray()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun UInt.toUByteArray(): UByteArray {
    return listOf<UByte>(
        ((this and 0xFF000000u) shr 24).toUByte(),
        ((this and 0xFF0000u) shr 16).toUByte(),
        ((this and 0xFF00u) shr 8).toUByte(),
        (this and 0xFFu).toUByte(),
    ).toUByteArray()
}

data class FormatRegion2bpb(
    val chunkAddressingFormat: ChunkAddressingFormat,
    val chunkPosX: Int,
    val chunkPosZ: Int,
    val yLevel: UByte,
    val clusterCount: UInt
) {
    companion object {
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun toUByteArray(): UByteArray {
        var buffer = UByteArray(0)

        // Encoding format
        buffer += 0x00000003u.toUByteArray()

        buffer += chunkAddressingFormat.toUByte()
        buffer += truncate32BitSignedTo24BitSigned(chunkPosX)
        buffer += truncate32BitSignedTo24BitSigned(chunkPosZ)
        buffer += yLevel
        buffer += clusterCount.toUByteArray()

        return buffer
    }
}
