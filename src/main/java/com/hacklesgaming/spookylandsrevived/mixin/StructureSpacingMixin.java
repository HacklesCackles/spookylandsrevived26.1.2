package com.hacklesgaming.spookylandsrevived.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hacklesgaming.spookylandsrevived.StructureSpacing;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

// Skips a spawn spot for one of our structures when another of ours is due too close by (see StructureSpacing).
@Mixin(StructurePlacement.class)
public abstract class StructureSpacingMixin {

    @Inject(method = "isStructureChunk", at = @At("RETURN"), cancellable = true)
    private void spookylandsrevived$keepStructuresApart(ChunkGeneratorStructureState state, int sourceX, int sourceZ, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && StructureSpacing.tooClose(this, state, sourceX, sourceZ)) {
            cir.setReturnValue(false);
        }
    }
}
