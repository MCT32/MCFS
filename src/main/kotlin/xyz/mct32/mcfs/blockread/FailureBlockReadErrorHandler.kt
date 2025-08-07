package xyz.mct32.mcfs.blockread

import org.bukkit.block.Block

object FailureBlockReadErrorHandler : BlockReadErrorHandler {
    override fun handleReadError(block: Block): Result<UByte> {
        return Result.failure(Exception("Cannot read block"))
    }
}