package net.mcreator.doomsdaydecoration.functionality;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Lootable container BlockEntity (27 slots), native NeoForge 1.21.1 port of
 * Raiiiden/DoomsdayFunctionality's {@code DoomsdayBlockEntity}.
 *
 * <p>Loot table is derived from the owning block's registry name as
 * {@code doomsday_decoration:chests/<path>}. If that table does not exist the
 * container is simply left empty (never crashes) so KubeJS / datapacks can fill in
 * tables later. Loot re-rolls after {@link #LOOT_REFRESH_DAYS} in-game days.</p>
 *
 * <p>Opening is fully vanilla: this BE is a {@link MenuProvider}, the block layer
 * calls {@code player.openMenu(this)}, and the standard container protocol syncs the
 * slots. No custom packet, no mixin.</p>
 */
public class DoomsdayBlockEntity extends BlockEntity implements MenuProvider {

    /** Loot tables live under data/doomsday_decoration/loot_table/chests/. */
    public static final String LOOT_PREFIX = "chests/";
    /** Days between automatic loot refreshes. Simplified (no config); KJS can re-roll. */
    public static final int LOOT_REFRESH_DAYS = 7;

    private final ItemStackHandler lootHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private boolean filledFromLoot = false;
    private long lastLootDay = -1L;
    @Nullable
    private ResourceLocation lootTable = null;

    public DoomsdayBlockEntity(BlockPos pos, BlockState state) {
        super(ModFunctionality.DOOMSDAY_BE.get(), pos, state);
    }

    public ItemStackHandler getLootHandler() {
        return lootHandler;
    }

    /** Resolve loot table id from the block registry name (once). */
    private ResourceLocation resolveLootTable() {
        if (lootTable != null) return lootTable;
        ResourceLocation blockId = getBlockState().getBlock().builtInRegistryHolder().key().location();
        lootTable = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), LOOT_PREFIX + blockId.getPath());
        return lootTable;
    }

    /**
     * Re-rolls loot if the refresh interval has elapsed (or first ever open).
     * Server-side only. Missing tables leave the container empty without error.
     */
    public void tryLoadLoot(@Nullable Player player) {
        if (!(level instanceof ServerLevel server)) return;

        long currentDay = server.getDayTime() / 24000L;
        if (filledFromLoot && lastLootDay != -1L && currentDay - lastLootDay < LOOT_REFRESH_DAYS) {
            return;
        }

        for (int i = 0; i < lootHandler.getSlots(); i++) {
            lootHandler.setStackInSlot(i, ItemStack.EMPTY);
        }

        ResourceLocation tableId = resolveLootTable();
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, tableId);
        // 1.21.1: loot tables are a reloadable registry on MinecraftServer.
        // getLootTable always returns a table, falling back to LootTable.EMPTY for
        // unknown ids -> empty container (never crashes).
        LootTable table = server.getServer().reloadableRegistries().getLootTable(key);

        if (table != LootTable.EMPTY) {
            LootParams.Builder params = new LootParams.Builder(server)
                    .withParameter(LootContextParams.ORIGIN, getBlockPos().getCenter());
            if (player != null) {
                params.withOptionalParameter(LootContextParams.THIS_ENTITY, player);
            }
            List<ItemStack> items = table.getRandomItems(params.create(LootContextParamSets.CHEST));

            List<Integer> slots = new ArrayList<>(
                    IntStream.range(0, lootHandler.getSlots()).boxed().toList());
            Collections.shuffle(slots, new java.util.Random(server.getRandom().nextLong()));
            for (int i = 0; i < Math.min(slots.size(), items.size()); i++) {
                lootHandler.setStackInSlot(slots.get(i), items.get(i));
            }
        }

        filledFromLoot = true;
        lastLootDay = currentDay;
        setChanged();
    }

    // --- persistence (1.21.1 uses HolderLookup.Provider on save/load) ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Loot", lootHandler.serializeNBT(registries));
        tag.putBoolean("Filled", filledFromLoot);
        tag.putLong("LastLootDay", lastLootDay);
        if (lootTable != null) tag.putString("LootTable", lootTable.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Loot")) lootHandler.deserializeNBT(registries, tag.getCompound("Loot"));
        filledFromLoot = tag.getBoolean("Filled");
        lastLootDay = tag.getLong("LastLootDay");
        if (tag.contains("LootTable")) lootTable = ResourceLocation.parse(tag.getString("LootTable"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        tryLoadLoot(player);
        return new DoomsdayContainerMenu(id, playerInventory, this);
    }

    /**
     * Writes the BlockPos for the client-side menu factory.
     *
     * <p>Mandatory: {@code player.openMenu(MenuProvider)} only ships extra client data
     * via this hook. The client constructor
     * {@link DoomsdayContainerMenu#DoomsdayContainerMenu(int, Inventory, net.minecraft.network.FriendlyByteBuf)}
     * reads this BlockPos to re-resolve the BE. Without it the buffer is empty and the
     * client reads garbage / can't find the BE, so the GUI shows no loot.</p>
     */
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(getBlockPos());
    }
}
