package xyz.mct32.mcfs.fs

import org.bukkit.Chunk
import org.checkerframework.common.returnsreceiver.qual.This
import xyz.mct32.mcfs.blockread.FailureBlockReadErrorHandler
import xyz.mct32.mcfs.readDataFromChunk
import xyz.mct32.mcfs.toUByteArray
import xyz.mct32.mcfs.writeDataToChunk
import kotlin.toUByte

enum class ChunkAddressingFormat {
    ZCURVE;

    companion object {
        fun fromUByte(input: UByte): ChunkAddressingFormat {
            return when (input) {
                0x01u.toUByte() -> ChunkAddressingFormat.ZCURVE
                else            -> throw Exception("Invalid addressing format")
            }
        }
    }

    fun toUByte(): UByte {
        return when (this) {
            ChunkAddressingFormat.ZCURVE    -> 0x01u
        }
    }

    fun convertAddressToChunkOffset(address: UInt): Pair<UInt, UInt> {
        // Mask bits
        var x = address and 0x55555555u
        var y = (address shl 1) and 0x55555555u

        // Reverse interleaving
        x = (x or (x shr 1))  and 0x33333333u
        x = (x or (x shr 2))  and 0x0F0F0F0Fu
        x = (x or (x shr 4))  and 0x00FF00FFu
        x = (x or (x shr 8))  and 0x0000FFFFu

        y = (y or (y shr 1))  and 0x33333333u
        y = (y or (y shr 2))  and 0x0F0F0F0Fu
        y = (y or (y shr 4))  and 0x00FF00FFu
        y = (y or (y shr 8))  and 0x0000FFFFu

        return Pair(x, y shr 1)
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
fun expand24BitSignedTo32BitSigned(bytes: UByteArray): Int {
    require(bytes.size == 3) { "UByteArray must have exactly 3 elements" }

    // Combine into a 24-bit number
    val combined =
        (bytes[0].toInt() shl 16) or
                (bytes[1].toInt() shl 8) or
                (bytes[2].toInt())

    // Sign-extend from 24-bit to 32-bit
    return if ((combined and 0x800000) != 0) {
        combined or -0x1000000 // Fill the top 8 bits with 1s
    } else {
        combined
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun uIntFromUByteArray(array: UByteArray): UInt {
    return (array[0].toUInt() shl 24) + (array[1].toUInt() shl 16) + (array[2].toUInt() shl 8) + array[3].toUInt()
}

data class FormatRegion2bpb(
    val chunkAddressingFormat: ChunkAddressingFormat,
    val chunkPosX: Int,
    val chunkPosZ: Int,
    val yLevel: UByte,
    val clusterCount: UInt
) {
    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun fromUByteArray(array: UByteArray): FormatRegion2bpb {
            return FormatRegion2bpb(
                ChunkAddressingFormat.fromUByte(array[4]),
                expand24BitSignedTo32BitSigned(ubyteArrayOf(array[5], array[6], array[7])),
                expand24BitSignedTo32BitSigned(ubyteArrayOf(array[8], array[9], array[10])),
                array[11],
                uIntFromUByteArray(ubyteArrayOf(array[12], array[13], array[14], array[15]))
            )
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        fun fromChunk(chunk: Chunk, yLevel: Int): FormatRegion2bpb {
            return FormatRegion2bpb.fromUByteArray(readDataFromChunk(chunk, yLevel, 16u))
        }
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

    @OptIn(ExperimentalUnsignedTypes::class)
    fun toChunk(chunk: Chunk, yLevel: Int) {
        writeDataToChunk(chunk, yLevel, this.toUByteArray())
    }
}
