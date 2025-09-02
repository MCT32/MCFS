package xyz.mct32.mcfs.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import xyz.mct32.mcfs.blockToNibble
import xyz.mct32.mcfs.blockToString
import xyz.mct32.mcfs.blockToUByte
import xyz.mct32.mcfs.blockread.RandomBlockReadErrorHandler
import xyz.mct32.mcfs.fs.ChunkAddressingFormat
import xyz.mct32.mcfs.fs.FormatRegion2bpb
import xyz.mct32.mcfs.fs.Volume
import xyz.mct32.mcfs.nibbleToBlock
import xyz.mct32.mcfs.stringToBlock
import xyz.mct32.mcfs.uByteToBlock

@OptIn(ExperimentalUnsignedTypes::class)
fun createCommand(): LiteralCommandNode<CommandSourceStack> {
    // TODO: Clean this up
    return Commands.literal("mcfs")
        .executes {
            context -> context.source.sender.sendMessage("Hello from MCFS!")
                Command.SINGLE_SUCCESS
        }
        .then(nibbleCommand())
        .then(byteCommand())
        .then(stringCommand())
        .then(volumeCommand())
        .then(fatCommand())
        .build()
}