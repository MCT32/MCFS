package xyz.mct32.mcfs.fs

import org.bukkit.Chunk
import org.bukkit.World
import xyz.mct32.mcfs.blockread.NullBlockReadErrorHandler
import xyz.mct32.mcfs.readDataFromChunk
import xyz.mct32.mcfs.toUByteArray
import xyz.mct32.mcfs.writeDataToChunk

class Volume(val format: FormatRegion2bpb, val world: World) {
    companion object {
        fun fromChunk(chunk: Chunk, yLevel: Int): Volume {
            return Volume(FormatRegion2bpb.fromChunk(chunk, yLevel), chunk.world)
        }
    }

    fun toChunk(chunk: Chunk, yLevel: Int) {
        format.toChunk(chunk, yLevel)
    }

    fun chunkAtFatEntry(index: UInt): Chunk {
        // Divide by number of entries in each cluster
        // Add 1 to skip format region
        val clusterNumber = index.div(8192u) + 1u

        val offset = format.chunkAddressingFormat.convertAddressToChunkOffset(clusterNumber)

        return world.getChunkAt(format.chunkPosX + offset.first.toInt(), format.chunkPosZ + offset.second.toInt())
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun getFatEntry(index: UInt): Result<UInt> {
        val chunk = chunkAtFatEntry(index)

        // 4 bytes per entry
        val offset = index.rem(8192u) * 4u

        val array = readDataFromChunk(chunk, format.yLevel.toInt() + world.minHeight, 4u, offset,
            NullBlockReadErrorHandler)

        return Result.success(uIntFromUByteArray(array))
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun setFatEntry(index: UInt, value: UInt) {
        val chunk = chunkAtFatEntry(index)

        // 4 bytes per entry
        val offset = index.rem(8192u) * 4u

        writeDataToChunk(chunk, format.yLevel.toInt() + world.minHeight, value.toUByteArray(), offset)
    }
}