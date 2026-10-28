package com.hacklesgaming.spookylandsrevived;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


/**
 * Runs once per chunk, after trees and vanilla pumpkin patches are placed.
 * - Each tree gets a configurable chance of 1-2 cobwebs nearby.
 * - Each naturally generated pumpkin gets a configurable chance of a cobweb beside it.
 */
public class TreeCobwebFeature extends Feature<NoneFeatureConfiguration> {

    public TreeCobwebFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        double treeChance = Config.TREE_COBWEBS_ENABLED.get() ? Config.TREE_COBWEB_CHANCE.get() : 0.0;
        double pumpkinChance = Config.PUMPKIN_COBWEBS_ENABLED.get() ? Config.PUMPKIN_COBWEB_CHANCE.get() : 0.0;
        if (treeChance <= 0.0 && pumpkinChance <= 0.0) {
            return false;
        }

        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        // Chunk bounds from the origin (avoids ChunkPos, whose constructor changed in 26.x)
        int minX = ctx.origin().getX() & ~15;
        int minZ = ctx.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placedAny = false;

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                // Highest solid non-leaf block in this column
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                pos.set(x, top - 1, z);
                BlockState surface = level.getBlockState(pos);

                // ---- Pumpkins (vanilla pumpkin patches sit on the surface) ----
                if (surface.is(Blocks.PUMPKIN)) {
                    if (pumpkinChance > 0.0 && random.nextDouble() < pumpkinChance) {
                        placedAny |= placeWebBesidePumpkin(level, pos.immutable(), random);
                    }
                    continue;
                }

                // ---- Trees ----
                if (treeChance <= 0.0 || !surface.is(BlockTags.LOGS)) {
                    continue;
                }

                // Walk down to the bottom log of the trunk
                while (pos.getY() > level.getMinY() && level.getBlockState(pos.below()).is(BlockTags.LOGS)) {
                    pos.move(Direction.DOWN);
                }

                // Must stand on ground (filters out branches)
                if (!isGround(level.getBlockState(pos.below()))) {
                    continue;
                }

                // Count 2x2 trees (dark oak, giant spruce, jungle) only once
                if (level.getBlockState(pos.west()).is(BlockTags.LOGS)
                        || level.getBlockState(pos.north()).is(BlockTags.LOGS)) {
                    continue;
                }

                if (random.nextDouble() < treeChance) {
                    placedAny |= placeWebsOnTree(level, pos.immutable(), top, random, 1 + random.nextInt(2));
                }
            }
        }
        return placedAny;
    }

    private static boolean isGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.MANGROVE_ROOTS)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    /** One cobweb in an open spot touching the pumpkin's side, resting on solid ground. */
    private static boolean placeWebBesidePumpkin(WorldGenLevel level, BlockPos pumpkin, RandomSource random) {
        Direction[] sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int start = random.nextInt(4); // random starting side so webs aren't always north
        for (int i = 0; i < 4; i++) {
            BlockPos p = pumpkin.relative(sides[(start + i) % 4]);
            if (level.isEmptyBlock(p)
                    && level.getFluidState(p).isEmpty()
                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                level.setBlock(p, Blocks.COBWEB.defaultBlockState(), 2);
                return true;
            }
        }
        return false;
    }

    /** Tries random spots around the tree; webs go under leaves or against the trunk. */
    private static boolean placeWebsOnTree(WorldGenLevel level, BlockPos base, int topY, RandomSource random, int count) {
        int placed = 0;
        int height = Math.max(1, topY - base.getY());

        for (int attempt = 0; attempt < 24 && placed < count; attempt++) {
            BlockPos p = base.offset(
                    random.nextInt(7) - 3,
                    1 + random.nextInt(height + 2),
                    random.nextInt(7) - 3);

            if (!level.isEmptyBlock(p)) {
                continue;
            }

            boolean underLeaves = level.getBlockState(p.above()).is(BlockTags.LEAVES);
            boolean besideLog = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(p.relative(d)).is(BlockTags.LOGS)) {
                    besideLog = true;
                    break;
                }
            }

            if (underLeaves || besideLog) {
                level.setBlock(p, Blocks.COBWEB.defaultBlockState(), 2);
                placed++;
            }
        }
        return placed > 0;
    }
}
