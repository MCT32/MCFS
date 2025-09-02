package xyz.mct32.mcfs.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import xyz.mct32.mcfs.blockToString
import xyz.mct32.mcfs.blockread.RandomBlockReadErrorHandler
import xyz.mct32.mcfs.stringToBlock

fun stringCommand(): LiteralArgumentBuilder<CommandSourceStack> {
    return Commands.literal("string")
        .then(Commands.literal("get")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .executes {
                    // TODO: Properly handle errors
                        context ->

                    val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                    val blockPos = blockPosSelector.resolve(context.source)

                    context.source.sender.sendMessage(
                        blockToString(
                            context.source.location.world.getBlockAt(
                                blockPos.x().toInt(),
                                blockPos.y().toInt(),
                                blockPos.z().toInt()
                            ),
                            RandomBlockReadErrorHandler
                        ).getOrThrow()
                    )

                    Command.SINGLE_SUCCESS
                }
            )
        )
        .then(Commands.literal("set")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                // TODO: Create nibble argument type
                .then(Commands.argument("string", StringArgumentType.string())
                    .executes {
                            context ->

                        val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                        val blockPos = blockPosSelector.resolve(context.source)

                        val string = context.getArgument("string", String::class.java)

                        val block = context.source.location.world.getBlockAt(
                            blockPos.x().toInt(),
                            blockPos.y().toInt(),
                            blockPos.z().toInt()
                        )

                        stringToBlock(block, string).getOrThrow()

                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
}