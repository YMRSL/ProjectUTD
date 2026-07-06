package com.scarasol.sona.compat;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * TaCZ 枪械读取桥(消音器判定)。
 *
 * <p>只在确认 TaCZ 在场时被调用——调用方仅在监听到 {@code tacz:bullet} 实体生成后才进入,
 * 子弹存在即 TaCZ 已加载,故 HotSpot 惰性链接此处的 TaCZ 类不会在无 TaCZ 时触发。
 */
public final class TaczLureBridge {

    private TaczLureBridge() {
    }

    /**
     * 枪是否装了消音枪口(枪口配件 id 路径含 {@code "silencer"},如 {@code tacz:muzzle_silencer_*})。
     * 装了消音器则不引怪。任何异常按"未消音"处理,绝不影响开枪流程。
     */
    public static boolean isSilenced(ItemStack gunStack) {
        try {
            if (gunStack.getItem() instanceof IGun iGun) {
                ResourceLocation muzzle = iGun.getAttachmentId(gunStack, AttachmentType.MUZZLE);
                if (muzzle != null && muzzle.getPath().contains("silencer")) {
                    return true;
                }
                ResourceLocation builtin = iGun.getBuiltInAttachmentId(gunStack, AttachmentType.MUZZLE);
                return builtin != null && builtin.getPath().contains("silencer");
            }
        } catch (Throwable ignored) {
            // 读取失败按未消音处理
        }
        return false;
    }
}
