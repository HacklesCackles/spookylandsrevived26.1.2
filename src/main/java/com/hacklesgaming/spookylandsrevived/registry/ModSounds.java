package com.hacklesgaming.spookylandsrevived.registry;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mod sound events. Which .ogg files each one plays is set in
 * assets/spookylandsrevived/sounds.json (files live in assets/spookylandsrevived/sounds/).
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, SpookyLandsRevived.MOD_ID);

    /** Random caws now and then. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_CAW =
            SOUND_EVENTS.register("entity.raven.caw", SoundEvent::createVariableRangeEvent);
    /** Short squawk when hurt. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_HURT =
            SOUND_EVENTS.register("entity.raven.hurt", SoundEvent::createVariableRangeEvent);
}
