package xyz.mct32.mcfs

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
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
        .then(Commands.literal("test")
            .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                .executes {
                    context ->

                    val blockPosSelector = context.getArgument<BlockPositionResolver>("pos", BlockPositionResolver::class.java)
                    val blockPos = blockPosSelector.resolve(context.source)
                    val chunk = context.source.location.world.getChunkAt(
                        blockPos.x().toInt().floorDiv(16),
                        blockPos.z().toInt().floorDiv(16)
                    )

                    val yLevelOffset = (blockPos.y().toInt() - chunk.world.minHeight).toUByte()
                    
                    val formatRegion = FormatRegion2bpb(
                        chunkAddressingFormat = ChunkAddressingFormat.ZCURVE,
                        chunkPosX = chunk.x,
                        chunkPosZ = chunk.z,
                        yLevel = yLevelOffset,
                        clusterCount = 1u
                    )

                    writeDataToChunk(chunk, blockPos.y().toInt(), formatRegion.toUByteArray())

                    Command.SINGLE_SUCCESS
                }
            )
        )
        .build()
}