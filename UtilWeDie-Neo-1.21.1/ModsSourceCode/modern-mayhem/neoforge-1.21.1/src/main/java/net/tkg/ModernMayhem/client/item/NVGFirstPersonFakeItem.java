package net.tkg.ModernMayhem.client.item;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tkg.ModernMayhem.client.event.RenderNVGFirstPerson;
import net.tkg.ModernMayhem.client.renderer.custom.NVGFirstPersonRenderer;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.network.NVGSyncSwitchOffPacket;
import net.tkg.ModernMayhem.server.network.NVGSyncSwitchOnPacket;
import net.tkg.ModernMayhem.server.registry.SoundRegistryMM;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.animation.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NVGFirstPersonFakeItem
extends Item
implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    public static final NVGFirstPersonRenderer NVG_FIRST_PERSON_RENDERER = new NVGFirstPersonRenderer();
    public static final RawAnimation ANIM_OPENED = RawAnimation.begin().thenLoop("opened");
    public static final RawAnimation ANIM_CLOSED = RawAnimation.begin().thenLoop("closed");

    public NVGFirstPersonFakeItem() {
        super(new Item.Properties());
    }

    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions(){
            private NVGFirstPersonRenderer renderer = null;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new NVGFirstPersonRenderer();
                }
                return this.renderer;
            }
        });
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController controller = new AnimationController((GeoAnimatable)this, 0, state -> {
            ItemStack stack;
            LocalPlayer player = Minecraft.getInstance().player;
            assert (player != null);
            AnimationProcessor.QueuedAnimation currentAnim = state.getController().getCurrentAnimation();
            if (currentAnim == null) {
                if (CuriosUtil.hasNVGEquipped((Player)player)) {
                    ItemStack facewearItem = CuriosUtil.getFaceWearItem((Player)player);
                    if (facewearItem.getItem() instanceof GenericSpecialGogglesItem) {
                        if (GenericSpecialGogglesItem.isNVGOnFace(facewearItem)) {
                            state.setAnimation(ANIM_CLOSED);
                        } else {
                            state.setAnimation(ANIM_OPENED);
                        }
                    } else {
                        state.setAnimation(GenericSpecialGogglesItem.ANIM_IDLE);
                    }
                } else {
                    state.setAnimation(GenericSpecialGogglesItem.ANIM_IDLE);
                }
                return PlayState.CONTINUE;
            }
            if ((state.isCurrentAnimationStage("opened") || state.isCurrentAnimationStage("closed") || state.isCurrentAnimationStage("idle") || state.isCurrentAnimationStage("")) && (stack = CuriosUtil.getFaceWearItem((Player)player)).getItem() instanceof GenericSpecialGogglesItem) {
                if (GenericSpecialGogglesItem.isNVGOnFace(stack)) {
                    state.setAnimation(GenericSpecialGogglesItem.ANIM_CLOSE);
                } else {
                    state.setAnimation(GenericSpecialGogglesItem.ANIM_OPEN);
                }
            }
            if (!(state.isCurrentAnimation(GenericSpecialGogglesItem.ANIM_CLOSE) || state.isCurrentAnimation(GenericSpecialGogglesItem.ANIM_OPEN) || state.isCurrentAnimation(GenericSpecialGogglesItem.ANIM_IDLE))) {
                state.setAnimation(GenericSpecialGogglesItem.ANIM_IDLE);
            }
            RenderNVGFirstPerson.shouldRenderLeftArm = !state.isCurrentAnimationStage("opening") && !state.isCurrentAnimationStage("closing");
            return PlayState.CONTINUE;
        });
        controller.setCustomInstructionKeyframeHandler(event -> {
            CustomInstructionKeyframeData keyframeData = event.getKeyframeData();
            String key = keyframeData.getInstructions();
            LocalPlayer player = Minecraft.getInstance().player;
            ItemStack facewearItem = CuriosUtil.getFaceWearItem((Player)player);
            if (player != null && facewearItem.getItem() instanceof GenericSpecialGogglesItem) {
                if (key.equals("enableNVGEffect;")) {
                    GenericSpecialGogglesItem.switchOnNVGMode(facewearItem);
                    PacketDistributor.sendToServer(new NVGSyncSwitchOnPacket());
                } else if (key.equals("disableNVGEffect;")) {
                    GenericSpecialGogglesItem.switchOffNVGMode(facewearItem);
                    PacketDistributor.sendToServer(new NVGSyncSwitchOffPacket());
                }
            }
        });
        controller.setCustomInstructionKeyframeHandler(event -> {
            CustomInstructionKeyframeData keyframeData = event.getKeyframeData();
            String key = keyframeData.getInstructions();
            LocalPlayer player = Minecraft.getInstance().player;
            ItemStack facewearItem = CuriosUtil.getFaceWearItem((Player)player);
            if (player != null && facewearItem.getItem() instanceof GenericSpecialGogglesItem) {
                if (key.equals("enableNVGEffect;")) {
                    GenericSpecialGogglesItem.switchOnNVGMode(facewearItem);
                    PacketDistributor.sendToServer(new NVGSyncSwitchOnPacket());
                } else if (key.equals("disableNVGEffect;")) {
                    GenericSpecialGogglesItem.switchOffNVGMode(facewearItem);
                    PacketDistributor.sendToServer(new NVGSyncSwitchOffPacket());
                }
            }
        });
        controller.setSoundKeyframeHandler(event -> {
            SoundKeyframeData soundData = event.getKeyframeData();
            String soundKey = soundData.getSound();
            LocalPlayer player = Minecraft.getInstance().player;
            ClientLevel world = Minecraft.getInstance().level;
            if (player != null && world != null) {
                ItemStack facewearItem = CuriosUtil.getFaceWearItem((Player)player);
                Item patt9641$temp = facewearItem.getItem();
                if (patt9641$temp instanceof GenericSpecialGogglesItem) {
                    GenericSpecialGogglesItem genericSpecialGogglesItem = (GenericSpecialGogglesItem)patt9641$temp;
                    switch (soundKey) {
                        case "nvg_on": {
                            world.playSeededSound((Player)player, player.getX(), player.getY(), player.getZ(), (SoundEvent)genericSpecialGogglesItem.ACTIVATION_SOUND.get(), SoundSource.NEUTRAL, 1.0f, 1.0f, 0L);
                            break;
                        }
                        case "nvg_off": {
                            world.playSeededSound((Player)player, player.getX(), player.getY(), player.getZ(), (SoundEvent)genericSpecialGogglesItem.DEACTIVATION_SOUND.get(), SoundSource.NEUTRAL, 1.0f, 1.0f, 0L);
                            break;
                        }
                        case "nvg_equip": {
                            world.playSeededSound((Player)player, player.getX(), player.getY(), player.getZ(), (SoundEvent)SoundRegistryMM.SOUND_NVG_PUT_ON.get(), SoundSource.NEUTRAL, 1.0f, 1.0f, 0L);
                            break;
                        }
                        case "nvg_unequip": {
                            world.playSeededSound((Player)player, player.getX(), player.getY(), player.getZ(), (SoundEvent)SoundRegistryMM.SOUND_NVG_PUT_OFF.get(), SoundSource.NEUTRAL, 1.0f, 1.0f, 0L);
                            break;
                        }
                        default: {
                            System.err.println("Unknown sound key: " + soundKey);
                        }
                    }
                }
            } else {
                System.err.println("Player or world is null, cannot play sound: " + soundKey);
            }
        });
        controllers.add(new AnimationController[]{controller});
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public double getTick(Object object) {
        return Minecraft.getInstance().level != null ? (double)Minecraft.getInstance().level.getGameTime() : 0.0;
    }
}

