package net.tkg.ModernMayhem.server.item;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.registry.SoundRegistryMM;
import net.tkg.ModernMayhem.server.util.NVGConfigs;

public enum NVGGoggleList {
    BLACK_GPNVG(NVGConfigs.GREEN_PHOSPHOR_GPVNG, 2, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 0, 0),
    TAN_GPNVG(NVGConfigs.WHITE_PHOSPHOR_GPVNG, 3, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 0, 1),
    GAMER_GPNVG(NVGConfigs.WHITE_PHOSPHOR_GPVNG, 2, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 0, 99),
    BLACK_PVS14(NVGConfigs.GREEN_PHOSPHOR_PVS14, 1, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 1, 0),
    TAN_PVS14(NVGConfigs.WHITE_PHOSPHOR_PVS14, 1, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 1, 1),
    GREEN_PVS14(NVGConfigs.GREEN_PHOSPHOR_PVS14, 1, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 1, 2),
    BLACK_PVS7(NVGConfigs.GREEN_PHOSPHOR_PVS7, 1, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 2, 0),
    BLACK_VISOR(null, 0, SoundRegistryMM.SOUND_VISOR_CLOSE, SoundRegistryMM.SOUND_VISOR_CLOSE, 3, 0),
    TAN_VISOR(null, 0, SoundRegistryMM.SOUND_VISOR_CLOSE, SoundRegistryMM.SOUND_VISOR_CLOSE, 3, 1),
    BLACK_TVG(NVGConfigs.THERMAL, 0, SoundRegistryMM.SOUND_NVG_ON, SoundRegistryMM.SOUND_NVG_OFF, 4, 0);

    private final GenericSpecialGogglesItem.NVGConfig[] configs;
    private final int configIndex;
    private final DeferredHolder<SoundEvent, SoundEvent> activationSound;
    private final DeferredHolder<SoundEvent, SoundEvent> deactivationSound;
    private final int Type;
    private final int Variant;

    private NVGGoggleList(GenericSpecialGogglesItem.NVGConfig[] pConfigs, int pConfigIndex, DeferredHolder<SoundEvent, SoundEvent> pActivationSound, DeferredHolder<SoundEvent, SoundEvent> pDeactivationSound, int Type2, int Variant) {
        this.configs = pConfigs;
        this.configIndex = pConfigIndex;
        this.activationSound = pActivationSound;
        this.deactivationSound = pDeactivationSound;
        this.Type = Type2;
        this.Variant = Variant;
    }

    public GenericSpecialGogglesItem.NVGConfig[] getConfigs() {
        return this.configs;
    }

    public int getConfigIndex() {
        return this.configIndex;
    }

    public DeferredHolder<SoundEvent, SoundEvent> getActivationSound() {
        return this.activationSound;
    }

    public DeferredHolder<SoundEvent, SoundEvent> getDeactivationSound() {
        return this.deactivationSound;
    }

    public int getType() {
        return this.Type;
    }

    public int getVariant() {
        return this.Variant;
    }

    /**
     * IR 红外照明分档。Type 编码: 0=GPNVG(四镜头), 1=PVS14(双镜头), 2=PVS7(单镜头), 3=Visor, 4=TVG。
     * lumMul/rangeMul 以单镜头为基准单位; 四镜头可主动开关, 单/双镜头常开不可关。
     */
    public enum IrTier {
        NONE(0.0, 0.0),
        SINGLE(1.0, 1.0),
        DUAL(1.5, 1.5),
        QUAD(2.0, 2.0);

        public final double lumMul;
        public final double rangeMul;

        private IrTier(double lumMul, double rangeMul) {
            this.lumMul = lumMul;
            this.rangeMul = rangeMul;
        }
    }

    public IrTier getIrTier() {
        switch (this.Type) {
            case 0: return IrTier.QUAD;
            case 1: return IrTier.DUAL;
            case 2: return IrTier.SINGLE;
            default: return IrTier.NONE;
        }
    }

    /** 仅四镜头 GPNVG 的 IR 可主动开关 (其余常开)。 */
    public boolean isQuad() {
        return this.Type == 0;
    }
}

