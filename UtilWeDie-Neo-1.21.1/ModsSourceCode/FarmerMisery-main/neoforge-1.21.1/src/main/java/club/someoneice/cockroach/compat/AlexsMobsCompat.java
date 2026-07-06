package club.someoneice.cockroach.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Soft, reflection-free-where-possible integration with Alex's Mobs.
 * <p>
 * The original 1.20.1 mod compiled directly against Alex's Mobs
 * ({@code AMEntityRegistry.COCKROACH} / {@code EntityCockroach}). Alex's Mobs has no
 * NeoForge 1.21.1 build available to compile against, so this mod treats it as an
 * <b>optional runtime dependency</b>: the cockroach-spawning / bottle-catching features
 * activate only when the {@code alexsmobs} mod is present and exposes a {@code cockroach}
 * entity type. When it is absent, the food items, brewing and the throwable bottle all
 * still work (the bottle simply breaks like a splash potion without spawning a roach).
 */
public final class AlexsMobsCompat {
    public static final String ALEXS_MOBS_ID = "alexsmobs";
    // Alex's Mobs registers its cockroach under this id (alexsmobs:cockroach).
    private static final ResourceLocation COCKROACH_ID =
            ResourceLocation.fromNamespaceAndPath(ALEXS_MOBS_ID, "cockroach");

    private static Boolean loaded;

    private AlexsMobsCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded(ALEXS_MOBS_ID);
        }
        return loaded;
    }

    /**
     * Looks up Alex's Mobs' cockroach {@link EntityType} from the live registry.
     * Returns {@code null} when Alex's Mobs is not installed.
     */
    public static EntityType<?> getCockroachType(Level level) {
        if (!isLoaded()) {
            return null;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
        return registry.get(COCKROACH_ID);
    }

    /** True when the given entity is an Alex's Mobs cockroach. */
    public static boolean isCockroach(Entity entity) {
        if (entity == null || !isLoaded()) {
            return false;
        }
        EntityType<?> type = getCockroachType(entity.level());
        return type != null && entity.getType() == type;
    }

    /**
     * Spawns an Alex's Mobs cockroach at the given position, if Alex's Mobs is present.
     *
     * @return true if a cockroach was spawned.
     */
    public static boolean spawnCockroach(Level level, double x, double y, double z) {
        EntityType<?> type = getCockroachType(level);
        if (type == null) {
            return false;
        }
        Entity roach = type.create(level);
        if (roach == null) {
            return false;
        }
        roach.setPos(x, y, z);
        return level.addFreshEntity(roach);
    }
}
