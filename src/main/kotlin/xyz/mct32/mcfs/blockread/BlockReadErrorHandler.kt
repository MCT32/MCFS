package xyz.mct32.mcfs.blockread

import org.bukkit.block.Block

// TODO: Extend to other block encoding formats
interface BlockReadErrorHandler {
    fun handleReadError(block: Block): Result<UByte>
}