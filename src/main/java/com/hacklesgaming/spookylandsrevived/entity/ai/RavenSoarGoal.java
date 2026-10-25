package com.hacklesgaming.spookylandsrevived.entity.ai;

import java.util.EnumSet;
import java.util.List;

import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.entity.RavenEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Soaring: now and then a wild raven out under open sky spirals up high (flapping), circles
 * around on a glide for 20-40 seconds, then spirals slowly back down and carries on as normal.
 * When one starts, nearby flockmates often join in and circle the same spot, each at a
 * slightly different height and size of circle.
 *
 * Tower dungeons: ravens within about 64 blocks of one soar much more often, and circle
 * close around the tower's top (just outside its edge, 1-3 blocks above it), with a
 * shorter rest in between, so towers usually have a few ravens wheeling over them.
 *
 * Steered with the move control directly (no pathfinding), always aiming a little ahead
 * along the circle, so the turn is smooth. Height comes from the config (ravens.soarHeight).
 */
public class RavenSoarGoal extends Goal {
    private enum Phase { CLIMB, CIRCLE, DESCEND }

    /** Roughly how often (in ticks) a raven thinks about soaring: about every 2 minutes. */
    private static final int CHANCE_INTERVAL = 2400;
    /** Near a tower dungeon it's far more keen: about every 30 seconds. */
    private static final int TOWER_CHANCE_INTERVAL = 600;
    /** Circling height above the top of a tower (plus a random 0-2), kept low so they stay in view. */
    private static final int ABOVE_TOWER = 1;
    /** How far out from the edge of the tower top they circle (plus a random 0-2). */
    private static final double TOWER_CIRCLE_GAP = 2.0;
    /** Flockmates within this range may join in. */
    private static final double INVITE_RANGE = 16.0;
    private static final float INVITE_CHANCE = 0.6F;
    /** How far ahead along the circle it aims (blocks). */
    private static final double LEAD_DISTANCE = 2.5;
    private static final int MAX_CLIMB_TICKS = 400;
    private static final int MAX_DESCEND_TICKS = 700;
    /** Stops spiralling down at this height above the ground, then normal flight takes over. */
    private static final double DESCEND_TO_ABOVE_GROUND = 5.0;
    /** Stuck against something this long: give up. */
    private static final int MAX_STUCK_TICKS = 30;

    private final RavenEntity raven;

    private Phase phase = Phase.CLIMB;
    private Vec3 centre = Vec3.ZERO;
    private double cruiseY;
    private double radius;
    private int direction;
    private int phaseTicks;
    private int circleTicks;
    private int stuckTicks;
    private boolean invited;
    private boolean overTower;
    private boolean finished;
    private int cooldown;

    public RavenSoarGoal(RavenEntity raven) {
        this.raven = raven;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.cooldown = 1200; // not straight after spawning
    }

    @Override
    public boolean canUse() {
        // Read (and clear) any invite first so a stale one can't fire later.
        Vec3 invite = raven.consumeSoarInvite();
        if (!Config.RAVENS_SOAR.get() || !canSoarNow()) {
            return false;
        }
        Level level = raven.level();
        BoundingBox towerBox = raven.getNearbyTower();
        TowerLocator.TowerTop tower;
        radius = 6.0 + raven.getRandom().nextInt(6);
        if (invite != null) {
            tower = towerTop(level, towerBox);
            invited = true;
            centre = new Vec3(invite.x, 0.0, invite.z);
            cruiseY = invite.y + raven.getRandom().nextInt(5) - 2;
            overTower = tower != null && Math.abs(invite.x - tower.x()) < 1.0 && Math.abs(invite.z - tower.z()) < 1.0;
        } else {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            int interval = towerBox != null ? TOWER_CHANCE_INTERVAL : CHANCE_INTERVAL;
            if (raven.wantsFlockTakeoff() || raven.getRandom().nextInt(reducedTickDelay(interval)) != 0) {
                return false;
            }
            tower = towerTop(level, towerBox);
            invited = false;
            overTower = tower != null;
            if (overTower) {
                centre = new Vec3(tower.x(), 0.0, tower.z());
                cruiseY = tower.topY() + ABOVE_TOWER + raven.getRandom().nextInt(3);
            } else {
                centre = new Vec3(raven.getX() + raven.getRandom().nextInt(9) - 4, 0.0,
                        raven.getZ() + raven.getRandom().nextInt(9) - 4);
                double ground = groundY(centre.x, centre.z);
                cruiseY = Math.max(ground, raven.getY()) + Config.RAVEN_SOAR_HEIGHT.get()
                        + raven.getRandom().nextInt(7) - 3;
            }
        }
        if (overTower) {
            // Never dip below the tower top while circling it (flockmates vary the height a little).
            cruiseY = Math.max(cruiseY, tower.topY() + ABOVE_TOWER);
            // Tight circle just outside the edge of the tower top.
            radius = tower.halfSpan() + TOWER_CIRCLE_GAP + raven.getRandom().nextInt(3);
        }
        cruiseY = Math.min(cruiseY, level.getMaxY() - 4);
        // Needs open sky above it (not in a cave, under a roof or deep in a forest canopy).
        // (Ravens already up on the tower may circle it straight away.)
        return level.canSeeSky(raven.blockPosition().above()) && (overTower || cruiseY > raven.getY() + 4);
    }

    private @Nullable BoundingBox cachedTowerBox;
    private TowerLocator.@Nullable TowerTop cachedTowerTop;

    /** Top of the tower in that box. Scanning the box is a bit of work, so it's remembered per tower. */
    private TowerLocator.@Nullable TowerTop towerTop(Level level, @Nullable BoundingBox box) {
        if (box == null || !(level instanceof ServerLevel server)) {
            return null;
        }
        if (!box.equals(cachedTowerBox) || cachedTowerTop == null) {
            cachedTowerBox = box;
            cachedTowerTop = TowerLocator.findTop(server, box);
        }
        return cachedTowerTop;
    }

    private boolean canSoarNow() {
        return !raven.isTame() && !raven.isPassenger() && !raven.isOnShoulder() && !raven.hasCarriedItem()
                && !raven.isBathing() && !raven.isInWater();
    }

    @Override
    public void start() {
        phase = Phase.CLIMB;
        phaseTicks = 0;
        stuckTicks = 0;
        finished = false;
        direction = raven.getRandom().nextBoolean() ? 1 : -1;
        circleTicks = reducedTickDelay(400 + raven.getRandom().nextInt(400));
        raven.getNavigation().stop();
        if (raven.onGround()) {
            // Hop into the air so it takes off (plays the takeoff animation).
            raven.setDeltaMovement(raven.getDeltaMovement().add(0.0, 0.4, 0.0));
        }
        if (!invited) {
            inviteFlock();
        }
    }

    private void inviteFlock() {
        Vec3 invite = new Vec3(centre.x, cruiseY, centre.z);
        List<RavenEntity> nearby = raven.level().getEntitiesOfClass(RavenEntity.class,
                raven.getBoundingBox().inflate(INVITE_RANGE), other -> other != raven && other.isAlive());
        for (RavenEntity other : nearby) {
            if (raven.getRandom().nextFloat() < INVITE_CHANCE) {
                other.inviteToSoar(invite);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !finished && canSoarNow();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        phaseTicks++;

        // Bumping into something (a tree on the way up, a cliff): give up after a moment.
        stuckTicks = raven.horizontalCollision ? stuckTicks + 1 : 0;
        if (stuckTicks > MAX_STUCK_TICKS) {
            finished = true;
            return;
        }
        if (phase == Phase.CLIMB && raven.horizontalCollision) {
            // Flying into a wall (like the tower itself) on the way up: go straight up first.
            raven.getMoveControl().setWantedPosition(raven.getX(), raven.getY() + 3.0, raven.getZ(), 0.9);
            return;
        }

        double targetY;
        double speed;
        switch (phase) {
            case CLIMB -> {
                raven.setSoarGliding(false); // flapping hard on the way up
                targetY = Math.min(cruiseY, raven.getY() + 2.0);
                speed = 0.9;
                if (raven.getY() >= cruiseY - 1.0) {
                    nextPhase(Phase.CIRCLE);
                } else if (phaseTicks > MAX_CLIMB_TICKS) {
                    cruiseY = raven.getY(); // couldn't get all the way up: circle where it is
                    nextPhase(Phase.CIRCLE);
                }
            }
            case CIRCLE -> {
                raven.setSoarGliding(true);
                // Gentle rise and fall while it circles.
                targetY = cruiseY + Mth.sin(phaseTicks * 0.05F) * 0.8;
                speed = 0.6;
                if (phaseTicks >= circleTicks) {
                    nextPhase(Phase.DESCEND);
                }
            }
            default -> {
                raven.setSoarGliding(true);
                targetY = raven.getY() - 0.6;
                speed = 0.6;
                double ground = groundY(raven.getX(), raven.getZ());
                if (raven.getY() <= ground + DESCEND_TO_ABOVE_GROUND || phaseTicks > MAX_DESCEND_TICKS) {
                    finished = true;
                    return;
                }
            }
        }

        // Aim a little further round the circle than where it is now.
        double angle = Math.atan2(raven.getZ() - centre.z, raven.getX() - centre.x) + direction * (LEAD_DISTANCE / radius);
        double tx = centre.x + Math.cos(angle) * radius;
        double tz = centre.z + Math.sin(angle) * radius;
        raven.getMoveControl().setWantedPosition(tx, targetY, tz, speed);
        raven.getLookControl().setLookAt(tx, targetY, tz);
    }

    private void nextPhase(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    @Override
    public void stop() {
        raven.setSoarGliding(false);
        raven.getNavigation().stop();
        // Shorter rest near a tower, so there's usually something circling it.
        cooldown = overTower ? reducedTickDelay(600 + raven.getRandom().nextInt(1200))
                : reducedTickDelay(invited ? 2400 : 3600 + raven.getRandom().nextInt(2400));
        invited = false;
        overTower = false;
    }

    /** Height of the top of whatever is below (ground, leaves, buildings). */
    private double groundY(double x, double z) {
        return raven.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }
}
