package com.hacklesgaming.spookylandsrevived.registry;

import java.util.function.Supplier;

import com.hacklesgaming.spookylandsrevived.SpookyLandsRevived;
import com.hacklesgaming.spookylandsrevived.pet.PetModeData;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SpookyLandsRevived.MOD_ID);

    /** Wander-mode flag and home spot, saved with any tamable pet. */
    public static final Supplier<AttachmentType<PetModeData>> PET_MODE = ATTACHMENT_TYPES.register("pet_mode",
            () -> AttachmentType.builder(() -> PetModeData.DEFAULT).serialize(PetModeData.CODEC).build());
}
