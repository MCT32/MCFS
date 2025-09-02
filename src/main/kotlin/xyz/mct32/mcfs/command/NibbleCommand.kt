package xyz.mct32.mcfs.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import xyz.mct32.mcfs.blockToNibble
import xyz.mct32.mcfs.blockread.RandomBlockReadErrorHandler
import xyz.mct32.mcfs.nibbleToBlock

fun nibbleCommand(): LiteralArgumentBuilder<CommandSourceStack> {
    return Commands.literal("nibble")
        .then(Commands.literal("get")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .executes {
                    // TODO: Properly handle errors
                        context ->

                    val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                    val blockPos = blockPosSelector.resolve(context.source)

                    context.source.sender.sendMessage(
                        ubyteArrayOf(
                            blockToNibble(
                                context.source.location.world.getBlockAt(
                                    blockPos.x().toInt(),
                                    blockPos.y().toInt(),
                                    blockPos.z().toInt()
                                ),
                                RandomBlockReadErrorHandler
                            ).getOrThrow()
                        ).toHexString()
                    )

                    Command.SINGLE_SUCCESS
                }
            )
        )
        .then(Commands.literal("set")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                // TODO: Create nibble argument type
                .then(Commands.argument("nibble", IntegerArgumentType.integer(0, 15))
                    .executes {
                            context ->

                        val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                        val blockPos = blockPosSelector.resolve(context.source)

                        val nibble = context.getArgument("nibble", Int::class.java).toUByte()

                        var block = context.source.location.world.getBlockAt(
                            blockPos.x().toInt(),
                            blockPos.y().toInt(),
                            blockPos.z().toInt()
                        )

                        nibbleToBlock(block, nibble).getOrThrow()

                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
}