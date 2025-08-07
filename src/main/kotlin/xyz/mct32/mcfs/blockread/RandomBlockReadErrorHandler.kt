package xyz.mct32.mcfs.blockread

import org.bukkit.block.Block
import kotlin.random.Random

object RandomBlockReadErrorHandler : BlockReadErrorHandler {
    override fun handleReadError(block: Block): Result<UByte> {
        return Result.success(Random.nextInt().toUByte())
    }
}