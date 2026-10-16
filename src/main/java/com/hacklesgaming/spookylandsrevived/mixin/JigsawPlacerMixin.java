package com.hacklesgaming.spookylandsrevived.mixin;

import com.hacklesgaming.spookylandsrevived.GallowsInjector;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * Limits the village gallows to one per structure. Each time the jigsaw placer asks a pool for its
 * candidate pieces, the gallows is removed from the list if this structure already contains one.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public abstract class JigsawPlacerMixin {

    @Shadow
    @Final
    private List<? super PoolElementStructurePiece> pieces;

    @Redirect(
            method = "tryPlacingChildren",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getShuffledTemplates(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"
            )
    )
    private List<StructurePoolElement> spookylandsrevived$limitGallows(StructureTemplatePool pool, RandomSource random) {
        List<StructurePoolElement> candidates = pool.getShuffledTemplates(random);

        boolean hasGallows = false;
        for (Object piece : this.pieces) {
            if (piece instanceof PoolElementStructurePiece p && GallowsInjector.isGallows(p.getElement())) {
                hasGallows = true;
                break;
            }
        }
        if (!hasGallows) return candidates;

        List<StructurePoolElement> filtered = new ArrayList<>(candidates);
        filtered.removeIf(GallowsInjector::isGallows);
        return filtered;
    }
}
