package xyz.mct32.mcfs

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.plugin.java.JavaPlugin
import xyz.mct32.mcfs.blockread.BlockReadErrorHandler
import xyz.mct32.mcfs.blockread.FailureBlockReadErrorHandler
import xyz.mct32.mcfs.command.createCommand

// TODO: Make all this block encoding/decoding into an interface to support different implementations
fun blockToNibble(block: Block, errorHandler: BlockReadErrorHandler): Result<UByte> {
    return when (block.type) {
        Material.WHITE_CONCRETE         -> Result.success(0b0000u)
        Material.LIGHT_GRAY_CONCRETE    -> Result.success(0b0001u)
        Material.GRAY_CONCRETE          -> Result.success(0b0010u)
        Material.BLACK_CONCRETE         -> Result.success(0b0011u)
        Material.BROWN_CONCRETE         -> Result.success(0b0100u)
        Material.RED_CONCRETE           -> Result.success(0b0101u)
        Material.ORANGE_CONCRETE        -> Result.success(0b0110u)
        Material.YELLOW_CONCRETE        -> Result.success(0b0111u)
        Material.LIME_CONCRETE          -> Result.success(0b1000u)
        Material.GREEN_CONCRETE         -> Result.success(0b1001u)
        Material.CYAN_CONCRETE          -> Result.success(0b1010u)
        Material.LIGHT_BLUE_CONCRETE    -> Result.success(0b1011u)
        Material.BLUE_CONCRETE          -> Result.success(0b1100u)
        Material.PURPLE_CONCRETE        -> Result.success(0b1101u)
        Material.MAGENTA_CONCRETE       -> Result.success(0b1110u)
        Material.PINK_CONCRETE          -> Result.success(0b1111u)
        else                            -> errorHandler.handleReadError(block)
    }
}

fun nibbleToBlock(block: Block, nibble: UByte): Result<Unit> {
    block.type = when (nibble) {
        0b0000.toUByte()    -> Material.WHITE_CONCRETE
        0b0001.toUByte()    -> Material.LIGHT_GRAY_CONCRETE
        0b0010.toUByte()    -> Material.GRAY_CONCRETE
        0b0011.toUByte()    -> Material.BLACK_CONCRETE
        0b0100.toUByte()    -> Material.BROWN_CONCRETE
        0b0101.toUByte()    -> Material.RED_CONCRETE
        0b0110.toUByte()    -> Material.ORANGE_CONCRETE
        0b0111.toUByte()    -> Material.YELLOW_CONCRETE
        0b1000.toUByte()    -> Material.LIME_CONCRETE
        0b1001.toUByte()    -> Material.GREEN_CONCRETE
        0b1010.toUByte()    -> Material.CYAN_CONCRETE
        0b1011.toUByte()    -> Material.LIGHT_BLUE_CONCRETE
        0b1100.toUByte()    -> Material.BLUE_CONCRETE
        0b1101.toUByte()    -> Material.PURPLE_CONCRETE
        0b1110.toUByte()    -> Material.MAGENTA_CONCRETE
        0b1111.toUByte()    -> Material.PINK_CONCRETE
        else                -> return Result.failure(Exception("Invalid nibble"))
    }

    return Result.success(Unit)
}

fun blockToUByte(block: Block, errorHandler: BlockReadErrorHandler): Result<UByte> {
    val msn = blockToNibble(block, errorHandler).getOrThrow()
    val lsn = blockToNibble(block.getRelative(1, 0, 0), errorHandler).getOrThrow()

    // Ugly
    val byte = (msn.toInt() shl 4).toUByte() or lsn

    return Result.success(byte)
}

fun uByteToBlock(block: Block, byte: UByte): Result<Unit> {
    val msn = ((byte and 0b11110000u).toInt() shr 4).toUByte()
    val lsn = byte and 0b00001111u

    nibbleToBlock(block, msn).getOrThrow()
    nibbleToBlock(block.getRelative(1, 0, 0), lsn).getOrThrow()

    return Result.success(Unit)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun blockToUByteArray(block: Block, errorHandler: BlockReadErrorHandler, length: UInt): Result<UByteArray> {
    var buffer = UByteArray(length.toInt())

    for (i in 0..<length.toInt()) {
        val block = block.getRelative(i * 2, 0, 0)

        buffer[i] = blockToUByte(block, errorHandler).getOrThrow()
    }

    return Result.success(buffer)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun uByteArrayToBlock(block: Block, array: UByteArray): Result<Unit> {
    for (i in 0..<array.size) {
        val block = block.getRelative(i * 2, 0, 0)

        uByteToBlock(block, array[i])
    }

    return Result.success(Unit)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun blockToString(block: Block, errorHandler: BlockReadErrorHandler): Result<String> {
    var buffer = UByteArray(0)
    var offset = 0

    while (true) {
        val byte = blockToUByte(block.getRelative(offset * 2, 0, 0), errorHandler).getOrThrow()
        if (byte == 0b0u.toUByte()) break

        buffer += byte
        offset++
    }

    return Result.success(buffer.toByteArray().toString(Charsets.UTF_8))
}

@OptIn(ExperimentalUnsignedTypes::class)
fun stringToBlock(block: Block, string: String): Result<Unit> {
    // Why not one function?
    val array = string.toByteArray().asUByteArray() + 0b0u

    uByteArrayToBlock(block, array).getOrThrow()

    return Result.success(Unit)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun writeDataToChunk(chunk: Chunk, yLevel: Int, data: UByteArray, offset: UInt = 0u) {
    for (i in 0..<data.size) {
        val iBlock = (i + offset.toInt()) shl 1

        val x = iBlock and 0xF
        val z = (iBlock and 0xF0) shr 4
        val y = (iBlock and 0xFF00) shr 8

        val block = chunk.getBlock(x, y + yLevel, z)

        uByteToBlock(block, data[i])
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun clearDataInChunk(chunk: Chunk, yLevel: Int, length: Int, offset: UInt = 0u) {
    for (i in 0..<length) {
        val iBlock = (i + offset.toInt()) shl 1

        val x = iBlock and 0xF
        val z = (iBlock and 0xF0) shr 4
        val y = (iBlock and 0xFF00) shr 8

        var block = chunk.getBlock(x, y + yLevel, z)
        block.setType(Material.AIR);

        // Second block
        block = chunk.getBlock(x + 1, y + yLevel, z)
        block.setType(Material.AIR);
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun readDataFromChunk(chunk: Chunk, yLevel: Int, size: UInt, offset: UInt = 0u, errorHandler: BlockReadErrorHandler = FailureBlockReadErrorHandler): UByteArray {
    var buffer = UByteArray(0)

    for (i in 0..<size.toInt()) {
        val iBlock = (i + offset.toInt()) shl 1

        val x = iBlock and 0xF
        val z = (iBlock and 0xF0) shr 4
        val y = (iBlock and 0xFF00) shr 8

        val block = chunk.getBlock(x, y + yLevel, z)

        buffer += blockToUByte(block, errorHandler).getOrThrow()
    }

    return buffer
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

@OptIn(ExperimentalUnsignedTypes::class)
fun ULong.toUByteArray(): UByteArray {
    return listOf<UByte>(
        ((this and 0xFF00000000000000u) shr 56).toUByte(),
        ((this and 0xFF000000000000u) shr 48).toUByte(),
        ((this and 0xFF0000000000u) shr 40).toUByte(),
        ((this and 0xFF00000000u) shr 32).toUByte(),
        ((this and 0xFF000000u) shr 24).toUByte(),
        ((this and 0xFF0000u) shr 16).toUByte(),
        ((this and 0xFF00u) shr 8).toUByte(),
        (this and 0xFFu).toUByte(),
    ).toUByteArray()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun ULong.to48BitUByteArray(): UByteArray {
    return listOf<UByte>(
        ((this and 0xFF0000000000u) shr 40).toUByte(),
        ((this and 0xFF00000000u) shr 32).toUByte(),
        ((this and 0xFF000000u) shr 24).toUByte(),
        ((this and 0xFF0000u) shr 16).toUByte(),
        ((this and 0xFF00u) shr 8).toUByte(),
        (this and 0xFFu).toUByte(),
    ).toUByteArray()
}

class MCFS : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, {
            commands -> commands.registrar().register(createCommand(), "MCFS utility command")
        })
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
