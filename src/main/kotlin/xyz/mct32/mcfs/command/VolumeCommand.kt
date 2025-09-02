package xyz.mct32.mcfs.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import xyz.mct32.mcfs.fs.ChunkAddressingFormat
import xyz.mct32.mcfs.fs.FormatRegion2bpb
import xyz.mct32.mcfs.fs.Volume

fun volumeCommand(): LiteralArgumentBuilder<CommandSourceStack> {
    return Commands.literal("volume")
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
}