package com.utdpatch.doomsday.mixin;

import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {GunSmithTableBlockEntity.class},
   remap = false
)
public abstract class TaczWorkbenchBlockEntityNameMixin implements Nameable {
   private static final ResourceLocation OLD_WORKBENCH = ResourceLocation.fromNamespaceAndPath("hamster", "oldworkbench");
   private static final ResourceLocation SMITH_TABLE = ResourceLocation.fromNamespaceAndPath("lrtactical", "smith_table");
   @Shadow
   private ResourceLocation id;

   @Inject(
      method = {"getDisplayName"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void utd$displayVariantName(CallbackInfoReturnable<Component> var1) {
      Component var2 = this.targetDisplayName();
      if (var2 != null) {
         var1.setReturnValue(var2);
      }
   }

   public Component getName() {
      Component var1 = this.targetDisplayName();
      return (Component)(var1 != null ? var1 : Component.literal("Gun Smith Table"));
   }

   public boolean hasCustomName() {
      return this.targetDisplayName() != null;
   }

   public Component getCustomName() {
      return this.targetDisplayName();
   }

   private Component targetDisplayName() {
      ResourceLocation var1 = this.resolveBlockId();
      if (OLD_WORKBENCH.equals(var1)) {
         return Component.translatable("hamster.block.oldworkbench", new Object[0]);
      } else {
         return SMITH_TABLE.equals(var1) ? Component.translatable("lrtactical.block.smith_table.name", new Object[0]) : null;
      }
   }

   private ResourceLocation resolveBlockId() {
      if (isTargetBlockId(this.id)) {
         return this.id;
      } else {
         BlockEntity var1 = (BlockEntity)(Object)this;
         Level var2 = var1.getLevel();
         if (var2 == null) {
            return this.id;
         } else {
            BlockState var3 = var1.getBlockState();
            if (var3 == null) {
               return this.id;
            } else if (var3.getBlock() instanceof AbstractGunSmithTableBlock var5) {
               BlockPos var6 = var1.getBlockPos();
               BlockPos var7 = var5.getRootPos(var6, var3);
               if (var7 != null && !var7.equals(var6)) {
                  return var2.getBlockEntity(var7) instanceof GunSmithTableBlockEntity var9 && isTargetBlockId(var9.getId()) ? var9.getId() : this.id;
               } else {
                  return this.id;
               }
            } else {
               return this.id;
            }
         }
      }
   }

   private static boolean isTargetBlockId(ResourceLocation var0) {
      return OLD_WORKBENCH.equals(var0) || SMITH_TABLE.equals(var0);
   }
}
