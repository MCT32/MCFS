package xyz.mct32.mcfs.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

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