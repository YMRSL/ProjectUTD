package com.scarasol.zombiekit.block.entity;

import com.scarasol.zombiekit.block.ShortwaveRadioBlock;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitBlockEntities;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.init.ZombieKitTags;
import com.scarasol.zombiekit.inventory.ShortwaveRadioMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ShortwaveRadioBlockEntity extends BlockEntity implements MenuProvider {

    private BlockPos currentPos;
    private int times;
    private String content = Component.translatable("zombiekit.message.response1", getBlockPos().getX(), getBlockPos().getZ()).getString();
    private final Map<Mob, BlockPos> survivorsNeedMove = new HashMap<>();

    public ShortwaveRadioBlockEntity(BlockPos position, BlockState state) {
        super(ZombieKitBlockEntities.SHORTWAVE_RADIO.get(), position, state);
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        compoundTag.putInt("Time", times);
        compoundTag.putString("Content", content);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        if (compoundTag.contains("Time")) {
            times = compoundTag.getInt("Time");
        }
        if (compoundTag.contains("Content")) {
            content = compoundTag.getString("Content");
        }

    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.zombiekit.shortwave_radio");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ShortwaveRadioMenu(id, inventory, getBlockPos());
    }

    public void clearTime() {
        times = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, ShortwaveRadioBlockEntity blockEntity) {
        if (blockState.getValue(ShortwaveRadioBlock.TURN_ON)) {
            if (level.getGameTime() % 20 == 0) {
                level.playSound(null, pos, ZombieKitSounds.radio_static.get(), SoundSource.BLOCKS, 1, 1);
                int time = blockEntity.times;
                RandomSource random = level.random;
                if (level.canSeeSkyFromBelowWater(pos) && level.isDay() && !level.getLevelData().isThundering()){
                    if (time < 600){
                        if (random.nextDouble() < 0.000001 * time){
                            if (blockEntity.spawnSurvivors(level, pos, random))
                                blockEntity.times = 0;
                        }
                        blockEntity.times++;
                    }else if (time < 1200) {
                        if (random.nextDouble() < 0.0006 + 0.000015 * (time - 600)){
                            if (blockEntity.spawnSurvivors(level, pos, random))
                                blockEntity.times = 0;
                        }
                        blockEntity.times++;
                    }else {
                        if (random.nextDouble() < 0.01){
                            if (blockEntity.spawnSurvivors(level, pos, random))
                                blockEntity.times = 0;
                        }
                    }
                }
                blockEntity.setChanged();
            }
            if (blockEntity.currentPos == null) {
                blockEntity.currentPos = pos;
            }else if (!blockEntity.currentPos.equals(pos)) {
                ShortwaveRadioBlock.removeRadio(blockEntity.currentPos, level);
                ShortwaveRadioBlock.addRadio(pos, level);
                blockEntity.currentPos = pos;
            }

        }

        blockEntity.moveSurvivors();
    }

    public boolean spawnSurvivors(Level level, BlockPos pos, RandomSource random){
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int tx;
        int tz;
        int dx;
        int dz;
        int dr;
        for (int index0 = 0; index0 < 100; index0++) {
            dr = Mth.nextInt(random, 48, 64);
            dx = Mth.nextInt(random, 0, dr);
            dr = dr - dx;
            dz = dr;
            if (Mth.nextInt(random, 1, 2) == 1) {
                tx = x + dx;
            } else {
                tx = x - dx;
            }
            if (Mth.nextInt(random, 1, 2) == 1) {
                tz = z + dz;
            } else {
                tz = z - dz;
            }

            BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(tx, y, tz));
            BlockState blockState = level.getBlockState(spawnPos);
            BlockState blockState2 = level.getBlockState(spawnPos.below());
            if (!NaturalSpawner.isValidEmptySpawnBlock(level, spawnPos, blockState, blockState.getFluidState(), EntityType.PILLAGER) && blockState2.isValidSpawn(level, spawnPos.below(), EntityType.VILLAGER)) {
                continue;
            }
            if (spawnPillagers((ServerLevel) level, pos, spawnPos, random)){
                return true;
            }
            Entity entity = BuiltInRegistries.ENTITY_TYPE.getRandomElementOf(ZombieKitTags.SURVIVORS, random)
                    .map(holder -> holder.value().spawn((ServerLevel) level, spawnPos, MobSpawnType.REINFORCEMENT))
                    .orElse(null);
            if (entity instanceof Mob _living){
                survivorsNeedMove.put(_living, pos);
            }
            return true;
        }
        return false;
    }

    public boolean spawnPillagers(ServerLevel level, BlockPos radioPos, BlockPos spawnPos, RandomSource random){
        if (level.getDifficulty() != Difficulty.PEACEFUL){
            if (random.nextDouble() < CommonConfig.ILLAGER_CHANCE.get()){
                for (int i = 0; i < CommonConfig.ILLAGER_NUMBER.get(); i++){
                    PatrollingMonster patrollingMonster;
                    if (random.nextDouble() < CommonConfig.VINDICATOR_CHANCE.get()){
                        patrollingMonster = EntityType.VINDICATOR.create(level);
                    }else {
                        patrollingMonster = EntityType.PILLAGER.create(level);
                    }
                    if (i == 0){
                        patrollingMonster.setPatrolLeader(true);
                        patrollingMonster.setPatrolTarget(radioPos);
                    }
                    patrollingMonster.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                    patrollingMonster.finalizeSpawn(level, level.getCurrentDifficultyAt(radioPos), MobSpawnType.PATROL, null);
                    level.addFreshEntityWithPassengers(patrollingMonster);
                }
                return true;
            }
        }
        return false;
    }

    public void moveSurvivors(){
        if (!survivorsNeedMove.isEmpty()){
            for(Map.Entry<Mob, BlockPos> survivor : survivorsNeedMove.entrySet()){
                int x = survivor.getValue().getX();
                int y = survivor.getValue().getY();
                int z = survivor.getValue().getZ();
                Mob mob = survivor.getKey();
                mob.getNavigation().moveTo(x, y, z, 0.8);
            }
            survivorsNeedMove.clear();
        }
    }
}
