package de.bene2212.holdmyitems.util;

import de.bene2212.holdmyitems.config.HoldMyItemsClientConfig;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;

public class RenderHelper {
    private static final Set<String> DENIED_NAMESPACES = Set.of("laserio");
    private static final Set<String> DENIED_ITEMS = Set.of("blue_skies:bluebright_chest", "blue_skies:starlit_chest", "blue_skies:frostbright_chest", "blue_skies:comet_chest", "blue_skies:lunar_chest", "blue_skies:dusk_chest", "blue_skies:maple_chest", "draconicevolution:basic_io_crystal", "draconicevolution:basic_relay_crystal", "draconicevolution:basic_wireless_crystal", "draconicevolution:dislocator_pedestal", "draconicevolution:draconic_io_crystal", "draconicevolution:draconic_relay_crystal", "draconicevolution:draconic_wireless_crystal", "draconicevolution:draconium_chest", "draconicevolution:reactor_core", "draconicevolution:reactor_injector", "draconicevolution:reactor_stabilizer", "draconicevolution:wyvern_io_crystal", "draconicevolution:wyvern_relay_crystal", "draconicevolution:wyvern_wireless_crystal", "bloodmagic:alchemytable", "industrialforegoing:machine_frame_pity", "industrialforegoing:machine_frame_simple", "industrialforegoing:machine_frame_advanced", "industrialforegoing:machine_frame_supreme", "ars_creo:starbuncle_wheel", "productivebees:feeder");

    public static boolean shouldRenderCustom(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof BlockItem)) {
            return false;
        }
        BlockItem blockItem = (BlockItem) item;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return false;
        }
        String namespace = itemId.getNamespace();
        String fullItemId = itemId.toString();
        if (stack.is(Items.CONDUIT) || item instanceof SignItem || item instanceof HangingSignItem || namespace.equals("sophisticatedstorage") || HoldMyItemsClientConfig.isInRenderBlockAsItem(itemId.toString())) {
            return false;
        }
        Block block = blockItem.getBlock();
        if (!(block instanceof EntityBlock)) {
            return true;
        }
        // BlockEntityRenderer-only blocks (RenderShape ENTITYBLOCK_ANIMATED / INVISIBLE)
        // ship just a particle-stub baked model, so HMI's renderSingleBlockEmission draws
        // nothing for them (e.g. TaCZ gun smith tables -> empty in first person). These
        // carry their own item renderer (builtin/entity BEWLR), so fall back to vanilla
        // item rendering which uses it. Skull/chest/ender chest/shulker/bed/bell are BER
        // blocks HMI already draws explicitly in its custom path -- leave those alone.
        boolean hmiHandlesExplicitly = block instanceof SkullBlock || block instanceof ChestBlock
                || block instanceof EnderChestBlock || block instanceof ShulkerBoxBlock
                || block instanceof BedBlock || block instanceof BellBlock;
        if (!hmiHandlesExplicitly && block.defaultBlockState().getRenderShape() != RenderShape.MODEL) {
            return false;
        }
        return !DENIED_NAMESPACES.contains(namespace) && !DENIED_ITEMS.contains(fullItemId);
    }
}
