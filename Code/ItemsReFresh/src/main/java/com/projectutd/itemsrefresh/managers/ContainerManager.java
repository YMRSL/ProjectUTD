package com.projectutd.itemsrefresh.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Barrel;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.List;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class ContainerManager {
    private final ItemsRefreshPlugin plugin;
    private final Map<Player, Set<Location>> selectedContainers;
    private final Map<Location, ItemStack[]> initialContainerContents;
    private final Map<Player, Boolean> particleEffectsEnabled;
    private final Set<Material> validContainerTypes;
    
    // 存储数据文件中的注册容器
    private final Map<String, Set<Location>> registeredContainers;
    
    // 存储容器的刷新限制配置
    private final Map<Location, RefreshLimitConfig> refreshLimits;
    
    public static class RefreshLimitConfig {
        private int maxItems;        // 最大刷新物品数量
        private long cooldownTime;   // 冷却时间（毫秒）
        private long lastRefreshTime; // 上次刷新时间
        private int currentItems;    // 当前物品数量
        
        public RefreshLimitConfig(int maxItems, long cooldownTime) {
            this.maxItems = maxItems;
            this.cooldownTime = cooldownTime;
            this.lastRefreshTime = 0;
            this.currentItems = 0;
        }
        
        public int getMaxItems() { return maxItems; }
        public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
        
        public long getCooldownTime() { return cooldownTime; }
        public void setCooldownTime(long cooldownTime) { this.cooldownTime = cooldownTime; }
        
        public long getLastRefreshTime() { return lastRefreshTime; }
        public void setLastRefreshTime(long lastRefreshTime) { this.lastRefreshTime = lastRefreshTime; }
        
        public int getCurrentItems() { return currentItems; }
        public void setCurrentItems(int currentItems) { this.currentItems = currentItems; }
        
        // 检查是否可以刷新
        public boolean canRefresh() {
            long currentTime = System.currentTimeMillis();
            // 如果冷却时间已过，重置物品计数
            if (currentTime - lastRefreshTime >= cooldownTime) {
                currentItems = 0;
                return true;
            }
            // 否则检查物品数量是否未达到上限
            return currentItems < maxItems;
        }
        
        // 增加物品计数
        public void incrementItems(int count) {
            this.currentItems += count;
            this.lastRefreshTime = System.currentTimeMillis();
        }
    }
    
    public ContainerManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        this.selectedContainers = new ConcurrentHashMap<>();
        this.initialContainerContents = new ConcurrentHashMap<>();
        this.particleEffectsEnabled = new ConcurrentHashMap<>();
        this.registeredContainers = new ConcurrentHashMap<>();
        this.refreshLimits = new ConcurrentHashMap<>();
        
        // 初始化有效的容器类型
        this.validContainerTypes = new HashSet<>();
        this.validContainerTypes.add(Material.CHEST);
        this.validContainerTypes.add(Material.TRAPPED_CHEST);
        this.validContainerTypes.add(Material.BARREL);
        this.validContainerTypes.add(Material.FURNACE);
        // 注意：ITEM_FRAME和GLOW_ITEM_FRAME是实体，不是方块类型，需要特殊处理
    }
    
    /**
     * 检查一个方块是否是有效的容器
     */
    public boolean isValidContainer(Block block) {
        if (block == null) return false;
        
        BlockState state = block.getState();
        // 对于箱子、木桶和熔炉，确保它们是有效的
        return validContainerTypes.contains(block.getType()) && 
               (state instanceof Chest || state instanceof Barrel || state instanceof Furnace);
    }
    
    /**
     * 检查一个位置是否是有效的容器
     */
    public boolean isValidContainer(Location location) {
        if (location == null) return false;
        
        Block block = location.getBlock();
        BlockState state = block.getState();
        
        // 检查是否为有效的方块容器
        boolean isValidBlockContainer = validContainerTypes.contains(block.getType()) && 
               (state instanceof Chest || state instanceof Barrel || state instanceof Furnace);
        
        // 检查是否为ItemFrame实体
        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5);
        boolean isValidEntityContainer = nearbyEntities.stream()
                .anyMatch(entity -> entity instanceof ItemFrame);
        
        return isValidBlockContainer || isValidEntityContainer;
    }
    
    // 保存容器的初始状态
    private void saveContainerInitialState(Location location) {
        Block block = location.getBlock();
        BlockState state = block.getState();
        
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            Inventory inventory = chest.getInventory();
            ItemStack[] contents = inventory.getContents().clone();
            initialContainerContents.put(location, contents);
        } else if (state instanceof Barrel) {
            Barrel barrel = (Barrel) state;
            Inventory inventory = barrel.getInventory();
            ItemStack[] contents = inventory.getContents().clone();
            initialContainerContents.put(location, contents);
        } else if (state instanceof Furnace) {
            Furnace furnace = (Furnace) state;
            Inventory inventory = furnace.getInventory();
            ItemStack[] contents = inventory.getContents().clone();
            initialContainerContents.put(location, contents);
        }
        // 注意：ItemFrame是实体，需要单独处理
    }
    
    // 显示容器的粒子效果
    private void showContainerParticles(Player player, Set<Location> containers) {
        // 记录玩家已启用粒子效果
        particleEffectsEnabled.put(player, true);
        
        // 这里只是标记，实际的粒子效果显示需要在定时任务中实现
        // 这部分将在主插件类中实现
    }
    
    // 清空玩家选择的容器
    public void clearSelectedContainers(Player player) {
        selectedContainers.remove(player);
        particleEffectsEnabled.remove(player);
    }
    
    // 获取玩家选择的容器
    public Set<Location> getSelectedContainers(Player player) {
        return selectedContainers.getOrDefault(player, Collections.emptySet());
    }
    
    // 获取容器的初始状态
    public ItemStack[] getContainerInitialState(Location location) {
        return initialContainerContents.get(location);
    }
    
    // 检查玩家是否启用了粒子效果
    public boolean hasParticleEffectsEnabled(Player player) {
        return particleEffectsEnabled.getOrDefault(player, false);
    }
    

    
    /**
     * 注册容器到数据文件
     */
    public void registerContainer(String dataFileName, Location containerLocation) {
        if (!registeredContainers.containsKey(dataFileName)) {
            registeredContainers.put(dataFileName, ConcurrentHashMap.newKeySet());
        }
        registeredContainers.get(dataFileName).add(containerLocation);
        
        // 保存容器的初始状态
        saveContainerInitialState(containerLocation);
    }
    
    /**
     * 从数据文件移除容器
     */
    public void unregisterContainer(String dataFileName, Location containerLocation) {
        if (registeredContainers.containsKey(dataFileName)) {
            registeredContainers.get(dataFileName).remove(containerLocation);
            // 如果集合为空，移除该数据文件的条目
            if (registeredContainers.get(dataFileName).isEmpty()) {
                registeredContainers.remove(dataFileName);
            }
        }
        
        // 移除初始状态和刷新限制
        initialContainerContents.remove(containerLocation);
        refreshLimits.remove(containerLocation);
    }
    
    /**
     * 获取数据文件中的所有注册容器
     */
    public Set<Location> getRegisteredContainers(String dataFileName) {
        return registeredContainers.getOrDefault(dataFileName, ConcurrentHashMap.newKeySet());
    }
    
    /**
     * 检查容器是否已注册
     */
    public boolean isContainerRegistered(String dataFileName, Location containerLocation) {
        if (!registeredContainers.containsKey(dataFileName)) {
            return false;
        }
        return registeredContainers.get(dataFileName).contains(containerLocation);
    }
    
    /**
     * 设置容器的刷新限制配置
     */
    public void setRefreshLimit(Location containerLocation, int maxItems, long cooldownTime) {
        refreshLimits.put(containerLocation, new RefreshLimitConfig(maxItems, cooldownTime));
    }
    
    /**
     * 获取容器的刷新限制配置
     */
    public RefreshLimitConfig getRefreshLimit(Location containerLocation) {
        return refreshLimits.get(containerLocation);
    }
    
    /**
     * 检查容器是否可以刷新
     */
    public boolean canRefreshContainer(Location containerLocation) {
        RefreshLimitConfig config = refreshLimits.get(containerLocation);
        if (config == null) {
            // 如果没有配置限制，则可以刷新
            return true;
        }
        return config.canRefresh();
    }
    
    /**
     * 记录容器刷新了物品
     */
    public void recordContainerRefresh(Location containerLocation, int itemCount) {
        RefreshLimitConfig config = refreshLimits.get(containerLocation);
        if (config != null) {
            config.incrementItems(itemCount);
        }
    }
    
    // 简化版的记录刷新方法，默认计数为1
    public void recordContainerRefresh(Location containerLocation) {
        recordContainerRefresh(containerLocation, 1);
    }
    
    // 获取指定位置的所有ItemFrame实体
    public List<ItemFrame> getItemFrames(Location location) {
        List<ItemFrame> frames = new ArrayList<>();
        World world = location.getWorld();
        if (world != null) {
            Collection<Entity> entities = world.getNearbyEntities(location, 2, 2, 2);
            for (Entity entity : entities) {
                if (entity instanceof ItemFrame) {
                    frames.add((ItemFrame) entity);
                }
            }
        }
        return frames;
    }
    
    // 更新selectContainersInArea方法以包含ItemFrame和GlowItemFrame
    public int selectContainersInArea(Player player, AreaManager.Selection selection) {
        if (selection == null) {
            player.sendMessage("§c请先确认一个选区!");
            return 0;
        }
        
        // 清空之前的选择
        clearSelectedContainers(player);
        
        Set<Location> containers = new HashSet<>();
        
        // 扫描选区内的所有方块
        for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
            for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                    Location loc = new Location(selection.getWorld(), x, y, z);
                    Block block = loc.getBlock();
                    
                    // 检查是否是有效的容器方块
                    if (isValidContainer(block)) {
                        containers.add(loc);
                        
                        // 记录容器的初始状态
                        saveContainerInitialState(loc);
                    }
                }
            }
        }
        
        // 扫描选区内的所有ItemFrame和GlowItemFrame实体
        World world = selection.getWorld();
        if (world != null) {
            // 获取选区内的所有实体
            BoundingBox box = new BoundingBox(
                selection.getMinX(), selection.getMinY(), selection.getMinZ(),
                selection.getMaxX() + 1, selection.getMaxY() + 1, selection.getMaxZ() + 1
            );
            
            Collection<Entity> entities = world.getNearbyEntities(box);
            for (Entity entity : entities) {
                if (entity instanceof ItemFrame) {
                    // 对于实体，我们需要存储它们的精确位置
                    containers.add(entity.getLocation());
                    
                    // 注意：ItemFrame的内容处理需要特殊实现
                }
            }
        }
        
        // 保存选择的容器
        if (!containers.isEmpty()) {
            selectedContainers.put(player, containers);
            
            // 为管理员显示粒子效果
            if (player.hasPermission("area.refresh.edit")) {
                showContainerParticles(player, containers);
            }
            
            player.sendMessage("§a已选择 " + containers.size() + " 个容器!");
        } else {
            player.sendMessage("§c选区内没有找到有效的容器!");
        }
        
        return containers.size();
    }
    
    /**
     * 从数据文件注册容器及其配置
     */
    public void registerDataFileContainers(String dataFileName, org.bukkit.configuration.file.YamlConfiguration config) {
        try {
            // 读取容器列表
            List<String> containerList = config.getStringList("containers.list");
            for (String serializedLoc : containerList) {
                // 反序列化位置
                Location loc = deserializeLocation(serializedLoc);
                if (loc != null && loc.getWorld() != null) {
                    // 注册容器
                    registerContainer(dataFileName, loc);
                    
                    // 加载容器的刷新限制配置
                    String locKey = "containers." + serializeLocationKey(loc);
                    if (config.contains(locKey)) {
                        int maxItems = config.getInt(locKey + ".max_items", -1);
                        long cooldownTime = config.getLong(locKey + ".cooldown_time", 0);
                        setRefreshLimit(loc, maxItems, cooldownTime);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("注册数据文件容器失败: " + e.getMessage());
        }
    }
    
    // 辅助方法：反序列化位置
    private Location deserializeLocation(String serializedLoc) {
        try {
            String[] parts = serializedLoc.split(",");
            if (parts.length == 4) {
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
                if (world != null) {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    return new Location(world, x, y, z);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("无法反序列化位置: " + serializedLoc + " - " + e.getMessage());
        }
        return null;
    }
    
    // 辅助方法：生成位置的键名
    private String serializeLocationKey(Location loc) {
        return loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
    
    /**
     * 检查指定位置是否是有效的容器
     */
    public boolean isContainer(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        Block block = location.getBlock();
        Material type = block.getType();
        
        // 检查是否是有效容器类型
        return type == Material.CHEST || type == Material.BARREL || type == Material.FURNACE || 
               type == Material.DISPENSER || type == Material.DROPPER || type == Material.HOPPER;
    }
    
    /**
     * 移除被破坏的容器
     */
    public void removeBrokenContainer(Location location) {
        // 从所有玩家的选中容器列表中移除
        for (Map.Entry<Player, Set<Location>> entry : selectedContainers.entrySet()) {
            entry.getValue().remove(location);
        }
        
        // 从初始内容记录中移除
        initialContainerContents.remove(location);
        
        // 从刷新限制配置中移除
        refreshLimits.remove(location);
        
        // 从注册的容器中移除
        for (Set<Location> containers : registeredContainers.values()) {
            containers.remove(location);
        }
    }
}