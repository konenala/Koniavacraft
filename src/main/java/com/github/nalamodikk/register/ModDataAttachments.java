package com.github.nalamodikk.register;


import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class ModDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KoniavacraftMod.MOD_ID);

    private static final Codec<ItemStack> ITEMSTACK_CODEC_ALLOW_EMPTY =
            ItemStack.OPTIONAL_CODEC;


    public static final Supplier<AttachmentType<Boolean>> NARA_BOUND =
            ATTACHMENT_TYPES.register("nara_bound", () ->
                    AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .copyOnDeath()
                            .build());

    // 飾品裝備本身的附加資料
    public static final Supplier<AttachmentType<NonNullList<ItemStack>>> EXTRA_EQUIPMENT =
            ATTACHMENT_TYPES.register("extra_equipment", () ->
                    AttachmentType.<NonNullList<ItemStack>>builder(() -> NonNullList.withSize(8, ItemStack.EMPTY))
                            .serialize(Codec.list(ITEMSTACK_CODEC_ALLOW_EMPTY).xmap(
                                    list -> {
                                        NonNullList<ItemStack> result = NonNullList.withSize(8, ItemStack.EMPTY);
                                        for (int i = 0; i < Math.min(list.size(), 8); i++) {
                                            result.set(i, list.get(i));
                                        }
                                        return result;
                                    },
                                    list -> {
                                        List<ItemStack> plain = new ArrayList<>(8);
                                        for (int i = 0; i < 8; i++) {
                                            plain.add(i < list.size() ? list.get(i) : ItemStack.EMPTY);
                                        }
                                        return plain;
                                    }
                            ))
                            .copyOnDeath()
                            .build()
            );


// 九格存儲飾品裝備
public static final Supplier<AttachmentType<NonNullList<ItemStack>>> NINE_GRID =
        ATTACHMENT_TYPES.register("nine_grid", () ->
                AttachmentType.<NonNullList<ItemStack>>builder(() -> NonNullList.withSize(9, ItemStack.EMPTY))
                        .serialize(Codec.list(ITEMSTACK_CODEC_ALLOW_EMPTY).xmap(
                                    list -> {
                                        NonNullList<ItemStack> result = NonNullList.withSize(9, ItemStack.EMPTY);
                                        for (int i = 0; i < Math.min(list.size(), 9); i++) {
                                            result.set(i, list.get(i));
                                        }
                                        return result;
                                    },
                                    list -> {
                                        List<ItemStack> plain = new ArrayList<>(9);
                                        for (int i = 0; i < 9; i++) {
                                            plain.add(i < list.size() ? list.get(i) : ItemStack.EMPTY);
                                        }
                                        return plain;
                                    }
                            ))
                            .copyOnDeath() // 可選：死亡是否保留
                            .build()
            );


    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
