package net.tkg.ModernMayhem.server.registry;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tkg.ModernMayhem.server.network.NVGAutoGainTogglePacket;
import net.tkg.ModernMayhem.server.network.NVGCotiTogglePacket;
import net.tkg.ModernMayhem.server.network.NVGIrTogglePacket;
import net.tkg.ModernMayhem.server.network.NVGTubeGainDownPacket;
import net.tkg.ModernMayhem.server.network.NVGTubeGainUpPacket;
import net.tkg.ModernMayhem.server.network.OpenBackpackKeyPacket;
import net.tkg.ModernMayhem.server.network.OpenRigKeyPacket;
import net.tkg.ModernMayhem.server.network.SwitchNVGStatusPacket;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

@OnlyIn(value=Dist.CLIENT)
public class KeyMappingRegistryMM {
    public static final String CATEGORY = "key.categories.mm";
    public static final KeyMapping TOGGLE_NVG_KEY = new KeyMapping("key.mm.toggle_nvg", 78, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasNVGEquipped((Player)Minecraft.getInstance().player)) {
                PacketDistributor.sendToServer(new SwitchNVGStatusPacket());
            }
            this.isDownOld = isDown;
        }
    };
    public static final KeyMapping INCREASE_TUBE_GAIN_KEY = new KeyMapping("key.mm.increase_tube_gain", 265, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasNVGEquipped((Player)Minecraft.getInstance().player)) {
                PacketDistributor.sendToServer(new NVGTubeGainUpPacket());
            }
            this.isDownOld = isDown;
        }
    };
    public static final KeyMapping DECREASE_TUBE_GAIN_KEY = new KeyMapping("key.mm.decrease_tube_gain", 264, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasNVGEquipped((Player)Minecraft.getInstance().player)) {
                PacketDistributor.sendToServer(new NVGTubeGainDownPacket());
            }
            this.isDownOld = isDown;
        }
    };
    public static final KeyMapping TOGGLE_AUTO_GAIN_KEY = new KeyMapping("key.mm.toggle_auto_gain", 263, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasNVGEquipped((Player)Minecraft.getInstance().player)) {
                PacketDistributor.sendToServer(new NVGAutoGainTogglePacket());
            }
            this.isDownOld = isDown;
        }
    };
    public static final KeyMapping OPEN_BACKPACK_KEY = new KeyMapping("key.mm.open_backpack", 66, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            LocalPlayer player;
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasBackpackEquipped((Player)(player = Minecraft.getInstance().player))) {
                PacketDistributor.sendToServer(new OpenBackpackKeyPacket());
            }
            this.isDownOld = isDown;
        }
    };
    public static final KeyMapping OPEN_RIG_KEY = new KeyMapping("key.mm.open_rig", 48, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            LocalPlayer player;
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasRigEquipped((Player)(player = Minecraft.getInstance().player))) {
                PacketDistributor.sendToServer(new OpenRigKeyPacket());
            }
            this.isDownOld = isDown;
        }
    };
    public static final KeyMapping TOGGLE_COTI_KEY = new KeyMapping("key.mm.toggle_coti", 262, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasNVGEquipped((Player)Minecraft.getInstance().player)) {
                PacketDistributor.sendToServer(new NVGCotiTogglePacket());
            }
            this.isDownOld = isDown;
        }
    };
    // 主动红外照明开关; 默认 L (76, light/illuminator), 避开 DayZ 占用的 I。可在控制设置里改键。
    // 仅四镜头 GPNVG 的 IR 可主动开关; 服务端会再校验, 单/双镜头按了无效。
    public static final KeyMapping TOGGLE_IR_KEY = new KeyMapping("key.mm.toggle_ir", 76, "key.categories.mm"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown && CuriosUtil.hasNVGEquipped((Player)Minecraft.getInstance().player)) {
                PacketDistributor.sendToServer(new NVGIrTogglePacket());
            }
            this.isDownOld = isDown;
        }
    };
}

