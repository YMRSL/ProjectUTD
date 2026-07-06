package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.entity.HomemadeBombProyectileEntity;
import net.mcreator.survivalinstinct.entity.MolotovEntity;
import net.mcreator.survivalinstinct.entity.NailProyectileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(bus=EventBusSubscriber.Bus.MOD, modid = "survival_instinct")
public class SurvivalInstinctModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, (String)"survival_instinct");
    public static final DeferredHolder<EntityType<?>, EntityType<HomemadeBombProyectileEntity>> HOMEMADE_BOMB_PROYECTILE = SurvivalInstinctModEntities.register("homemade_bomb_proyectile", EntityType.Builder.<HomemadeBombProyectileEntity>of(HomemadeBombProyectileEntity::new, (MobCategory)MobCategory.MISC).clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<MolotovEntity>> MOLOTOV = SurvivalInstinctModEntities.register("molotov", EntityType.Builder.<MolotovEntity>of(MolotovEntity::new, (MobCategory)MobCategory.MISC).clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<NailProyectileEntity>> NAIL_PROYECTILE = SurvivalInstinctModEntities.register("nail_proyectile", EntityType.Builder.<NailProyectileEntity>of(NailProyectileEntity::new, (MobCategory)MobCategory.MISC).clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
        return REGISTRY.register(registryname, () -> entityTypeBuilder.build(registryname));
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {});
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
    }
}

