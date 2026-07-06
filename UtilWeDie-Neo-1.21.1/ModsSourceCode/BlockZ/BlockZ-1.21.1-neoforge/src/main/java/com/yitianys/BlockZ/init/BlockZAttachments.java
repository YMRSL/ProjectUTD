package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 玩家背包数据附件（Data Attachment）注册。
 * 取代 Forge 时代的 PlayerBackpackProvider + AttachCapabilitiesEvent。
 */
public class BlockZAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BlockZ.MODID);

    public static final Supplier<AttachmentType<PlayerBackpack>> PLAYER_BACKPACK =
            ATTACHMENTS.register("player_backpack",
                    () -> AttachmentType.serializable(PlayerBackpack::new).copyOnDeath().build());
}
