package com.hacklesgaming.spookylandsrevived;

import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;
import com.hacklesgaming.spookylandsrevived.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Spawns flocks of wild ravens near players, on the surface or on treetops.
 *
 * Why not just vanilla spawning: ravens are AMBIENT mobs, the same group as bats. Vanilla picks a
 * random height anywhere from bedrock to the surface for ambient spawns, so it almost never lands on
 * the surface where ravens can spawn, and bats in caves fill the shared ambient cap anyway. On a
 * busy server that means almost no ravens, whatever the spawn weight.
 *
 * Uses the "ravens" config: spawnWeight = how often (0 = never), min/maxGroupSize = flock size.
 * These are read live, so changes apply without a restart.
 */
@EventBusSubscriber(modid = SpookyLandsRevived.MOD_ID)
public class RavenSpawner {
    public static final TagKey<Biome> SPAWNS_RAVENS = TagKey.create(Registries.BIOME,
            Identifier.parse(SpookyLandsRevived.MOD_ID + ":spawns_ravens"));

    /** How often (in ticks) each player gets a spawn roll. 200 ticks = 10 seconds. */
    private static final int INTERVAL = 200;
    /** Wild ravens already this close to the player block new spawns. */
    private static final double NEARBY_RADIUS = 96.0;
    private static final int MAX_NEARBY = 10;
    /** Flocks spawn this far from the player (in blocks), so they don't pop in right next to you. */
    private static final int MIN_DISTANCE = 32;
    private static final int MAX_DISTANCE = 72;

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % INTERVAL != 0) {
            return;
        }
        int weight = Config.RAVEN_SPAWN_WEIGHT.get();
        if (weight <= 0 || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            // spawnWeight 9 (default) = 9% chance every 10 seconds, about one flock every 2 minutes.
            if (random.nextInt(100) >= weight) {
                continue;
            }
            trySpawnFlockNear(level, player, random);
        }
    }

    private static void trySpawnFlockNear(ServerLevel level, ServerPlayer player, RandomSource random) {
        AABB area = player.getBoundingBox().inflate(NEARBY_RADIUS);
        int nearby = level.getEntitiesOfClass(RavenEntity.class, area, raven -> !raven.isTame()).size();
        if (nearby >= MAX_NEARBY) {
            return;
        }

        float angle = random.nextFloat() * Mth.TWO_PI;
        int distance = Mth.randomBetweenInclusive(random, MIN_DISTANCE, MAX_DISTANCE);
        int x = Mth.floor(player.getX() + Mth.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
        BlockPos center = surface(level, x, z);
        if (center == null || !level.getBiome(center).is(SPAWNS_RAVENS)) {
            return;
        }

        int min = Config.RAVEN_MIN_GROUP.get();
        int max = Math.max(min, Config.RAVEN_MAX_GROUP.get());
        int flock = Math.min(Mth.randomBetweenInclusive(random, min, max), MAX_NEARBY - nearby);
        EntityType<RavenEntity> type = ModEntities.RAVEN.get();

        int spawned = 0;
        for (int attempt = 0; attempt < flock * 4 && spawned < flock; attempt++) {
            BlockPos pos = attempt == 0 ? center
                    : surface(level, x + random.nextInt(9) - 4, z + random.nextInt(9) - 4);
            if (pos == null) {
                continue;
            }
            if (!RavenEntity.checkRavenSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)) {
                continue;
            }
            if (type.spawn(level, pos, EntitySpawnReason.NATURAL) != null) {
                spawned++;
            }
        }
    }

    /** Top of the ground or treetop at x/z (first open block), or null if the spot isn't loaded or isn't free. */
    private static BlockPos surface(ServerLevel level, int x, int z) {
        BlockPos column = new BlockPos(x, level.getMinY(), z);
        if (!level.hasChunkAt(column)) {
            return null;
        }
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        if (!isOpen(level, pos) || !isOpen(level, pos.above())) {
            return null;
        }
        return pos;
    }

    /** Air, short grass, flowers and the like are fine; solid blocks and water aren't. */
    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getFluidState(pos).isEmpty();
    }
}
