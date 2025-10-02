package xyz.mct32.mcfs.fs

import org.bukkit.Chunk
import org.bukkit.World
import xyz.mct32.mcfs.blockread.NullBlockReadErrorHandler
import xyz.mct32.mcfs.clearDataInChunk
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
    fun getFatEntry(index: UInt): UInt? {
        val chunk = chunkAtFatEntry(index)

        // 4 bytes per entry
        val offset = index.rem(8192u) * 4u

        val array = readDataFromChunk(chunk, format.yLevel.toInt() + world.minHeight, 4u, offset,
            NullBlockReadErrorHandler)

        return uIntFromUByteArray(array)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun setFatEntry(index: UInt, value: UInt) {
        val chunk = chunkAtFatEntry(index)

        // 4 bytes per entry
        val offset = index.rem(8192u) * 4u

        writeDataToChunk(chunk, format.yLevel.toInt() + world.minHeight, value.toUByteArray(), offset)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    // Replace with air, looks better
    fun clearFatEntry(index: UInt) {
        val chunk = chunkAtFatEntry(index)

        // 4 bytes per entry
        val offset = index.rem(8192u) * 4u

        clearDataInChunk(chunk, format.yLevel.toInt() + world.minHeight, 4, offset)
    }

    fun nextAvailableFatEntry(start: UInt = 0u): UInt? {
        for (index in start..<this.format.clusterCount) {
            if (this.getFatEntry(index.toUInt())!! == 0u) {
                return index.toUInt()
            }
        }

        return null
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun readFatChain(start: UInt): UIntArray {
        var buffer = UIntArray(0)

        var index = start

        while (true) {
            buffer += index

            val newIndex = this.getFatEntry(index)!!

            if (newIndex == 0xffffffffu) {
                return buffer
            }

            if (buffer.contains(newIndex)) {
                throw Exception("Loop detected in FAT chain")
            }

            index = newIndex
        }
    }

    fun createFatChain(length: UInt, start: UInt = 0u): UInt {
        var created = 0u;
        var from = start;

        var next = this.nextAvailableFatEntry(from)!!;
        val actual_start = next;

        while (created < length) {
            if (created + 1u == length) {
                this.setFatEntry(next, 0xFFFFFFFFu)
            } else {
                val current = next;
                from = next;
                next = this.nextAvailableFatEntry(from + 1u)!!;
                this.setFatEntry(current, next);
            }

            created += 1u;
        }

        return actual_start;
    }

    fun deleteFatChain(start: UInt): UInt {
        var current = start;
        var deleted = 0u;

        while (true) {
            val next = this.getFatEntry(current)!!;
            println("current $current next $next");

            this.clearFatEntry(current);
            deleted++;

            if (next == 0x0u) {
                error("Found empty entry following chain");
            } else if (next == 0xffffffffu) {
                return deleted;
            }

            current = next;
        }
    }

    // TODO: Handle shrink and grow
}