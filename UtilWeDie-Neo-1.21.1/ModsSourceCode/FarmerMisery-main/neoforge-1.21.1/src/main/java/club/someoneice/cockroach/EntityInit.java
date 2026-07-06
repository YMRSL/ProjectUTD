package club.someoneice.cockroach;

import club.someoneice.cockroach.entity.BottleEntityRoach;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ModMain.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BottleEntityRoach>> ROACH =
            ENTITIES.register("roach_bottle", () -> EntityType.Builder.<BottleEntityRoach>of(BottleEntityRoach::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                    .build("roach_bottle"));
}
