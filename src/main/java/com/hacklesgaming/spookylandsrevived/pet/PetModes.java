package com.hacklesgaming.spookylandsrevived.pet;

import java.util.Optional;

import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;

/**
 * Pet behaviour modes for tamable animals (vanilla and other mods):
 * SIT    = the pet's own "ordered to sit" flag (vanilla's click toggle keeps working),
 * WANDER = roams within a few blocks of a home spot, doesn't follow the owner,
 * FOLLOW = neither of the above: the pet's normal behaviour.
 * Only WANDER is stored by us; the mode is worked out from the pet, so it can't get out of
 * sync with vanilla's sit toggle.
 */
public final class PetModes {
    public enum Mode { FOLLOW, SIT, WANDER }

    /** Pets in this tag never get the wander mode. data/spookylandsrevived/tags/entity_type/no_mode_cycle.json */
    public static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SpookyLandsRevived.MOD_ID, "no_mode_cycle"));

    private PetModes() {
    }

    /** True if the entity type is in the spookylandsrevived:no_mode_cycle tag. */
    public static boolean isExcludedType(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(EXCLUDED);
    }

    public static Mode get(TamableAnimal pet) {
        if (pet.isOrderedToSit()) {
            return Mode.SIT;
        }
        return pet.getData(ModAttachments.PET_MODE).wandering() ? Mode.WANDER : Mode.FOLLOW;
    }

    public static Optional<BlockPos> getHome(TamableAnimal pet) {
        return pet.getData(ModAttachments.PET_MODE).home();
    }

    public static void setWander(TamableAnimal pet, BlockPos home) {
        pet.setData(ModAttachments.PET_MODE, new PetModeData(true, Optional.of(home.immutable())));
    }

    public static void clearWander(TamableAnimal pet) {
        if (pet.getData(ModAttachments.PET_MODE).wandering()) {
            pet.setData(ModAttachments.PET_MODE, PetModeData.DEFAULT);
        }
    }

    /** Whether the wander mode applies to this pet, going by the config and the exclusion tag. */
    public static boolean isEligible(TamableAnimal pet) {
        if (!Config.PET_MODES_ENABLED.get()) {
            return false;
        }
        EntityType<?> type = pet.getType();
        if (isExcludedType(type)) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        boolean vanilla = "minecraft".equals(id.getNamespace());
        if (vanilla ? !Config.PET_INCLUDE_VANILLA.get() : !Config.PET_INCLUDE_MODDED.get()) {
            return false;
        }
        return !Config.PET_EXCLUDED.get().contains(id.toString());
    }
}
