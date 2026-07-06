package net.tkg.ModernMayhem.server.registry;

import net.minecraft.world.item.Item;
import net.tkg.ModernMayhem.client.renderer.curios.back.BackpackRenderer;
import net.tkg.ModernMayhem.client.renderer.curios.body.BandoleerRenderer;
import net.tkg.ModernMayhem.client.renderer.curios.body.HexagonRigRenderer;
import net.tkg.ModernMayhem.client.renderer.curios.body.PlateCarrierRenderer;
import net.tkg.ModernMayhem.client.renderer.curios.body.ReconRigRenderer;
import net.tkg.ModernMayhem.client.renderer.curios.facewear.GenericSpecialGogglesRenderer;
import net.tkg.ModernMayhem.client.renderer.curios.head.HeadGearRenderer;
import net.tkg.ModernMayhem.client.renderer.curios.knee.KneepadRenderer;
import net.tkg.ModernMayhem.server.registry.ItemRegistryMM;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public class CuriosRendererRegistryMM {
    public static void register() {
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_GPNVG.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_GPNVG.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_PVS14.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_PVS14.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.GREEN_PVS14.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.ULTRA_GAMER_GPNVG.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_PVS7.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_TVG.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_VISOR.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_VISOR.get()), GenericSpecialGogglesRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BALACLAVA.get()), HeadGearRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_GLASSES.get()), HeadGearRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_GOGGLES.get()), HeadGearRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_HEADSET.get()), HeadGearRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_MILITARY_BALACLAVA.get()), HeadGearRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.GP5_GAS_MASK.get()), HeadGearRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_BACKPACK_T1.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_BACKPACK_T2.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_BACKPACK_T3.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.GREEN_BACKPACK_T1.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.GREEN_BACKPACK_T2.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.GREEN_BACKPACK_T3.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_BACKPACK_T1.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_BACKPACK_T2.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_BACKPACK_T3.get()), BackpackRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_PLATE_CARRIER.get()), PlateCarrierRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_PLATE_CARRIER_AMMO.get()), PlateCarrierRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_PLATE_CARRIER_POUCHES.get()), PlateCarrierRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_PLATE_CARRIER.get()), PlateCarrierRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_PLATE_CARRIER_AMMO.get()), PlateCarrierRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_PLATE_CARRIER_POUCHES.get()), PlateCarrierRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_BANDOLEER.get()), BandoleerRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.GREEN_RECON.get()), ReconRigRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.HEXAGON_RIG.get()), HexagonRigRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.BLACK_KNEE_PADS.get()), KneepadRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.GREEN_KNEE_PADS.get()), KneepadRenderer::new);
        CuriosRendererRegistry.register((Item)((Item)ItemRegistryMM.TAN_KNEE_PADS.get()), KneepadRenderer::new);
    }
}

