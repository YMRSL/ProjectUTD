package com.projectutd.itemsrefresh.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.World;
import java.util.HashMap;
import java.util.Map;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

// 导入Bukkit版本的WorldEdit适配器
import com.sk89q.worldedit.bukkit.BukkitAdapter;
// 不直接导入ForgeAdapter，而是在运行时通过反射尝试支持

public class AreaManager {
    private final ItemsRefreshPlugin plugin;
    private final Map<Player, Location> selectionPoints1;
    private final Map<Player, Location> selectionPoints2;
    private final Map<Player, Selection> playerSelections;
    private int maxSelectionSize = 5000; // 默认选区最大范围
    
    public AreaManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        this.selectionPoints1 = new HashMap<>();
        this.selectionPoints2 = new HashMap<>();
        this.playerSelections = new HashMap<>();
    }
    
    // 设置第一个选点
    public void setSelectionPoint1(Player player, Location location) {
        selectionPoints1.put(player, location);
        player.sendMessage("§a已选择第一个点: X:" + location.getBlockX() + ", Y:" + location.getBlockY() + ", Z:" + location.getBlockZ());
    }
    
    // 设置第二个选点
    public void setSelectionPoint2(Player player, Location location) {
        selectionPoints2.put(player, location);
        player.sendMessage("§a已选择第二个点: X:" + location.getBlockX() + ", Y:" + location.getBlockY() + ", Z:" + location.getBlockZ());
    }
    
    // 确认选区 - 优先使用WorldEdit选区
    public boolean confirmSelection(Player player) {
        // 尝试获取WorldEdit选区
        Selection selection = getWorldEditSelection(player);
        
        if (selection != null) {
            // 校验选区是否合法
            if (!isValidSelection(selection)) {
                player.sendMessage("§cWorldEdit所选区域过大或超出边界，请重新选择!");
                return false;
            }
            
            playerSelections.put(player, selection);
            
            // 保存选区数据
            plugin.getDataManager().savePlayerSelection(player, selection);
            
            player.sendMessage("§a已成功使用WorldEdit选区! 范围: " + 
                    selection.getMinX() + "," + selection.getMinY() + "," + selection.getMinZ() + 
                    " 至 " + 
                    selection.getMaxX() + "," + selection.getMaxY() + "," + selection.getMaxZ());
            
            return true;
        }
        
        // 如果没有WorldEdit选区，则使用传统选区方法
        Location point1 = selectionPoints1.get(player);
        Location point2 = selectionPoints2.get(player);
        
        if (point1 == null || point2 == null) {
            player.sendMessage("§c请先选择两个点来定义区域，或使用WorldEdit小木斧选择区域!");
            return false;
        }
        
        if (!point1.getWorld().equals(point2.getWorld())) {
            player.sendMessage("§c两个选点必须在同一个世界中!");
            return false;
        }
        
        // 校验选区是否合法
        if (!isValidSelection(point1, point2)) {
            player.sendMessage("§c所选区域无效，请重新选择!");
            return false;
        }
        
        // 创建选区
        selection = new Selection(point1, point2);
        playerSelections.put(player, selection);
        
        // 保存选区数据
        plugin.getDataManager().savePlayerSelection(player, selection);
        
        player.sendMessage("§a选区已确认! 范围: " + 
                selection.getMinX() + "," + selection.getMinY() + "," + selection.getMinZ() + 
                " 至 " + 
                selection.getMaxX() + "," + selection.getMaxY() + "," + selection.getMaxZ());
        
        return true;
    }
    
    // 尝试获取玩家的WorldEdit选区（支持Bukkit和尝试支持Forge版本）
    private Selection getWorldEditSelection(Player player) {
        try {
            // 首先尝试Bukkit版本的WorldEdit
            try {
                // 检查WorldEdit插件是否已加载
                if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
                    // 使用BukkitAdapter将Bukkit玩家转换为WorldEdit玩家
                    com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
                    
                    // 获取WorldEdit的WorldEdit对象
                    WorldEdit worldEdit = WorldEdit.getInstance();
                    
                    // 获取玩家当前所在的世界
                    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(player.getWorld());
                    
                    // 获取玩家的选区
                    Region region = worldEdit.getSessionManager().get(wePlayer).getSelection(weWorld);
                    
                    if (region != null && region instanceof CuboidRegion) {
                        return createSelectionFromRegion(region, BukkitAdapter::adapt);
                    }
                }
            } catch (Exception e) {
                // Bukkit版本WorldEdit不可用，继续尝试Forge版本
                plugin.getLogger().fine("Bukkit版本WorldEdit不可用: " + e.getMessage());
            }
            
            // 尝试通过反射支持Forge版本的WorldEdit
            try {
                return tryGetForgeSelectionWithReflection(player);
            } catch (Exception e) {
                // Forge版本WorldEdit也不可用
                plugin.getLogger().fine("Forge版本WorldEdit不可用: " + e.getMessage());
            }
            
            // 两种版本的WorldEdit都不可用或没有选区
            return null;
        } catch (Exception e) {
            // 如果WorldEdit不可用或出现错误，返回null
            plugin.getLogger().warning("尝试获取WorldEdit选区时出错: " + e.getMessage());
            return null;
        }
    }
    
    // 从WorldEdit选区创建Selection对象
    private Selection createSelectionFromRegion(Region region, java.util.function.Function<com.sk89q.worldedit.world.World, World> worldAdapter) {
        if (!(region instanceof CuboidRegion)) {
            return null;
        }
        
        CuboidRegion cuboidRegion = (CuboidRegion) region;
        
        // 获取选区的最小和最大点
        BlockVector3 min = cuboidRegion.getMinimumPoint();
        BlockVector3 max = cuboidRegion.getMaximumPoint();
        
        // 转换为Bukkit的Location
        World world = worldAdapter.apply(cuboidRegion.getWorld());
        Location point1 = new Location(world, min.getX(), min.getY(), min.getZ());
        Location point2 = new Location(world, max.getX(), max.getY(), max.getZ());
        
        // 创建选区对象
        return new Selection(point1, point2);
    }
    
    // 通过反射尝试获取Forge版本的WorldEdit选区
    private Selection tryGetForgeSelectionWithReflection(Player player) {
        try {
            // 检查是否在Forge环境中
            if (!isForgeEnvironment()) {
                return null;
            }
            
            // 使用反射获取Forge版本的WorldEdit相关类和方法
            
            // 1. 尝试获取WorldEdit实例
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Object worldEditInstance = worldEditClass.getMethod("getInstance").invoke(null);
            
            // 2. 尝试获取SessionManager
            Object sessionManager = worldEditClass.getMethod("getSessionManager").invoke(worldEditInstance);
            
            // 3. 获取MinecraftServer实例（用于获取玩家对应的Forge Player）
            Class<?> minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
            Object minecraftServer = minecraftServerClass.getMethod("getServer").invoke(null);
            
            // 4. 获取玩家的EntityPlayer（Forge版本的玩家对象）
            Object entityPlayer = minecraftServerClass.getMethod("getPlayerList").invoke(minecraftServer);
            Object playerList = entityPlayer;
            entityPlayer = playerList.getClass().getMethod("getPlayerByName", String.class).invoke(playerList, player.getName());
            
            if (entityPlayer == null) {
                return null;
            }
            
            // 5. 将Forge的EntityPlayer转换为WorldEdit的Player对象
            Class<?> forgeAdapterClass = Class.forName("com.sk89q.worldedit.forge.ForgeAdapter");
            Object wePlayer = forgeAdapterClass.getMethod("adaptPlayer", entityPlayer.getClass()).invoke(null, entityPlayer);
            
            // 6. 获取玩家会话
            Object session = sessionManager.getClass().getMethod("get", wePlayer.getClass()).invoke(sessionManager, wePlayer);
            
            // 7. 获取玩家的世界
            Object forgeWorld = getForgeWorld(player);
            
            if (forgeWorld == null) {
                return null;
            }
            
            // 8. 转换世界对象为WorldEdit的World
            Object weWorld = forgeAdapterClass.getMethod("adaptWorld", forgeWorld.getClass()).invoke(null, forgeWorld);
            
            // 9. 获取选区
            try {
                // 尝试获取选区（针对较新的WorldEdit版本）
                Object region = session.getClass().getMethod("getSelection", weWorld.getClass()).invoke(session, weWorld);
                
                if (region != null) {
                    // 检查是否是长方体选区
                    if (region.getClass().getName().contains("CuboidRegion")) {
                        // 获取最小和最大点
                        Object minPoint = region.getClass().getMethod("getMinimumPoint").invoke(region);
                        Object maxPoint = region.getClass().getMethod("getMaximumPoint").invoke(region);
                        
                        // 获取坐标值
                        int minX = (int) minPoint.getClass().getMethod("getX").invoke(minPoint);
                        int minY = (int) minPoint.getClass().getMethod("getY").invoke(minPoint);
                        int minZ = (int) minPoint.getClass().getMethod("getZ").invoke(minPoint);
                        int maxX = (int) maxPoint.getClass().getMethod("getX").invoke(maxPoint);
                        int maxY = (int) maxPoint.getClass().getMethod("getY").invoke(maxPoint);
                        int maxZ = (int) maxPoint.getClass().getMethod("getZ").invoke(maxPoint);
                        
                        // 创建Bukkit的Location对象
                        World world = player.getWorld();
                        Location point1 = new Location(world, minX, minY, minZ);
                        Location point2 = new Location(world, maxX, maxY, maxZ);
                        
                        // 创建选区
                        return new Selection(point1, point2);
                    }
                }
            } catch (NoSuchMethodException e) {
                // 尝试使用旧版本的方法
                try {
                    Object region = session.getClass().getMethod("getRegion", weWorld.getClass()).invoke(session, weWorld);
                    // 后续处理类似
                    if (region != null) {
                        // 这里简化处理，实际可能需要根据不同版本的API调整
                        return null;
                    }
                } catch (Exception ex) {
                    plugin.getLogger().fine("尝试获取Forge选区失败: " + ex.getMessage());
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().fine("通过反射获取Forge选区失败: " + e.getMessage());
            return null;
        }
    }
    
    // 通过反射获取Forge的World对象
    private Object getForgeWorld(Player player) {
        try {
            // 获取MinecraftServer实例
            Class<?> minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
            Object minecraftServer = minecraftServerClass.getMethod("getServer").invoke(null);
            
            // 获取世界列表
            Object worldServer = minecraftServerClass.getMethod("getLevel", String.class).invoke(minecraftServer, player.getWorld().getName());
            
            return worldServer;
        } catch (Exception e) {
            plugin.getLogger().fine("通过反射获取Forge世界失败: " + e.getMessage());
            return null;
        }
    }
    
    // 检测当前是否在Forge环境中运行
    private boolean isForgeEnvironment() {
        try {
            // 尝试加载Forge特有的类来检测环境
            Class.forName("net.minecraftforge.fml.common.Mod");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    // 校验选区是否合法(重载，用于WorldEdit选区)
    private boolean isValidSelection(Selection selection) {
        // 计算选区大小
        int sizeX = selection.getMaxX() - selection.getMinX() + 1;
        int sizeY = selection.getMaxY() - selection.getMinY() + 1;
        int sizeZ = selection.getMaxZ() - selection.getMinZ() + 1;
        
        long totalBlocks = (long) sizeX * sizeY * sizeZ;
        
        // 检查是否超过最大范围
        if (totalBlocks > maxSelectionSize) {
            return false;
        }
        
        // 检查是否超出世界边界
        if (selection.getMinX() < -29999984 || selection.getMaxX() > 29999984 || 
            selection.getMinZ() < -29999984 || selection.getMaxZ() > 29999984) {
            return false;
        }
        
        // 检查Y坐标是否在有效范围内
        if (selection.getMinY() < 0 || selection.getMaxY() > 255) {
            return false;
        }
        
        return true;
    }
    
    // 清空选区
    public void clearSelection(Player player) {
        selectionPoints1.remove(player);
        selectionPoints2.remove(player);
        playerSelections.remove(player);
        
        // 删除选区数据
        plugin.getDataManager().deletePlayerSelection(player);
        
        player.sendMessage("§a已清空选区!");
    }
    
    // 检查选区是否合法
    private boolean isValidSelection(Location point1, Location point2) {
        // 计算选区大小
        int sizeX = Math.abs(point1.getBlockX() - point2.getBlockX()) + 1;
        int sizeY = Math.abs(point1.getBlockY() - point2.getBlockY()) + 1;
        int sizeZ = Math.abs(point1.getBlockZ() - point2.getBlockZ()) + 1;
        
        long totalBlocks = (long) sizeX * sizeY * sizeZ;
        
        // 检查是否超过最大范围
        if (totalBlocks > maxSelectionSize) {
            return false;
        }
        
        // 检查是否超出世界边界
        World world = point1.getWorld();
        int minX = Math.min(point1.getBlockX(), point2.getBlockX());
        int maxX = Math.max(point1.getBlockX(), point2.getBlockX());
        int minY = Math.min(point1.getBlockY(), point2.getBlockY());
        int maxY = Math.max(point1.getBlockY(), point2.getBlockY());
        int minZ = Math.min(point1.getBlockZ(), point2.getBlockZ());
        int maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ());
        
        // Minecraft世界边界通常是±30,000,000，但我们使用更保守的值
        if (minX < -29999984 || maxX > 29999984 || minZ < -29999984 || maxZ > 29999984) {
            return false;
        }
        
        // 检查Y坐标是否在有效范围内
        if (minY < 0 || maxY > 255) {
            return false;
        }
        
        return true;
    }
    
    // 获取玩家的选区
    public Selection getPlayerSelection(Player player) {
        return playerSelections.get(player);
    }
    
    // 设置最大选区范围
    public void setMaxSelectionSize(int size) {
        this.maxSelectionSize = size;
    }
    
    // 获取最大选区范围
    public int getMaxSelectionSize() {
        return maxSelectionSize;
    }
    
    // 选区类
    public static class Selection {
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        private final World world;
        
        public Selection(Location point1, Location point2) {
            this.world = point1.getWorld();
            this.minX = Math.min(point1.getBlockX(), point2.getBlockX());
            this.minY = Math.min(point1.getBlockY(), point2.getBlockY());
            this.minZ = Math.min(point1.getBlockZ(), point2.getBlockZ());
            this.maxX = Math.max(point1.getBlockX(), point2.getBlockX());
            this.maxY = Math.max(point1.getBlockY(), point2.getBlockY());
            this.maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ());
        }
        
        // 获取最小坐标
        public int getMinX() { return minX; }
        public int getMinY() { return minY; }
        public int getMinZ() { return minZ; }
        
        // 获取最大坐标
        public int getMaxX() { return maxX; }
        public int getMaxY() { return maxY; }
        public int getMaxZ() { return maxZ; }
        
        // 获取世界
        public World getWorld() { return world; }
        
        // 检查位置是否在选区内
        public boolean contains(Location location) {
            if (!location.getWorld().equals(world)) {
                return false;
            }
            
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }
}