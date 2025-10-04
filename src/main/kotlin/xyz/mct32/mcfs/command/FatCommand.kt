package xyz.mct32.mcfs.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import xyz.mct32.mcfs.fs.Volume

@OptIn(ExperimentalUnsignedTypes::class)
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
                        context.source.sender.sendMessage("${value!!}")

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
        .then(Commands.literal("next_available")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .executes {
                        context ->

                    val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                    val blockPos = blockPosSelector.resolve(context.source)
                    val chunk = context.source.location.world.getChunkAt(
                        blockPos.x().toInt().floorDiv(16),
                        blockPos.z().toInt().floorDiv(16)
                    )

                    val volume = Volume.fromChunk(chunk, blockPos.y().toInt())

                    when (val index = volume.nextAvailableFatEntry()) {
                        null -> context.source.sender.sendMessage("All FAT entries exhausted")
                        else -> context.source.sender.sendMessage("$index")
                    }

                    Command.SINGLE_SUCCESS
                }
            )
        )
        .then(Commands.literal("read_chain")
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

                        val chain = volume.readFatChain(index)

                        context.source.sender.sendMessage("$chain")

                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        .then(Commands.literal("create_chain")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .then(Commands.argument("length", LongArgumentType.longArg(1, 4294967294))
                    .executes {
                            context ->

                        val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                        val blockPos = blockPosSelector.resolve(context.source)
                        val chunk = context.source.location.world.getChunkAt(
                            blockPos.x().toInt().floorDiv(16),
                            blockPos.z().toInt().floorDiv(16)
                        )

                        val volume = Volume.fromChunk(chunk, blockPos.y().toInt())

                        val length = context.getArgument("length", Long::class.java).toUInt()

                        val start = volume.createFatChain(length)

                        context.source.sender.sendMessage("Created chain at $start")

                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        .then(Commands.literal("extend_chain")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .then(Commands.argument("start", LongArgumentType.longArg(0, 4294967294))
                    .then(Commands.argument("length", LongArgumentType.longArg(1, 4294967294))
                        .executes {
                                context ->

                            val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                            val blockPos = blockPosSelector.resolve(context.source)
                            val chunk = context.source.location.world.getChunkAt(
                                blockPos.x().toInt().floorDiv(16),
                                blockPos.z().toInt().floorDiv(16)
                            )

                            val volume = Volume.fromChunk(chunk, blockPos.y().toInt())

                            val length = context.getArgument("length", Long::class.java).toUInt();
                            val start = context.getArgument("start", Long::class.java).toUInt();

                            volume.extendFatChain(length, start);

                            context.source.sender.sendMessage("Chain extended")

                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
        )
        .then(Commands.literal("delete_chain")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .then(Commands.argument("start", LongArgumentType.longArg(0, 4294967294))
                    .executes {
                            context ->

                        val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                        val blockPos = blockPosSelector.resolve(context.source)
                        val chunk = context.source.location.world.getChunkAt(
                            blockPos.x().toInt().floorDiv(16),
                            blockPos.z().toInt().floorDiv(16)
                        )

                        val volume = Volume.fromChunk(chunk, blockPos.y().toInt())

                        val start = context.getArgument("start", Long::class.java).toUInt()

                        val deleted = volume.deleteFatChain(start)

                        context.source.sender.sendMessage("Deleted chain of length $deleted")

                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        .then(Commands.literal("shrink_chain")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .then(Commands.argument("start", LongArgumentType.longArg(0, 4294967294))
                    .then(Commands.argument("length", LongArgumentType.longArg(1, 4294967294))
                        .executes {
                                context ->

                            val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                            val blockPos = blockPosSelector.resolve(context.source)
                            val chunk = context.source.location.world.getChunkAt(
                                blockPos.x().toInt().floorDiv(16),
                                blockPos.z().toInt().floorDiv(16)
                            )

                            val volume = Volume.fromChunk(chunk, blockPos.y().toInt())

                            val start = context.getArgument("start", Long::class.java).toUInt()
                            val length = context.getArgument("length", Long::class.java).toUInt()

                            volume.shrinkFatChain(start, length);

                            context.source.sender.sendMessage("Shrunk chain")

                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
        )
}