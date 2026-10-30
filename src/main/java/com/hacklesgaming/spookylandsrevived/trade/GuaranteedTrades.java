package com.hacklesgaming.spookylandsrevived.trade;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Trades a villager always gets at a level, on top of the 2 it rolls from the vanilla pool.
 *
 * Trades are data files (data/spookylandsrevived/villager_trade/...) listed in a villager_trade tag:
 *   vanilla professions: data/spookylandsrevived/tags/villager_trade/guaranteed/<profession>/level_<n>.json
 *   modded professions:  data/spookylandsrevived/tags/villager_trade/guaranteed/<namespace>/<profession>/level_<n>.json
 * Levels: 1 novice, 2 apprentice, 3 journeyman, 4 expert, 5 master.
 *
 * Only applies when a villager reaches a level, so villagers that already passed that level keep their trades.
 */
public final class GuaranteedTrades {

    private GuaranteedTrades() {}

    public static void addTo(Villager villager, ServerLevel level) {
        VillagerData data = villager.getVillagerData();
        Optional<ResourceKey<VillagerProfession>> professionKey = data.profession().unwrapKey();
        if (professionKey.isEmpty()) return;

        HolderSet.Named<VillagerTrade> trades = level.registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE)
                .get(tagFor(professionKey.get().identifier(), data.level()))
                .orElse(null);
        if (trades == null || trades.size() == 0) return;

        // Same loot context vanilla uses when rolling trades
        LootContext lootContext = new LootContext.Builder(
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, villager.position())
                        .withParameter(LootContextParams.THIS_ENTITY, villager)
                        .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                        .create(LootContextParamSets.VILLAGER_TRADE))
                .create(Optional.empty());

        MerchantOffers offers = villager.getOffers();
        for (Holder<VillagerTrade> trade : trades) {
            MerchantOffer offer = trade.value().getOffer(lootContext);
            if (offer != null && !alreadyOffered(offers, offer)) {
                offers.add(offer);
            }
        }
    }

    private static TagKey<VillagerTrade> tagFor(Identifier profession, int villagerLevel) {
        String name = profession.getNamespace().equals("minecraft")
                ? profession.getPath()
                : profession.getNamespace() + "/" + profession.getPath();
        return TagKey.create(Registries.VILLAGER_TRADE, Identifier.fromNamespaceAndPath(
                SpookyLandsRevived.MOD_ID, "guaranteed/" + name + "/level_" + villagerLevel));
    }

    /** Skip a trade the villager already has (e.g. a datapack also put it in the vanilla pool). */
    private static boolean alreadyOffered(MerchantOffers offers, MerchantOffer offer) {
        for (MerchantOffer existing : offers) {
            if (existing.getItemCostA().item().equals(offer.getItemCostA().item())
                    && ItemStack.isSameItemSameComponents(existing.getResult(), offer.getResult())) {
                return true;
            }
        }
        return false;
    }
}
