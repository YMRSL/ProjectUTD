package com.scarasol.sona.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.scarasol.sona.accessor.IGasMask;
import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.event.SonaEventHooks;
import com.scarasol.sona.init.SonaDamageTypes;
import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.sona.init.SonaTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.AttachedToLeavesDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.*;

public class InfectionManager {

    public static final Map<TreeConfiguration, TreeConfiguration> TREE_FEATURE = Maps.newHashMap();
    public static final Map<ColorType, Vec3> COLOR_MAP = Maps.newHashMap();
    public static final List<Tuple<ResourceLocation, Double>> ZOMBIES = Lists.newArrayList();
    public static double WEIGHT_SUM = -1;
    public static final Map<ResourceLocation, ResourceLocation> SPECIFIC_ZOMBIES = Maps.newHashMap();
    // 复用的 RandomSource 实例，避免在 turnZombie/onUseItem 中每次 new Random()
    private static final RandomSource RANDOM = RandomSource.create();

    public static final AttachedToLeavesDecorator INFECTION_DECORATOR = new AttachedToLeavesDecorator(
            0.25F,
            0, 0,
            BlockStateProvider.simple(Blocks.IRON_BLOCK),
            1,
            Direction.Plane.HORIZONTAL.stream().toList()
    );

    public static float getInfection(ILivingEntityAccessor livingEntity) {
        return livingEntity.getInfectionLevel();
    }

    public static void setInfection(ILivingEntityAccessor livingEntity, float infection) {
        livingEntity.setInfectionLevel(infection);
    }

    public static void addInfection(ILivingEntityAccessor livingEntity, float addition) {
        if (addition > 0) {
            addition = addition * CommonConfig.INFECTION_WEIGHT.get().floatValue();
        }
        addActualInfection(livingEntity, addition);
    }

    public static void addActualInfection(ILivingEntityAccessor livingEntity, float addition) {
        float infection = addition > 0 ? Math.min(100, addition + getInfection(livingEntity)) : Math.max(0, addition + getInfection(livingEntity));
        setInfection(livingEntity, infection);
    }

    public static void init(ILivingEntityAccessor newPlayer, ILivingEntityAccessor oldPlayer) {
        newPlayer.setInfectionLevel(Math.min(oldPlayer.getInfectionLevel(), CommonConfig.INFECTION_INITIAL_VALUE.get().floatValue()));
    }

    public static boolean canBeInfected(LivingEntity entity) {
        return (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) || CommonConfig.SUSCEPTIBLE_POPULATION.get().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    public static double canInfect(Entity entity) {
        List<String> entityList;
        if (entity instanceof LivingEntity) {
            entityList = CommonConfig.INFECTION_SOURCE_MOB.get();
        } else {
            entityList = CommonConfig.INFECTION_SOURCE_PROJECTILE.get();
        }
        int index = CommonConfig.findIndex(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entityList);
        if (index == -1) {
            // 用户定制：按名字匹配的感染源。主要服务 CNPC 这种共用 "customnpcs:customnpc" 实体类型、
            // 无法用上面的实体类型列表区分的怪（如 Fungal 系列，名字均含 "Fungal"）。仅对活体怪生效。
            if (entity instanceof LivingEntity) {
                double nameWeight = nameInfectionWeight(entity);
                if (nameWeight != -1) {
                    return nameWeight;
                }
            }
            if (entity.getType().is(EntityTypeTags.UNDEAD)) {
                return 1;
            } else {
                return index;
            }
        } else {
            String[] info = entityList.get(index).split(",");
            if (info.length < 2) {
                return 1;
            }
            return Double.parseDouble(info[1].trim());
        }
    }

    /**
     * 用户定制：按名字(子串、大小写不敏感)匹配感染源，返回感染权重；无匹配返回 -1。
     * 仅用原版 {@link Entity#getName()}，不硬依赖 CustomNPC。配置见 INFECTION_SOURCE_MOB_NAME。
     */
    private static double nameInfectionWeight(Entity entity) {
        String name = entity.getName().getString();
        if (name.isEmpty()) {
            return -1;
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        for (String entry : CommonConfig.INFECTION_SOURCE_MOB_NAME.get()) {
            String[] info = entry.split(",");
            String key = info[0].trim();
            if (key.isEmpty()) {
                continue;
            }
            if (lower.contains(key.toLowerCase(java.util.Locale.ROOT))) {
                if (info.length < 2) {
                    return 1;
                }
                try {
                    return Double.parseDouble(info[1].trim());
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        }
        return -1;
    }

    public static void infectionTick(LivingEntity livingEntity) {
        Level level = livingEntity.level();
        long gameTime = level.getGameTime() + livingEntity.getId();
        if (canBeInfected(livingEntity) && livingEntity instanceof ILivingEntityAccessor livingEntityAccessor) {
            if (CommonConfig.INFECTION_NATURAL_GROWTH.get()) {
                // 用户定制：感染值随时间自然增长，越高越快。感染>10 开始增长；25~75 之间每 10 点提一档速度，
                // <=25 用基础间隔、>=75 用最快间隔。免疫(IMMUNITY)期间不增长。
                float infection = livingEntityAccessor.getInfectionLevel();
                if (hasHealthyEffect(livingEntity)) {
                    // 用户定制：FPE 健康buff(维生素)生效期间，感染增长速度恒为 0(不增长)；
                    // 且感染 > 60 时反而每 INFECTION_HEALTHY_DECAY_INTERVAL tick 降低 1。
                    if (infection > 60) {
                        int decay = CommonConfig.INFECTION_HEALTHY_DECAY_INTERVAL.get();
                        if (decay > 0 && gameTime % decay == 0) {
                            addInfection(livingEntityAccessor, -1);
                        }
                    }
                } else if (infection > 10 && !livingEntity.hasEffect(SonaMobEffects.IMMUNITY)) {
                    int interval = infectionGrowthInterval(infection);
                    if (interval > 0 && gameTime % interval == 0) {
                        addInfection(livingEntityAccessor, 1);
                    }
                }
            } else if (gameTime % 1600 == 0) {
                switch (level.getDifficulty()) {
                    case PEACEFUL:
                        addInfection(livingEntityAccessor, -5);
                        break;
                    case EASY:
                        if (livingEntityAccessor.getInfectionLevel() < 40) {
                            addInfection(livingEntityAccessor, -2.5f);
                        }
                        break;
                    case NORMAL:
                        if (livingEntityAccessor.getInfectionLevel() < 40) {
                            addInfection(livingEntityAccessor, -2.5f);
                        }
                        if (livingEntityAccessor.getInfectionLevel() > 70 && !livingEntity.hasEffect(SonaMobEffects.IMMUNITY)) {
                            addInfection(livingEntityAccessor, 1);
                        }
                        break;
                    case HARD:
                        if (livingEntityAccessor.getInfectionLevel() > 40 && !livingEntity.hasEffect(SonaMobEffects.IMMUNITY)) {
                            addInfection(livingEntityAccessor, 1);
                        }
                        break;
                }
            }
        }
        // 用户定制：给高感染玩家打/撤计分板 tag，供 Fungal 的 CNPC 脚本读取后跳过索敌(被攻击的复仇逻辑在脚本侧不受影响)。
        updateFungalIgnoreTag(livingEntity);
        if (gameTime % 20 == 0) {
            if (canChunkInfection(level)) {
                infectionChunkTick(livingEntity);
            }
            infectionEffect(livingEntity);
        }

    }

    /**
     * 用户定制：按感染值计算"每 +1 感染所需 tick"。感染 &lt;=25 用基础间隔、&gt;=75 用最快间隔；
     * 25~75 之间每 10 点提一档(共 5 档)，按增长速度(1/interval)线性插值。
     * 配置：INFECTION_GROWTH_BASE_INTERVAL / INFECTION_GROWTH_FAST_INTERVAL。
     */
    /** 高感染玩家所打的计分板 tag，供 Fungal CNPC 脚本读取(o.hasTag('sona_no_fungal_aggro'))后跳过索敌。 */
    public static final String FUNGAL_IGNORE_TAG = "sona_no_fungal_aggro";

    /** 维护「高感染玩家」的计分板 tag：感染 > 阈值时加 tag，否则撤。仅对玩家、仅在 tag 状态变化时改动。 */
    private static void updateFungalIgnoreTag(LivingEntity entity) {
        if (!(entity instanceof Player) || !(entity instanceof ILivingEntityAccessor accessor)) {
            return;
        }
        int threshold = CommonConfig.INFECTION_FUNGAL_IGNORE_THRESHOLD.get();
        boolean shouldTag = threshold >= 0 && accessor.getInfectionLevel() > threshold;
        boolean hasTag = entity.getTags().contains(FUNGAL_IGNORE_TAG);
        if (shouldTag && !hasTag) {
            entity.addTag(FUNGAL_IGNORE_TAG);
        } else if (!shouldTag && hasTag) {
            entity.removeTag(FUNGAL_IGNORE_TAG);
        }
    }

    /** FPE 健康buff(firstpersonfoodeating:healthy)是否生效；按 ResourceLocation 软判定，FPE 缺席返回 false。 */
    private static boolean hasHealthyEffect(LivingEntity entity) {
        return BuiltInRegistries.MOB_EFFECT
                .getHolder(ResourceLocation.parse("firstpersonfoodeating:healthy"))
                .map(entity::hasEffect)
                .orElse(false);
    }

    private static int infectionGrowthInterval(float infection) {
        int base = CommonConfig.INFECTION_GROWTH_BASE_INTERVAL.get();
        int fast = CommonConfig.INFECTION_GROWTH_FAST_INTERVAL.get();
        if (fast > base) {
            fast = base;
        }
        int step = (int) ((infection - 25) / 10);
        if (step < 0) {
            step = 0;
        }
        if (step > 5) {
            step = 5;
        }
        double baseRate = 1.0 / base;
        double fastRate = 1.0 / fast;
        double rate = baseRate + (fastRate - baseRate) * step / 5.0;
        return Math.max(1, (int) Math.round(1.0 / rate));
    }

    protected static void infectionEffect(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide()) {
            return;
        }
        if (livingEntity instanceof ILivingEntityAccessor livingEntityAccessor) {
            if (livingEntityAccessor.getInfectionLevel() >= 100) {
                livingEntity.hurt(SonaDamageTypes.damageSource(livingEntity.level().registryAccess(), SonaDamageTypes.INFECTION), 999999);
            } else if (livingEntityAccessor.getInfectionLevel() > 90) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 25, 0, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 85, 0, false, false));
            } else if (livingEntityAccessor.getInfectionLevel() > 70) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 25, 0, false, false));
            } else if (livingEntityAccessor.getInfectionLevel() > 40) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 25, 0, false, false));
            }
            if (livingEntityAccessor.getInfectionLevel() > 70 && (livingEntity.level().getGameTime() + livingEntity.getId()) % 400 == 0 && livingEntity.getRandom().nextDouble() < 0.3) {

                livingEntity.level().playSound(null, livingEntity, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("minecraft:entity.zombie.ambient")), SoundSource.NEUTRAL, 1, 1);
            }
        }
    }

    public static void infectionChunkTick(LivingEntity livingEntity) {
        if (canInfect(livingEntity) > 0) {
            chunkInfectedEffect(livingEntity);
        } else if (canBeInfected(livingEntity)) {
            chunkSusceptibleEffect(livingEntity);
        }
    }

    protected static void chunkInfectedEffect(LivingEntity livingEntity) {
        Level level = livingEntity.level();

        int infection = getZoneInfection(level, livingEntity.blockPosition(), false);
        if (infection > 75) {
            livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.CAMOUFLAGE, 25, 1, false, false));
            livingEntity.heal(0.5F);
        }
        if (infection > 50) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, false, false));
        }
        if (infection > 30) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, 0, false, false));
        }
    }

    protected static void chunkSusceptibleEffect(LivingEntity livingEntity) {
        Level level = livingEntity.level();
        long gameTime = level.getGameTime() + livingEntity.getId();
        if (gameTime % 20 == 0) {

            int infection = getZoneInfection(level, livingEntity.blockPosition(), false);
            if (infection > 75) {
                livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.EXPOSURE, 25, 1, false, false));
                ItemStack helmet = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
                boolean isGasMask = helmet.is(SonaTags.GAS_MASK) || (helmet.getItem() instanceof IGasMask gasMask && gasMask.isGasMask(livingEntity, helmet));
                if (!isGasMask && !livingEntity.hasEffect(SonaMobEffects.IMMUNITY)) {
                    livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.INFECTION, 25, 1, false, false));
                }
            } else if (infection > 50) {
                ItemStack helmet = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
                boolean isGasMask = helmet.is(SonaTags.GAS_MASK) || (helmet.getItem() instanceof IGasMask gasMask && gasMask.isGasMask(livingEntity, helmet));
                if (!isGasMask && !livingEntity.hasEffect(SonaMobEffects.IMMUNITY)) {
                    livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.INFECTION, 25, 0, false, false));
                }
            }

        }
    }

    public static void turnZombie(LivingEntity livingEntity) {
        // 用户定制：玩家感染死亡时，生成一个 CNPC 克隆体(默认 Fungal Infected，命名为 "fungal <玩家名>")替代原版僵尸。
        if (livingEntity instanceof Player player && CommonConfig.INFECTION_PLAYER_FUNGAL.get()
                && livingEntity.level() instanceof ServerLevel serverLevel) {
            String[] ref = CommonConfig.INFECTION_FUNGAL_CLONE.get().split(",", 2);
            if (ref.length >= 2) {
                try {
                    int tab = Integer.parseInt(ref[0].trim());
                    String cloneName = ref[1].trim();
                    String newName = "fungal " + player.getName().getString();
                    if (com.scarasol.sona.compat.CnpcCompatBridge.spawnClone(
                            serverLevel, player.getX(), player.getY(), player.getZ(), tab, cloneName, newName)) {
                        return; // 已生成 Fungal CNPC，跳过原版僵尸生成
                    }
                } catch (NumberFormatException ignored) {
                    // 配置格式错误则回退原版僵尸逻辑
                }
            }
        }
        double weightSum = getWeightSum();
        double currentWeight = 0;
        double random = livingEntity.getRandom().nextDouble();
        ResourceLocation livingEntityResourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
        ResourceLocation zombieToSpawn = SPECIFIC_ZOMBIES.get(livingEntityResourceLocation);
        if (zombieToSpawn == null) {
            if (weightSum == 0) {
                return;
            }
            for (Tuple<ResourceLocation, Double> tuple : ZOMBIES) {
                currentWeight += tuple.getB();
                if (random < currentWeight / weightSum) {
                    zombieToSpawn = tuple.getA();
                    break;
                }
            }
        }
        if (zombieToSpawn == null) {
            return;
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(zombieToSpawn);
        if (entityType == null) {
            return;
        }
        // 1.21.1: EntityType.create(Level, MobSpawnType) was removed; use create(Level) then finalizeSpawn handles the conversion.
        Entity entityToSpawn = entityType.create(livingEntity.level());

        if (entityToSpawn == null) {
            return;
        }
        entityToSpawn.setPos(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        if (livingEntity.level() instanceof ServerLevel serverLevel && entityToSpawn instanceof Mob mob) {
            mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(entityToSpawn.blockPosition()), MobSpawnType.CONVERSION, null);
            mob.setPersistenceRequired();
        }
        if (entityToSpawn instanceof LivingEntity livingEntityToSpawn) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                livingEntityToSpawn.setItemSlot(slot, livingEntity.getItemBySlot(slot));
                livingEntity.setItemSlot(slot, ItemStack.EMPTY);
                if (entityToSpawn instanceof Mob mob && livingEntity instanceof Player) {
                    mob.setDropChance(slot, 1);
                }
            }
        }

        livingEntity.level().addFreshEntity(entityToSpawn);
    }

    protected static double getWeightSum() {
        if (WEIGHT_SUM == -1) {
            initZombieList();
        }

        return WEIGHT_SUM;
    }

    protected static void initZombieList() {
        double sum = 0;
        for (String str : CommonConfig.ZOMBIE_LIST.get()) {
            String[] buffer = str.split(",");
            if (buffer.length < 2) {
                continue;
            }
            ResourceLocation resourceLocation = ResourceLocation.parse(buffer[0].trim());
            double weight = Double.parseDouble(buffer[1].trim());
            sum += weight;
            ZOMBIES.add(new Tuple<>(resourceLocation, weight));
        }
        WEIGHT_SUM = sum;
        for (String str : CommonConfig.SPECIFIC_ZOMBIE_LIST.get()) {
            String[] buffer = str.split(",");
            if (buffer.length < 2) {
                continue;
            }
            ResourceLocation resourceLocation1 = ResourceLocation.parse(buffer[0].trim());
            ResourceLocation resourceLocation2 = ResourceLocation.parse(buffer[1].trim());
            SPECIFIC_ZOMBIES.put(resourceLocation1, resourceLocation2);
        }
    }

    public static float onAttacked(LivingEntity target, Entity entity, float amount, DamageSource damageSource) {
        Level level = entity.level();
        if (level.isClientSide()) {
            return amount;
        }
        double weight = canInfect(entity);
        if (weight != -1) {
            if (target.hasEffect(SonaMobEffects.IMMUNITY) && entity instanceof LivingEntity livingEntity) {
                immunityEffect(livingEntity, target.getEffect(SonaMobEffects.IMMUNITY).getAmplifier());
            } else {
                if (canBeInfected(target)) {
                    // 用户定制：被感染源(僵尸/Fungal 等)命中时，按护甲点数线性概率决定本次是否
                    // 提供感染值；判定通过则同一次命中一并施加流血(撕裂)。15 甲及以上两者概率均为 0。
                    if (infectionCalculate(target, weight)) {
                        target.addEffect(new MobEffectInstance(SonaMobEffects.LACERATION, 200, 0, false, false));
                    }
                }
            }

        }
        if (target instanceof ILivingEntityAccessor livingEntityAccessor && livingEntityAccessor.isSona$carapace()) {
            if (damageSource.is(DamageTypeTags.IS_FIRE)) {
                livingEntityAccessor.setSona$carapace(false);
            }
            if (amount >= target.getHealth()) {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4));
                livingEntityAccessor.setSona$carapace(false);
                return target.getHealth() - 1;
            }
        }
        return amount;
    }

    /**
     * 用户定制：被感染源命中时，是否提供感染值改为按「护甲点数线性概率」判定：
     * {@code prob = clamp((15 - 护甲点数) / 15, 0, 1)} —— 0 甲必中、7.5 甲半概率、≥15 甲永不感染。
     * 返回本次是否实际加了感染值，供调用方决定是否一并施加流血(撕裂)。
     * 感染「数值」仍沿用原二次方护甲衰减公式。
     */
    protected static boolean infectionCalculate(LivingEntity livingEntity, double weight) {
        double chance = Math.max(0, Math.min(1, (15.0 - livingEntity.getArmorValue()) / 15.0));
        if (livingEntity.getRandom().nextDouble() < chance && livingEntity instanceof ILivingEntityAccessor livingEntityAccessor) {
            addInfection(livingEntityAccessor, (float) (weight * (200 / (Math.pow(livingEntity.getArmorValue(), 2) + 50) + 1)));
            return true;
        }
        return false;
    }

    protected static void immunityEffect(LivingEntity livingEntity, int amplifier) {
        if (amplifier == 1) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
            livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.FRAGILITY, 140, 1));
        } else if (amplifier == 2) {
            livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.FRAGILITY, 140, 3));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 140, 2));
            livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.CONFUSION, 140, 0));
        }
    }

    public static void infectedEntitySpawn(LivingEntity livingEntity) {
        Level level = livingEntity.level();
        if (level.isClientSide()) {
            return;
        }
        if (canChunkInfection(level) && canInfect(livingEntity) > 0) {
            int infection = getZoneInfection(level, livingEntity.blockPosition(), false);
            if (livingEntity instanceof ILivingEntityAccessor livingEntityAccessor && infection > 75) {
                livingEntityAccessor.setSona$carapace(true);
            }
        }
    }

    public static void onUseItem(ILivingEntityAccessor livingEntity, ItemStack itemStack) {
        int index = Math.max(CommonConfig.tagSearch(itemStack, CommonConfig.INFECTION_SOURCE_ITEM.get()), CommonConfig.findIndex(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), CommonConfig.INFECTION_SOURCE_ITEM.get()));
        if (index == -1) {
            return;
        }
        String[] str = CommonConfig.INFECTION_SOURCE_ITEM.get().get(index).split(",");
        if (str.length < 4) {
            return;
        }

        if (RANDOM.nextFloat() < Float.parseFloat(str[1].trim()) / 100) {
            float min = Math.min(Float.parseFloat(str[2].trim()), Float.parseFloat(str[3].trim()));
            float max = Math.max(Float.parseFloat(str[2].trim()), Float.parseFloat(str[3].trim()));
            if (min == max) {
                addInfection(livingEntity, min);
            } else {
                addInfection(livingEntity, min + RANDOM.nextFloat() * (max - min));
            }
            // 用户定制：解毒——抗感染物品(本次净降低感染、即 max<=0)使用时清除中毒/反胃，
            // 给 FPE 药瓶/医疗包(经 Source of Infection Items 配置为负值)与金苹果等带上解毒功能。
            if (max <= 0 && livingEntity instanceof LivingEntity le) {
                le.removeEffect(MobEffects.POISON);
                le.removeEffect(MobEffects.CONFUSION);
            }
        }
    }

    public static boolean blurMessage(float infectionLevel, MutableComponent component) {
        if (infectionLevel > 70) {
            component.withStyle(ChatFormatting.OBFUSCATED);
            return true;
        }
        return false;
    }

    public static boolean canChunkInfection(Level level) {
        return ChunkSectionInfectionManager.getInstance().canChunkInfection(level);
    }

    public static BlockPos calculateZeroZone(BlockPos pos) {
        return ChunkSectionInfectionManager.getInstance().calculateZeroZone(pos);
    }


    public static int initializeInfectionZone(WorldGenLevel level, BlockPos blockPos) {
        return ChunkSectionInfectionManager.getInstance().initializeInfectionZone(level, blockPos);
    }

    public static void calculateInfectionZone(ServerLevel level, ChunkPos chunkPos) {
        ChunkSectionInfectionManager.getInstance().calculateInfectionZone(level, chunkPos);
    }

    public static int getZoneInfection(Level level, BlockPos blockPos, boolean ignoreBlock) {
        return ChunkSectionInfectionManager.getInstance().getZoneInfection(level, blockPos, ignoreBlock);
    }


    public static List<TreeDecorator> addTreeDecorator(ServerLevel serverLevel, BlockPos blockPos) {
        return SonaEventHooks.addTreeDecorator(serverLevel, blockPos, InfectionManager.getZoneInfection(serverLevel, blockPos, true));
    }

    public static boolean canMobSpawn(ServerLevel level, Mob mob, BlockPos blockPos, MobSpawnType spawnType) {
        return ChunkSectionInfectionManager.getInstance().canMobSpawn(level, mob, blockPos, spawnType);
    }


    @OnlyIn(Dist.CLIENT)
    public static Vec3 getFogColor(BlockPos pos, Vec3 oldColor, float particleTick, ClientLevel level) {
        float f = level.getTimeOfDay(particleTick);

        float f1 = Mth.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.5F;
        f1 = Mth.clamp(f1, 0.0F, 1.0F);

        float skyLight = level.getBrightness(LightLayer.SKY, pos) / 15.0F;

        f1 = Math.min(skyLight, f1);

        float f2 = (float) oldColor.x * f1;
        float f3 = (float) oldColor.y * f1;
        float f4 = (float) oldColor.z * f1;
        float f5 = level.getRainLevel(particleTick);
        if (f5 > 0.0F) {
            float f6 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.6F;
            float f7 = 1.0F - f5 * 0.75F;
            f2 = f2 * f7 + f6 * (1.0F - f7);
            f3 = f3 * f7 + f6 * (1.0F - f7);
            f4 = f4 * f7 + f6 * (1.0F - f7);
        }

        float f9 = level.getThunderLevel(particleTick);
        if (f9 > 0.0F) {
            float f10 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.2F;
            float f8 = 1.0F - f9 * 0.75F;
            f2 = f2 * f8 + f10 * (1.0F - f8);
            f3 = f3 * f8 + f10 * (1.0F - f8);
            f4 = f4 * f8 + f10 * (1.0F - f8);
        }

        int i = level.getSkyFlashTime();
        if (i > 0) {
            float f11 = (float) i - particleTick;
            if (f11 > 1.0F) {
                f11 = 1.0F;
            }

            f11 *= 0.45F;
            f2 = f2 * (1.0F - f11) + 0.8F * f11;
            f3 = f3 * (1.0F - f11) + 0.8F * f11;
            f4 = f4 * (1.0F - f11) + f11;
        }

        return new Vec3(f2, f3, f4);
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static Vec3 getColorByType(ColorType colorType) {
        if (COLOR_MAP.isEmpty()) {
            COLOR_MAP.put(ColorType.FOG, parseColor(CommonConfig.INFECTED_ZONE_FOG_COLOR.get()));
            COLOR_MAP.put(ColorType.SKY, parseColor(CommonConfig.INFECTED_ZONE_SKY_COLOR.get()));
            COLOR_MAP.put(ColorType.GRASS, parseColor(CommonConfig.INFECTED_ZONE_GRASS_COLOR.get()));
            COLOR_MAP.put(ColorType.WATER, parseColor(CommonConfig.INFECTED_ZONE_WATER_COLOR.get()));
        }
        return COLOR_MAP.get(colorType);
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static Vec3 parseColor(String color) {
        String[] rgb = color.split(",");
        if (rgb.length < 3) {
            return null;
        }
        return new Vec3(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()));
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static Vec3 getInfectionChunkFogColor(Vec3 oldColor, Vec3 pos, ClientLevel level) {
        Vec3 newColor = getColorByType(ColorType.FOG);
        if (newColor == null) {
            return null;
        }
        return getInfectionChunkColor(oldColor, pos, level, getFogColor(BlockPos.containing(pos), new Vec3(newColor.x / 255D, newColor.y / 255D, newColor.z / 255D), 0, level));
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static Vec3 getInfectionChunkSkyColor(Vec3 oldColor, Vec3 pos, Level level) {
        Vec3 newColor = getColorByType(ColorType.SKY);
        if (newColor == null) {
            return null;
        }
        return getInfectionChunkColor(oldColor, pos, level, new Vec3(newColor.x / 255D, newColor.y / 255D, newColor.z / 255D));
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static Vec3 getInfectionChunkGrassColor(Vec3 oldColor, Vec3 pos, Level level) {
        Vec3 newColor = getColorByType(ColorType.GRASS);
        if (newColor == null) {
            return null;
        }
        return getInfectionChunkColor(oldColor, pos, level, newColor);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static Vec3 getInfectionChunkWaterColor(Vec3 oldColor, Vec3 pos, Level level) {
        Vec3 newColor = getColorByType(ColorType.WATER);
        if (newColor == null) {
            return null;
        }
        return getInfectionChunkColor(oldColor, pos, level, newColor);
    }


    @OnlyIn(Dist.CLIENT)
    public static Vec3 getInfectionChunkColor(Vec3 oldColor, Vec3 pos, Level level, Vec3 newColor) {
        return ChunkSectionInfectionManager.getInstance().getInfectionZoneColor(oldColor, pos, level, newColor);
    }

    @OnlyIn(Dist.CLIENT)
    public static double getAveZoneInfectionInRender(Level level, Vec3 position) {
        return ChunkSectionInfectionManager.getInstance().getAveZoneInfectionInRender(level, position);
    }

    enum ColorType {
        FOG,
        SKY,
        GRASS,
        WATER
    }
}
