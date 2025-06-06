package com.github.nalamodikk.register;



import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;


public class ModDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KoniavacraftMod.MOD_ID);

    public static final Supplier<AttachmentType<Boolean>> NARA_BOUND =
            ATTACHMENT_TYPES.register("nara_bound", () ->
                    AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
