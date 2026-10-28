package com.hacklesgaming.spookylandsrevived.pet;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

/**
 * Saved with the pet (as a NeoForge data attachment): whether it's in "wander" mode and the
 * spot it wanders around. Sit and follow aren't stored here, they come from the pet itself
 * (see {@link PetModes#get}).
 */
public record PetModeData(boolean wandering, Optional<BlockPos> home) {
    public static final PetModeData DEFAULT = new PetModeData(false, Optional.empty());

    public static final MapCodec<PetModeData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("wandering", false).forGetter(PetModeData::wandering),
            BlockPos.CODEC.optionalFieldOf("home").forGetter(PetModeData::home)
    ).apply(instance, PetModeData::new));
}
