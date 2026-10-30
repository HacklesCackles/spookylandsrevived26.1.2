package com.hacklesgaming.spookylandsrevived.mixin;

import com.hacklesgaming.spookylandsrevived.trade.GuaranteedTrades;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a villager rolls its random trades for a new level, also give it the
 * mod's guaranteed trades for that profession and level (see GuaranteedTrades).
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin {

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void spookylandsrevived$addGuaranteedTrades(ServerLevel level, CallbackInfo ci) {
        GuaranteedTrades.addTo((Villager) (Object) this, level);
    }
}
