package com.github.nalamodikk.common.register;
/**
 * 📦 NeoForge 1.21.1 GUI 菜單註冊總管
 *
 * 支援：
 * - 普通菜單註冊（無 BlockEntity）
 * - 自動 BlockEntity 解析（id, Inventory, BlockEntity）
 * - 自動 FriendlyByteBuf 傳輸與驗證
 * - 特殊手動菜單（如 UpgradeMenu）
 *
 * 用法範例：
 * registerMenuType("mana_generator", entityMenu(ManaGeneratorBlockEntity.class, ManaGeneratorMenu::new));
 */

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.common.block.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.block.collector.solarmana.SolarManaCollectorMenu;
import com.github.nalamodikk.common.block.mana_crafting.ManaCraftingMenu;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorMenu;
import com.github.nalamodikk.common.screen.shared.FallbackUpgradeMenu;
import com.github.nalamodikk.common.screen.shared.UniversalConfigMenu;
import com.github.nalamodikk.common.screen.shared.UpgradeMenu;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;


public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MagicalIndustryMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ManaCraftingMenu>> MANA_CRAFTING_MENU =
            registerMenuType("mana_crafting", ManaCraftingMenu::create);

    public static final DeferredHolder<MenuType<?>, MenuType<UniversalConfigMenu>> UNIVERSAL_CONFIG_MENU =
            registerMenuType("universal_config", (id, inv, buf) ->
                    new UniversalConfigMenu(id, inv,
                            inv.player.level().getBlockEntity(buf.readBlockPos()),
                            buf.readWithCodec(NbtOps.INSTANCE, ItemStack.CODEC, NbtAccounter.unlimitedHeap()),
                            buf.readWithCodec(NbtOps.INSTANCE, CodecsLibrary.DIRECTION_IOTYPE_MAP, NbtAccounter.unlimitedHeap()) // ✅ 這裡才是正確的讀取
                    )
            );

    public static final DeferredHolder<MenuType<?>, MenuType<ManaGeneratorMenu>> MANA_GENERATOR_MENU =
            registerMenuType("mana_generator_menu", entityMenu(
                    ManaGeneratorBlockEntity.class,
                    ManaGeneratorMenu::new
            ));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarManaCollectorMenu>> SOLAR_MANA_COLLECTOR_MENU =
            registerMenuType("solar_mana_collector", entityMenu(
                    SolarManaCollectorBlockEntity.class,
                    SolarManaCollectorMenu::new
            ));

    public static final DeferredHolder<MenuType<?>, MenuType<UpgradeMenu>> UPGRADE_MENU =
            registerMenuType("upgrade_menu", ModMenuTypes::createUpgradeMenu);




    @SuppressWarnings("resource")
    private static UpgradeMenu createUpgradeMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        Level level = playerInv.player.level();

        if (!level.isLoaded(pos)) {
            MagicalIndustryMod.LOGGER.warn("UpgradeMenu open failed: chunk at {} not loaded.", pos);
            return new FallbackUpgradeMenu(id, playerInv);

        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IUpgradeableMachine machine) {
            return new UpgradeMenu(id, playerInv, machine.getUpgradeInventory(), machine);
        }

        MagicalIndustryMod.LOGGER.warn("UpgradeMenu open failed: BlockEntity at {} is not IUpgradeableMachine.", pos);
        return new FallbackUpgradeMenu(id, playerInv);
    }


    public static <T extends BlockEntity, M extends AbstractContainerMenu> IContainerFactory<M> entityMenu(
            Class<T> entityClass,
            BiFunction<Integer, T, M> constructor
    ) {
        return (id, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (!entityClass.isInstance(be)) {
                throw new IllegalStateException("Expected %s at %s but found %s".formatted(
                        entityClass.getSimpleName(), pos, be != null ? be.getClass().getSimpleName() : "null"));
            }
            return constructor.apply(id, entityClass.cast(be));
        };
    }

    public static <T extends BlockEntity, M extends AbstractContainerMenu> IContainerFactory<M> entityMenu(
            Class<T> entityClass,
            TriFunction<Integer, Inventory, T, M> constructor
    ) {
        return (id, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (!entityClass.isInstance(be)) {
                throw new IllegalStateException("Expected %s at %s but found %s".formatted(
                        entityClass.getSimpleName(), pos, be != null ? be.getClass().getSimpleName() : "null"));
            }
            return constructor.apply(id, inv, entityClass.cast(be));
        };
    }



    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }


}
