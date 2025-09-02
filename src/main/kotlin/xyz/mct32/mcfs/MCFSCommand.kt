package xyz.mct32.mcfs

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import org.bukkit.Material
import xyz.mct32.mcfs.blockread.RandomBlockReadErrorHandler
import xyz.mct32.mcfs.fs.ChunkAddressingFormat
import xyz.mct32.mcfs.fs.FormatRegion2bpb
import xyz.mct32.mcfs.fs.Volume

@OptIn(ExperimentalUnsignedTypes::class)
fun createCommand(): LiteralCommandNode<CommandSourceStack> {
    // TODO: Clean this up
    return Commands.literal("mcfs")
        .executes {
            context -> context.source.sender.sendMessage("Hello from MCFS!")
                Command.SINGLE_SUCCESS
        }
        .then(Commands.literal("nibble")
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
        )
        .then(Commands.literal("byte")
            .then(Commands.literal("get")
                .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                    .executes {
                        // TODO: Properly handle errors
                            context ->

                        val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                        val blockPos = blockPosSelector.resolve(context.source)

                        context.source.sender.sendMessage(
                            ubyteArrayOf(
                                blockToUByte(
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
                    .then(Commands.argument("byte", IntegerArgumentType.integer(0, 255))
                        .executes {
                                context ->

                            val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                            val blockPos = blockPosSelector.resolve(context.source)

                            val byte = context.getArgument("byte", Int::class.java).toUByte()

                            val block = context.source.location.world.getBlockAt(
                                blockPos.x().toInt(),
                                blockPos.y().toInt(),
                                blockPos.z().toInt()
                            )

                            uByteToBlock(block, byte).getOrThrow()

                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
        )
        .then(Commands.literal("string")
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
        )
        .then(Commands.literal("volume")
            .then(Commands.literal("create")
                .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                    .then(Commands.argument("cluster_count", LongArgumentType.longArg(1, 4294967294))
                        .executes {
                            context ->

                            val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                            val blockPos = blockPosSelector.resolve(context.source)
                            val chunk = context.source.location.world.getChunkAt(
                                blockPos.x().toInt().floorDiv(16),
                                blockPos.z().toInt().floorDiv(16)
                            )

                            val yLevelOffset = (blockPos.y().toInt() - chunk.world.minHeight).toUByte()

                            val clusters = context.getArgument("cluster_count", Long::class.java).toUInt()

                            val formatRegion = FormatRegion2bpb(
                                chunkAddressingFormat = ChunkAddressingFormat.ZCURVE,
                                chunkPosX = chunk.x,
                                chunkPosZ = chunk.z,
                                yLevel = yLevelOffset,
                                clusterCount = clusters
                            )

                            Volume(formatRegion, context.source.location.world).toChunk(chunk, blockPos.y().toInt())

                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .then(Commands.literal("read")
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
                        println(volume.format)

                        Command.SINGLE_SUCCESS
                    }
                )
            )
            .then(Commands.literal("get_fat")
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
            .then(Commands.literal("set_fat")
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
        )
        .build()
}