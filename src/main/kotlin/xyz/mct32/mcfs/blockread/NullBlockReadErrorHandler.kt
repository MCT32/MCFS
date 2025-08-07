package xyz.mct32.mcfs.blockread

import org.bukkit.block.Block

object NullBlockReadErrorHandler : BlockReadErrorHandler {
    override fun handleReadError(block: Block): Result<UByte> {
        return Result.success(0b0u)
    }
}