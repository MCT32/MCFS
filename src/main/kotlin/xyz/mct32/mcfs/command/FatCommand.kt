package xyz.mct32.mcfs.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import xyz.mct32.mcfs.fs.Volume

fun fatCommand(): LiteralArgumentBuilder<CommandSourceStack> {
    return Commands.literal("fat")
        .then(Commands.literal("get")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .then(Commands.argument("index", LongArgumentType.longArg(0, 4294967294))
                    .executes {
                            context ->

                        val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                        val blockPos = blockPosSelector.resolve(context.source)
                        val chunk = context.source.location.world.getChunkAt(
                            blockPos.x().toInt().floorDiv(16),
                            blockPos.z().toInt().floorDiv(16)
                        )

                        val volume = Volume.fromChunk(chunk, blockPos.y().toInt())

                        val index = context.getArgument("index", Long::class.java).toUInt()

                        if (index >= volume.format.clusterCount) {
                            context.source.sender.sendMessage("Index too large")
                            return@executes Command.SINGLE_SUCCESS
                        }

                        val value = volume.getFatEntry(index)
                        context.source.sender.sendMessage("${value.getOrThrow()}")

                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        .then(Commands.literal("set")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .then(Commands.argument("index", LongArgumentType.longArg(0, 4294967294))
                    .then(Commands.argument("value", LongArgumentType.longArg(0, 4294967294))
                        .executes {
                                context ->

                            val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                            val blockPos = blockPosSelector.resolve(context.source)
                            val chunk = context.source.location.world.getChunkAt(
                                blockPos.x().toInt().floorDiv(16),
                                blockPos.z().toInt().floorDiv(16)
                            )

                            val volume = Volume.fromChunk(chunk, blockPos.y().toInt())

                            val index = context.getArgument("index", Long::class.java).toUInt()
                            val value = context.getArgument("value", Long::class.java).toUInt()

                            if (index >= volume.format.clusterCount) {
                                context.source.sender.sendMessage("Index too large")
                                return@executes Command.SINGLE_SUCCESS
                            }

                            volume.setFatEntry(index, value)

                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
        )
}