package com.hacklesgaming.spookylandsrevived.pet;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.hacklesgaming.spookylandsrevived.Config;
import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Wander mode for tamable pets (vanilla and other mods): sneak-click your pet to make it
 * wander near where it is instead of following you; sneak-click again to make it follow.
 * Plain clicks are left alone (vanilla's sit toggle etc.) unless requireSneak is off.
 */
@EventBusSubscriber(modid = SpookyLandsRevived.MOD_ID)
public class PetModeEvents {
    /** Modded pet types already reported in the log. */
    private static final Set<Identifier> LOGGED = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof TamableAnimal pet)) {
            return;
        }
        if (PetModes.isExcludedType(pet.getType())) {
            return;
        }
        boolean has = pet.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof WanderNearHomeGoal);
        if (!has) {
            pet.goalSelector.addGoal(1, new WanderNearHomeGoal(pet, 1.0D));
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType());
        if (!"minecraft".equals(id.getNamespace()) && LOGGED.add(id)) {
            SpookyLandsRevived.LOGGER.info(
                    "Pet wander mode is available for {} (add it to pet_modes.excludedPets in the config to turn it off)", id);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getTarget() instanceof TamableAnimal pet)) {
            return;
        }
        Player player = event.getEntity();
        if (!pet.isTame() || !pet.isOwnedBy(player) || !PetModes.isEligible(pet)) {
            return;
        }
        boolean requireSneak = Config.PET_REQUIRE_SNEAK.get();
        if (requireSneak && !player.isShiftKeyDown()) {
            return;
        }
        if (pet.isFood(player.getItemInHand(event.getHand()))) {
            return; // feeding / healing still works
        }

        PetModes.Mode next = switch (PetModes.get(pet)) {
            case WANDER -> PetModes.Mode.FOLLOW;
            case SIT -> PetModes.Mode.WANDER;
            // With requireSneak on, plain clicks keep vanilla's sit toggle, so sneak-click goes
            // straight to wander. With it off, we run the whole cycle: follow -> sit -> wander.
            case FOLLOW -> requireSneak ? PetModes.Mode.WANDER : PetModes.Mode.SIT;
        };

        switch (next) {
            case SIT -> {
                PetModes.clearWander(pet);
                pet.setOrderedToSit(true);
                pet.setTarget(null);
                pet.getNavigation().stop();
            }
            case WANDER -> {
                pet.setOrderedToSit(false);
                pet.setInSittingPose(false);
                PetModes.setWander(pet, pet.blockPosition());
                pet.getNavigation().stop();
            }
            case FOLLOW -> {
                PetModes.clearWander(pet);
                pet.setOrderedToSit(false);
                pet.setInSittingPose(false);
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.sendOverlayMessage(Component.translatable(
                "message.spookylandsrevived.pet_mode." + next.name().toLowerCase(Locale.ROOT),
                pet.getDisplayName()));
    }
}
