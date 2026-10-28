package com.hacklesgaming.spookylandsrevived.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.block.entity.ScarecrowBlockEntity;
import com.hacklesgaming.spookylandsrevived.registry.ModSounds;
import com.hacklesgaming.spookylandsrevived.entity.ai.GallowsLocator;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenAvoidScarecrowGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenFollowOwnerGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenGiftGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenShoulderGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenSoarGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.TowerLocator;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenSitGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenBatheGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenFlockGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenPerchGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenPickUpValuableGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenRaidCropGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenRideMobGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenStashGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenStealFromPlayerGoal;
import com.hacklesgaming.spookylandsrevived.entity.ai.RavenWanderGoal;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ambient raven.
 * Flight AI: takes off, wanders in the air, glides on the way down, lands, and likes to
 * perch on raised blocks (data tag spookylandsrevived:raven_perches). Avoids lava and fire.
 * Flocking: drifts back toward nearby ravens, wanders in the flock's direction, and
 * nearby ravens take off together.
 * Bathing: occasionally splashes about in nearby shallow water or a full water cauldron.
 * Gallows: loosely drawn to a village gallows nearby, hangs around it and sits on its beam.
 * Theft: snatches valuables from players and the ground, and pulls up ripe crops.
 * Hit it and it drops what it's carrying.
 * Nests: takes loot home to its nest (builds one on a treetop if needed; flockmates share it).
 * Scarecrows: no theft/crop raids/nest building within 16 blocks; spooked away within 8.
 * Taming: give it valuables (1 in 3). Tamed: right-click toggles sit/follow, sneak-right-click
 * makes it wander near where it is instead of following (and back), never steals
 * or raids crops, and brings its owner valuables it finds lying around plus the odd gift.
 * Shoulder: lands on its owner's shoulder when they stand still; hops off on sneak, hits, water, falls.
 * Riding: wild ravens now and then sit on the back of an animal or a villager's head for a while.
 * Bold: about 1 in 100 ravens (config) isn't scared of scarecrows and likes to sit on them.
 * Soaring: now and then spirals up high, circles on a glide with its flock, then spirals down.
 * Near a tower dungeon it soars much more often, circling above the tower.
 */
public class RavenEntity extends PathfinderMob implements GeoEntity {

    /** Blocks ravens like to sit on. Edit data/spookylandsrevived/tags/block/raven_perches.json. */
    public static final TagKey<Block> PERCHES = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "raven_perches"));

    // ---- movement state (server decides, synced to client for animations) ----
    public static final byte STATE_GROUND = 0;
    public static final byte STATE_PERCHED = 1;
    public static final byte STATE_TAKEOFF = 2;
    public static final byte STATE_FLYING = 3;
    public static final byte STATE_GLIDING = 4;
    public static final byte STATE_LANDING = 5;
    public static final byte STATE_BATHING = 6;

    /** takeoff/land animations are 0.4s = 8 ticks. */
    private static final int TRANSITION_TICKS = 8;
    /** Must be airborne this long before touching down counts as a "landing". */
    private static final int MIN_AIR_TICKS_FOR_LANDING = 10;
    /** Descending this many ticks in a row before switching from flapping to gliding. */
    private static final int TICKS_TO_START_GLIDE = 6;

    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<ItemStack> DATA_CARRIED =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_TAME =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.BOOLEAN);
    /** Entity id of the player whose shoulder it's on, or -1. */
    private static final EntityDataAccessor<Integer> DATA_SHOULDER_OF =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.INT);

    // Animation names must match raven.animation.json exactly.
    private static final RawAnimation IDLE_GROUND = RawAnimation.begin().thenLoop("animation.raven.idle_ground");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.raven.walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.raven.fly");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.raven.glide");
    private static final RawAnimation PERCH_IDLE = RawAnimation.begin().thenLoop("animation.raven.perch_idle");
    private static final RawAnimation TAKEOFF = RawAnimation.begin()
            .thenPlay("animation.raven.takeoff").thenLoop("animation.raven.fly");
    private static final RawAnimation LAND = RawAnimation.begin()
            .thenPlay("animation.raven.land").thenLoop("animation.raven.idle_ground");
    private static final RawAnimation BATHE = RawAnimation.begin().thenLoop("animation.raven.bathe");
    private static final RawAnimation CARRY = RawAnimation.begin().thenLoop("animation.raven.carry");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // ---- flocking ----
    /** How far a raven looks for its flock. */
    public static final double FLOCK_RANGE = 24.0;
    /** Ravens on the ground within this range may take off when one of them does. */
    private static final double TAKEOFF_ALERT_RANGE = 8.0;
    /** Chance each nearby raven joins a takeoff. */
    private static final float TAKEOFF_JOIN_CHANCE = 0.75F;

    private @Nullable RavenWanderGoal wanderGoal;
    private int flockTakeoffDelay = -1;
    private boolean pendingFlockFlight;
    private boolean inducedTakeoff;
    private boolean alertPending;
    /** Set by RavenBatheGoal while splashing in the water. */
    private boolean bathing;
    private boolean bathingInCauldron;
    /** Set by RavenPerchGoal while sitting on its chosen spot. */
    private boolean perchOverride;

    // server-side bookkeeping for the state machine
    private boolean wasAirborne;
    private int airTicks;
    private int descendTicks;
    private int transitionTicks;

    public RavenEntity(EntityType<? extends RavenEntity> type, Level level) {
        super(type, level);
        // false = gravity applies when it has no move target, so ravens drift down and land.
        this.moveControl = new FlyingMoveControl(this, 10, false);
        // Never path through or next to fire/lava/damaging blocks.
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGING, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, -1.0F);
    }

    // ---- attributes ----

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ---- natural spawning ----

    /**
     * Where ravens may spawn naturally: under open sky, standing on grass/dirt-type ground,
     * leaves or logs (so they can spawn on treetops). Light doesn't matter: ravens are out at night too.
     * Which biomes is set by the data tag spookylandsrevived:spawns_ravens.
     */
    public static boolean checkRavenSpawnRules(EntityType<RavenEntity> type, ServerLevelAccessor level,
                                               EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean goodGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS);
        return goodGround && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos));
    }

    // ---- movement ----

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new RavenSitGoal(this));
        this.goalSelector.addGoal(1, new RavenGiftGoal(this));
        this.goalSelector.addGoal(1, new RavenFollowOwnerGoal(this));
        this.goalSelector.addGoal(1, new RavenShoulderGoal(this));
        this.goalSelector.addGoal(1, new RavenAvoidScarecrowGoal(this));
        this.goalSelector.addGoal(1, new RavenStashGoal(this));
        this.goalSelector.addGoal(1, new RavenStealFromPlayerGoal(this));
        this.goalSelector.addGoal(2, new RavenPickUpValuableGoal(this));
        this.goalSelector.addGoal(2, new RavenRaidCropGoal(this));
        this.goalSelector.addGoal(2, new RavenBatheGoal(this));
        this.goalSelector.addGoal(2, new RavenPerchGoal(this));
        this.goalSelector.addGoal(2, new RavenRideMobGoal(this));
        // Same priority as perching etc., so those can't cut a soaring flight short.
        this.goalSelector.addGoal(2, new RavenSoarGoal(this));
        this.goalSelector.addGoal(3, new RavenFlockGoal(this));
        this.wanderGoal = new RavenWanderGoal(this, 1.0);
        this.goalSelector.addGoal(4, this.wanderGoal);
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Ravens take no fall damage (same approach as the parrot).
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Wings out when dropping: cap the fall speed so it glides down instead of plummeting.
        Vec3 motion = this.getDeltaMovement();
        if (!this.onGround() && !this.isInWater() && motion.y < 0.0) {
            this.setDeltaMovement(motion.multiply(1.0, 0.75, 1.0));
        }

        if (this.level() instanceof ServerLevel) {
            rollBoldOnce();
            // Riding something the ride goal didn't put it on (e.g. reloaded from a save while
            // sitting on a cow): hop off rather than staying there forever.
            if (this.isPassenger() && !ridingByGoal) {
                this.stopRiding();
            }
            // Hovering over lava/fire with nothing to do: flap upward until the wander goal moves it away.
            if (!this.onGround() && this.getNavigation().isDone() && isHazardBelow(this.blockPosition())) {
                Vec3 m = this.getDeltaMovement();
                this.setDeltaMovement(m.x, Math.max(m.y, 0.08), m.z);
            }
            if (gallowsCheckCooldown > 0) {
                gallowsCheckCooldown--;
            }
            if (towerCheckCooldown > 0) {
                towerCheckCooldown--;
            }
            carriedTicks = hasCarriedItem() ? carriedTicks + 1 : 0;
            if (shoulderCooldown > 0) {
                shoulderCooldown--;
            }
            if (soarInviteTicks > 0 && --soarInviteTicks == 0) {
                soarInvite = null;
            }
            tickFlockTakeoff();
            updateMovementState();
        }
    }

    /** Server-side state machine that drives the animation state. */
    private void updateMovementState() {
        if (isOnShoulder() || this.isPassenger()) {
            setMovementState(STATE_PERCHED);
            wasAirborne = false;
            airTicks = 0;
            descendTicks = 0;
            transitionTicks = 0;
            alertPending = false;
            return;
        }
        if (bathing) {
            setMovementState(STATE_BATHING);
            wasAirborne = false;
            airTicks = 0;
            descendTicks = 0;
            transitionTicks = 0;
            alertPending = false;
            return;
        }

        boolean airborne = !this.onGround() && !this.isInWater();
        Vec3 motion = this.getDeltaMovement();

        if (airborne) {
            airTicks++;
            descendTicks = motion.y < -0.03 ? descendTicks + 1 : 0;
        }

        if (airborne && !wasAirborne && motion.y > 0.0) {
            // Left the ground going up: play takeoff. (Stepping off a ledge just falls into glide.)
            setMovementState(STATE_TAKEOFF);
            transitionTicks = TRANSITION_TICKS;
            // A raven taking off on its own startles the ones around it into following
            // (checked below once it's clearly a real flight, not a hop).
            // Ones that were themselves startled don't pass it on, so it can't chain across the map.
            alertPending = !inducedTakeoff;
            inducedTakeoff = false;
        } else if (!airborne && wasAirborne && airTicks >= MIN_AIR_TICKS_FOR_LANDING && !this.isInWater()) {
            setMovementState(STATE_LANDING);
            transitionTicks = TRANSITION_TICKS;
        }

        if (airborne && alertPending && airTicks >= MIN_AIR_TICKS_FOR_LANDING) {
            alertPending = false;
            alertFlockOfTakeoff();
        }

        if (!airborne) {
            airTicks = 0;
            descendTicks = 0;
            alertPending = false;
        }
        wasAirborne = airborne;

        if (transitionTicks > 0) {
            transitionTicks--;
            // Cancel a takeoff if it landed again right away, or a landing if it took off again.
            byte state = getMovementState();
            boolean stillValid = (state == STATE_TAKEOFF) == airborne;
            if (stillValid) {
                return;
            }
            transitionTicks = 0;
        }

        if (airborne) {
            // Circling high up (soar goal) holds its wings out even while flying level.
            setMovementState(soarGliding || descendTicks >= TICKS_TO_START_GLIDE ? STATE_GLIDING : STATE_FLYING);
        } else {
            // Sitting on command uses the calm perched pose too.
            setMovementState(isOnPerch() || isOrderedToSit() ? STATE_PERCHED : STATE_GROUND);
        }
    }

    // ---- flocking ----

    /** Average position of the other ravens within range, or null if it's alone. */
    public @Nullable Vec3 getFlockCenter(double range) {
        List<RavenEntity> others = this.level().getEntitiesOfClass(RavenEntity.class,
                this.getBoundingBox().inflate(range), other -> other != this && other.isAlive());
        if (others.isEmpty()) {
            return null;
        }
        double x = 0, y = 0, z = 0;
        for (RavenEntity other : others) {
            x += other.getX();
            y += other.getY();
            z += other.getZ();
        }
        int n = others.size();
        return new Vec3(x / n, y / n, z / n);
    }

    private void alertFlockOfTakeoff() {
        List<RavenEntity> nearby = this.level().getEntitiesOfClass(RavenEntity.class,
                this.getBoundingBox().inflate(TAKEOFF_ALERT_RANGE), other -> other != this && other.isAlive());
        for (RavenEntity other : nearby) {
            if (this.getRandom().nextFloat() < TAKEOFF_JOIN_CHANCE) {
                // Small random delay so they don't all lift off on the same tick.
                other.requestFlockTakeoff(2 + this.getRandom().nextInt(14));
            }
        }
    }

    /** Ask this raven to take off after a few ticks (only if it's sitting or standing). */
    public void requestFlockTakeoff(int delayTicks) {
        if (!this.onGround() || this.isInWater() || flockTakeoffDelay >= 0 || isTame()) {
            return;
        }
        flockTakeoffDelay = delayTicks;
    }

    private void tickFlockTakeoff() {
        if (flockTakeoffDelay < 0) {
            return;
        }
        if (flockTakeoffDelay-- > 0) {
            return;
        }
        flockTakeoffDelay = -1;
        if (this.onGround() && wanderGoal != null) {
            pendingFlockFlight = true;
            inducedTakeoff = true;
            wanderGoal.trigger();
        }
    }

    /** Make the next wander a flight, starting now (used after bathing). */
    public void requestFlight() {
        if (wanderGoal != null) {
            pendingFlockFlight = true;
            wanderGoal.trigger();
        }
    }

    public void setBathing(boolean bathing, boolean inCauldron) {
        this.bathing = bathing;
        this.bathingInCauldron = bathing && inCauldron;
    }

    @Override
    protected double getDefaultGravity() {
        // Held in place inside the cauldron while bathing there.
        // Also no gravity while sitting on a shoulder.
        return bathingInCauldron || isOnShoulder() ? 0.0 : super.getDefaultGravity();
    }

    public boolean isBathing() {
        return bathing;
    }

    /** True while a flock takeoff is pending: perching stops so the raven can leave. */
    public boolean wantsFlockTakeoff() {
        return pendingFlockFlight || flockTakeoffDelay >= 0;
    }

    /** Wander goal calls this: true once, meaning "pick a flight, not a walk". */
    public boolean consumeFlockFlight() {
        boolean pending = pendingFlockFlight;
        pendingFlockFlight = false;
        return pending;
    }

    /**
     * True when standing on a block from the raven_perches tag (fence, wall, leaves, log...),
     * or sitting on a spot the perch goal picked (like the gallows beam).
     */
    public boolean isOnPerch() {
        return this.onGround() && (perchOverride || this.getBlockStateOn().is(PERCHES));
    }

    public void setPerchOverride(boolean perchOverride) {
        this.perchOverride = perchOverride;
    }

    // ---- gallows ----

    /** How far away a village gallows still draws ravens in. */
    public static final double GALLOWS_RANGE = 48.0;
    /** Re-check for a gallows this often; the lookup reads structure data, so don't do it every tick. */
    private static final int GALLOWS_CHECK_INTERVAL = 200;

    private @Nullable BoundingBox nearbyGallows;
    private int gallowsCheckCooldown;

    /** Bounding box of the nearest village gallows within range (server only, cached). */
    public @Nullable BoundingBox getNearbyGallows() {
        if (!(this.level() instanceof ServerLevel level)) {
            return null;
        }
        if (gallowsCheckCooldown <= 0) {
            gallowsCheckCooldown = GALLOWS_CHECK_INTERVAL + this.getRandom().nextInt(40);
            nearbyGallows = GallowsLocator.findNearest(level, this.blockPosition(), GALLOWS_RANGE);
        }
        return nearbyGallows;
    }

    /** Lava, fire, magma or campfire anywhere in the column below (until the first solid block). */
    public boolean isHazardBelow(BlockPos start) {
        Level level = this.level();
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < 12 && pos.getY() > level.getMinY(); i++) {
            BlockState state = level.getBlockState(pos);
            if (isHazard(state) || level.getFluidState(pos).is(FluidTags.LAVA)) {
                return true;
            }
            if (!state.getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) {
                return false; // hit something safe to stand on (or water)
            }
            pos.move(0, -1, 0);
        }
        return false;
    }

    private static boolean isHazard(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.MAGMA_BLOCK);
    }

    // ---- synced state ----

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, STATE_GROUND);
        builder.define(DATA_CARRIED, ItemStack.EMPTY);
        builder.define(DATA_TAME, false);
        builder.define(DATA_SHOULDER_OF, -1);
    }

    public byte getMovementState() {
        return this.entityData.get(DATA_STATE);
    }

    private void setMovementState(byte state) {
        if (getMovementState() != state) {
            this.entityData.set(DATA_STATE, state);
        }
    }

    public boolean isFlying() {
        byte state = getMovementState();
        return state == STATE_TAKEOFF || state == STATE_FLYING || state == STATE_GLIDING;
    }

    // ---- carried item (theft, nests, gifts use this later) ----

    public ItemStack getCarriedItem() {
        return this.entityData.get(DATA_CARRIED);
    }

    public void setCarriedItem(ItemStack stack) {
        this.entityData.set(DATA_CARRIED, stack);
    }

    public boolean hasCarriedItem() {
        return !getCarriedItem().isEmpty();
    }

    /** Items ravens steal and pick up. Edit data/spookylandsrevived/tags/item/raven_valuables.json. */
    public static final TagKey<Item> VALUABLES = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "raven_valuables"));

    public static boolean isValuable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(VALUABLES);
    }

    /** Drop whatever it's carrying. Short pickup delay so it doesn't grab it straight back. */
    public void dropCarriedItem(ServerLevel level) {
        if (!hasCarriedItem()) {
            return;
        }
        ItemEntity dropped = this.spawnAtLocation(level, getCarriedItem());
        if (dropped != null) {
            dropped.setPickUpDelay(40);
        }
        setCarriedItem(ItemStack.EMPTY);
    }

    // ---- scarecrows ----

    /** Ravens won't steal, raid crops or build nests within this distance of a scarecrow. */
    public static final double PROTECT_RADIUS = 16.0;
    /** Ravens get spooked and fly off when this close to a scarecrow. */
    public static final double SCARE_RADIUS = 8.0;

    public boolean isScarecrowProtected(BlockPos pos) {
        return ScarecrowBlockEntity.isNear(this.level(), pos, PROTECT_RADIUS);
    }

    /** Bold ravens aren't spooked by scarecrows (but still won't steal near one). */
    public boolean isNearScarecrow(BlockPos pos) {
        return !isBold() && ScarecrowBlockEntity.isNear(this.level(), pos, SCARE_RADIUS);
    }

    // ---- bold ravens ----

    /** Rare fearless raven: ignores scarecrows' scare radius and likes to sit on them. */
    private boolean bold;
    /** Whether the bold chance has been rolled yet (rolled once, on the first server tick). */
    private boolean boldRolled;

    public boolean isBold() {
        return bold;
    }

    private void rollBoldOnce() {
        if (!boldRolled) {
            boldRolled = true;
            bold = this.getRandom().nextDouble() < Config.RAVEN_BOLD_CHANCE.get();
        }
    }

    // ---- riding other mobs ----

    /** Mobs a wild raven may hitch a ride on. Edit data/spookylandsrevived/tags/entity_type/raven_rideable.json. */
    public static final TagKey<EntityType<?>> RIDEABLE = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "raven_rideable"));

    // ---- soaring ----

    /** Tower dungeons within this many chunks draw ravens to soar around them (4 = about 64 blocks). */
    private static final int TOWER_CHUNK_RADIUS = 4;
    /** Re-check for a tower this often; it reads structure data from several chunks. */
    private static final int TOWER_CHECK_INTERVAL = 300;

    private @Nullable BoundingBox nearbyTower;
    private int towerCheckCooldown;

    /** Bounding box of the nearest tower dungeon within about 64 blocks (server only, cached). */
    public @Nullable BoundingBox getNearbyTower() {
        if (!(this.level() instanceof ServerLevel level)) {
            return null;
        }
        if (towerCheckCooldown <= 0) {
            towerCheckCooldown = TOWER_CHECK_INTERVAL + this.getRandom().nextInt(60);
            nearbyTower = TowerLocator.findNearest(level, this.blockPosition(), TOWER_CHUNK_RADIUS);
        }
        return nearbyTower;
    }

    /** Set by RavenSoarGoal while circling or spiralling down: forces the glide pose. */
    private boolean soarGliding;
    /** A flockmate started soaring and invited this raven along: circle centre and height. */
    private @Nullable Vec3 soarInvite;
    private int soarInviteTicks;

    public void setSoarGliding(boolean soarGliding) {
        this.soarGliding = soarGliding;
    }

    /** Ask this raven to join a soaring flight around centre (x, z) at height y. Lasts a few seconds. */
    public void inviteToSoar(Vec3 centre) {
        if (!isTame() && !isBathing() && !isPassenger() && !hasCarriedItem()) {
            soarInvite = centre;
            soarInviteTicks = 60;
        }
    }

    /** The pending soar invite (once), or null. */
    public @Nullable Vec3 consumeSoarInvite() {
        Vec3 invite = soarInviteTicks > 0 ? soarInvite : null;
        soarInvite = null;
        soarInviteTicks = 0;
        return invite;
    }

    /** Set by RavenRideMobGoal while it's deliberately riding something. */
    private boolean ridingByGoal;

    public void setRidingByGoal(boolean ridingByGoal) {
        this.ridingByGoal = ridingByGoal;
    }

    // ---- nest ----

    private @Nullable BlockPos homeNest;
    private int carriedTicks;

    /** Where this raven stashes loot (null = no nest yet). */
    public @Nullable BlockPos getHomeNest() {
        return homeNest;
    }

    public void setHomeNest(@Nullable BlockPos pos) {
        this.homeNest = pos == null ? null : pos.immutable();
    }

    /** How long it's been holding the current item (server side). */
    public int getCarriedTicks() {
        return carriedTicks;
    }

    // ---- sounds ----

    /** Random caw every once in a while (see getAmbientSoundInterval). */
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RAVEN_CAW.get();
    }

    /**
     * Minimum ticks between random caws. Vanilla default is 80 (4 s); 400 means a raven
     * caws roughly every 20-40 seconds, so a flock isn't constantly noisy.
     */
    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RAVEN_HURT.get();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            if (isOnShoulder()) {
                hopOffShoulder(SHOULDER_COOLDOWN_TICKS);
            }
            if (this.isPassenger()) {
                this.stopRiding(); // startled off whatever it was riding
            }
            dropCarriedItem(level); // startled: lets go of the loot
        }
        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        // Never despawn while holding something, or the stolen item would vanish with it.
        // Tamed ravens never despawn.
        return !isTame() && !hasCarriedItem() && super.removeWhenFarAway(distSqr);
    }

    // ---- taming ----

    /** Entity event: sparkles when it drops a gift for its owner. (Vanilla uses lower numbers.) */
    public static final byte EVENT_GIFT_PARTICLES = 80;
    /** Chance each valuable given to a wild raven tames it. */
    private static final float TAME_CHANCE = 1.0F / 3.0F;

    private @Nullable UUID ownerUUID;
    private boolean orderedToSit;

    public boolean isTame() {
        return this.entityData.get(DATA_TAME);
    }

    public boolean isOrderedToSit() {
        return orderedToSit;
    }

    public void setOrderedToSit(boolean sit) {
        this.orderedToSit = sit;
        if (sit) {
            stopWandering(); // sitting wins over wandering
            this.getNavigation().stop();
        }
    }

    // ---- wander mode (sneak-click): roams near a home spot instead of following the owner ----

    private boolean wandering;
    private @Nullable BlockPos wanderHome;

    public boolean isWandering() {
        return wandering;
    }

    /** Stop following and roam within the configured radius of where the raven is now. */
    public void startWandering() {
        this.orderedToSit = false;
        this.wandering = true;
        this.wanderHome = this.blockPosition().immutable();
        this.getNavigation().stop();
    }

    public void stopWandering() {
        this.wandering = false;
        this.wanderHome = null;
    }

    /** The spot a wandering raven stays near, or null when it isn't wandering. */
    public @Nullable BlockPos getWanderHome() {
        return wandering ? wanderHome : null;
    }

    /** True if pos is within the wander radius of the home spot (always true when not wandering). */
    public boolean isWithinWanderRange(Vec3 pos) {
        if (!wandering || wanderHome == null) {
            return true;
        }
        double radius = Config.PET_WANDER_RADIUS.get();
        return Vec3.atCenterOf(wanderHome).distanceToSqr(pos) <= radius * radius;
    }

    /**
     * Owner clicked the raven. Pet modes off or plain click with requireSneak on: toggles
     * sit / follow like before (a plain click on a wandering raven sits it). Sneak-click:
     * follow or sit -> wander -> follow. requireSneak off: plain click cycles follow -> sit -> wander.
     */
    private void cycleMode(Player player) {
        boolean modes = Config.PET_MODES_ENABLED.get();
        boolean requireSneak = Config.PET_REQUIRE_SNEAK.get();
        boolean modeClick = modes && (player.isShiftKeyDown() || !requireSneak);
        if (!modes && wandering) {
            stopWandering();
        }

        String message;
        if (wandering) {
            if (modeClick) {
                stopWandering();
                message = "entity.spookylandsrevived.raven.following";
            } else {
                setOrderedToSit(true);
                message = "entity.spookylandsrevived.raven.sitting";
            }
        } else if (modeClick && (requireSneak || orderedToSit)) {
            startWandering();
            message = "entity.spookylandsrevived.raven.wandering";
        } else {
            setOrderedToSit(!orderedToSit);
            message = orderedToSit
                    ? "entity.spookylandsrevived.raven.sitting"
                    : "entity.spookylandsrevived.raven.following";
        }
        player.sendOverlayMessage(Component.translatable(message));
    }

    public @Nullable Player getOwner() {
        return ownerUUID == null ? null : this.level().getPlayerByUUID(ownerUUID);
    }

    public boolean isOwnedBy(Player player) {
        return ownerUUID != null && ownerUUID.equals(player.getUUID());
    }

    private void tameFor(Player player) {
        this.ownerUUID = player.getUUID();
        this.entityData.set(DATA_TAME, true);
        this.setOrderedToSit(false);
        this.setPersistenceRequired();
        this.setHomeNest(null);
    }

    /**
     * Wild: give it a valuable (1 in 3 chance to tame, consumed either way).
     * Tamed, owner: right-click toggles sit / follow. Giving it a valuable heals it.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean valuable = isValuable(held);

        if (!isTame()) {
            if (!valuable) {
                return super.mobInteract(player, hand);
            }
            if (this.level() instanceof ServerLevel) {
                held.consume(1, player);
                if (this.getRandom().nextFloat() < TAME_CHANCE) {
                    tameFor(player);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!isOwnedBy(player) && this.level() instanceof ServerLevel) {
            return super.mobInteract(player, hand);
        }

        if (valuable && this.getHealth() < this.getMaxHealth()) {
            if (this.level() instanceof ServerLevel) {
                held.consume(1, player);
                this.heal(2.0F);
                this.level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.SUCCESS;
        }

        if (this.level() instanceof ServerLevel) {
            cycleMode(player);
        }
        return InteractionResult.SUCCESS;
    }

    // ---- riding on the owner's shoulder ----

    /** Right shoulder, relative to the player's feet, before turning with the player's body. */
    private static final double SHOULDER_SIDE = -0.42;
    private static final double SHOULDER_HEIGHT = 1.35;
    private static final double SHOULDER_HEIGHT_SNEAKING = 1.05;
    /** How far behind the shoulder centre it sits (negative = back), so it stays out of first-person view. */
    private static final double SHOULDER_BACK = -0.10;
    /** After hopping off, wait this long before landing on the shoulder again. */
    private static final int SHOULDER_COOLDOWN_TICKS = 200;
    private static final int SHOULDER_COOLDOWN_AFTER_SNEAK_TICKS = 600;

    private int shoulderCooldown;

    /*
     * Minecraft doesn't allow entities to ride players, so instead the raven stays a normal
     * entity and snaps to the owner's shoulder every tick, on both server and client
     * (DATA_SHOULDER_OF says whose shoulder, so the client can follow smoothly too).
     */

    /** Players a raven can land on right now (parrot-like rules). */
    public static boolean canCarryRaven(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isInWater() && !player.isSleeping()
                && !player.isFallFlying() && !player.isShiftKeyDown() && player.onGround() && !player.isPassenger();
    }

    public boolean canLandOnShoulderAgain() {
        return shoulderCooldown <= 0;
    }

    public boolean isOnShoulder() {
        return this.entityData.get(DATA_SHOULDER_OF) >= 0;
    }

    /** The player whose shoulder it's sitting on, or null. */
    public @Nullable Player getShoulderPlayer() {
        int id = this.entityData.get(DATA_SHOULDER_OF);
        return id >= 0 && this.level().getEntity(id) instanceof Player player ? player : null;
    }

    /** World position of the owner's right shoulder (where the raven's feet go). */
    public Vec3 shoulderPositionOf(Player player) {
        double height = player.isShiftKeyDown() ? SHOULDER_HEIGHT_SNEAKING : SHOULDER_HEIGHT;
        Vec3 offset = new Vec3(SHOULDER_SIDE, height, SHOULDER_BACK).yRot(-player.yBodyRot * Mth.DEG_TO_RAD);
        return player.position().add(offset);
    }

    public void landOnShoulder(Player player) {
        if (!(this.level() instanceof ServerLevel) || isOnShoulder()) {
            return;
        }
        this.entityData.set(DATA_SHOULDER_OF, player.getId());
        this.getNavigation().stop();
        this.playSound(SoundEvents.PARROT_FLY, 0.4F, 0.8F);
        snapToShoulder(player);
    }

    public void hopOffShoulder(int cooldown) {
        Player player = getShoulderPlayer();
        this.entityData.set(DATA_SHOULDER_OF, -1);
        shoulderCooldown = cooldown;
        if (player != null) {
            // Pop up and out to the side so it doesn't land straight back on the player.
            Vec3 side = new Vec3(SHOULDER_SIDE, 0.0, 0.0).yRot(-player.yBodyRot * Mth.DEG_TO_RAD).normalize();
            this.setDeltaMovement(side.x * 0.3, 0.3, side.z * 0.3);
        }
    }

    private void snapToShoulder(Player player) {
        Vec3 shoulder = shoulderPositionOf(player);
        this.setPos(shoulder.x, shoulder.y, shoulder.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(player.yBodyRot);
        this.yBodyRot = player.yBodyRot;
        this.yHeadRot = player.getYHeadRot();
        this.resetFallDistance();
    }

    /** Runs after the normal tick, so the shoulder position wins over any movement. */
    private void tickShoulder() {
        if (!isOnShoulder()) {
            return;
        }
        Player player = getShoulderPlayer();
        if (player == null) {
            if (this.level() instanceof ServerLevel) {
                hopOffShoulder(SHOULDER_COOLDOWN_TICKS); // owner left / changed dimension
            }
            return;
        }
        if (this.level() instanceof ServerLevel) {
            if (player.isShiftKeyDown()) {
                hopOffShoulder(SHOULDER_COOLDOWN_AFTER_SNEAK_TICKS);
                return;
            }
            if (hasCarriedItem()) {
                hopOffShoulder(SHOULDER_COOLDOWN_TICKS); // found a gift: hop down to hand it over
                return;
            }
            if (!player.isAlive() || player.isSpectator() || player.isInWater() || player.isSleeping()
                    || player.isFallFlying() || player.isPassenger() || player.hurtTime > 0
                    || player.fallDistance > 1.0F || !isOwnedBy(player) || isOrderedToSit() || isWandering()) {
                hopOffShoulder(SHOULDER_COOLDOWN_TICKS);
                return;
            }
        }
        snapToShoulder(player);
    }

    @Override
    public void tick() {
        super.tick();
        tickShoulder();
    }

    /** Goals that move or jump the raven are switched off while it's on a shoulder. */
    @Override
    protected void updateControlFlags() {
        super.updateControlFlags();
        if (isOnShoulder()) {
            this.goalSelector.setControlFlag(Goal.Flag.MOVE, false);
            this.goalSelector.setControlFlag(Goal.Flag.JUMP, false);
        }
    }

    /** Not targetable or pushable on the shoulder, so it doesn't get in the way of its owner. */
    @Override
    public boolean isPickable() {
        return !isOnShoulder() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !isOnShoulder() && super.isPushable();
    }

    @Override
    protected void doPush(Entity entity) {
        if (!isOnShoulder()) {
            super.doPush(entity);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 7 || id == 6 || id == EVENT_GIFT_PARTICLES) {
            ParticleOptions particle = id == 7 ? ParticleTypes.HEART
                    : id == 6 ? ParticleTypes.SMOKE : ParticleTypes.HAPPY_VILLAGER;
            for (int i = 0; i < 7; i++) {
                this.level().addParticle(particle, this.getRandomX(1.0), this.getRandomY() + 0.3, this.getRandomZ(1.0),
                        this.getRandom().nextGaussian() * 0.02, this.getRandom().nextGaussian() * 0.02,
                        this.getRandom().nextGaussian() * 0.02);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (hasCarriedItem()) {
            this.spawnAtLocation(level, getCarriedItem());
            setCarriedItem(ItemStack.EMPTY);
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("CarriedItem", ItemStack.OPTIONAL_CODEC, getCarriedItem());
        output.storeNullable("HomeNest", BlockPos.CODEC, homeNest);
        output.storeNullable("Owner", UUIDUtil.CODEC, ownerUUID);
        output.putBoolean("Sitting", orderedToSit);
        output.putBoolean("Wandering", wandering);
        output.storeNullable("WanderHome", BlockPos.CODEC, wanderHome);
        output.putBoolean("Bold", bold);
        output.putBoolean("BoldRolled", boldRolled);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setCarriedItem(input.read("CarriedItem", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        homeNest = input.read("HomeNest", BlockPos.CODEC).orElse(null);
        ownerUUID = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.entityData.set(DATA_TAME, ownerUUID != null);
        orderedToSit = input.getBooleanOr("Sitting", false);
        wanderHome = input.read("WanderHome", BlockPos.CODEC).orElse(null);
        wandering = wanderHome != null && !orderedToSit && input.getBooleanOr("Wandering", false);
        bold = input.getBooleanOr("Bold", false);
        boldRolled = input.getBooleanOr("BoldRolled", false);
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        // Body movement, picked from the synced movement state. 3-tick blend between states.
        controllers.add(new AnimationController<RavenEntity>("movement", 3, test -> {
            RavenEntity raven = test.animatable();
            return switch (raven.getMovementState()) {
                case STATE_TAKEOFF -> test.setAndContinue(TAKEOFF);
                case STATE_FLYING -> test.setAndContinue(FLY);
                case STATE_GLIDING -> test.setAndContinue(GLIDE);
                case STATE_LANDING -> test.setAndContinue(LAND);
                case STATE_PERCHED -> test.setAndContinue(PERCH_IDLE);
                case STATE_BATHING -> test.setAndContinue(BATHE);
                default -> test.setAndContinue(test.isMoving() ? WALK : IDLE_GROUND);
            };
        }));

        // Beak/held-item pose, layered on top of movement. Only runs while carrying something.
        controllers.add(new AnimationController<RavenEntity>("carry", 4, test ->
                test.animatable().hasCarriedItem() ? test.setAndContinue(CARRY) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
