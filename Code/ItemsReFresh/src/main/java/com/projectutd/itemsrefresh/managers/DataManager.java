package com.projectutd.itemsrefresh.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class DataManager {
    private final ItemsRefreshPlugin plugin;
    private final File dataFolder;
    private final File selectionsFile;
    private final File refreshGUIFolder;
    private final File refreshRatesFile;
    private final File backupsFolder;
    private final File dataFilesFolder; // 存储命名数据文件的文件夹
    
    // 当前活动的数据文件名称
    private final Map<Player, String> activeDataFiles = new HashMap<>();
    
    public DataManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        
        // 创建数据文件夹
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // 创建文件和子文件夹
        this.selectionsFile = new File(dataFolder, "selections.yml");
        this.refreshGUIFolder = new File(dataFolder, "refresh_guis");
        if (!refreshGUIFolder.exists()) {
            refreshGUIFolder.mkdirs();
        }
        this.refreshRatesFile = new File(dataFolder, "refresh_rates.yml");
        this.backupsFolder = new File(dataFolder, "backups");
        if (!backupsFolder.exists()) {
            backupsFolder.mkdirs();
        }
        // 创建命名数据文件的文件夹
        this.dataFilesFolder = new File(plugin.getDataFolder(), "data_files");
        if (!dataFilesFolder.exists()) {
            dataFilesFolder.mkdirs();
        }
    }
    
    // 保存玩家选区
    public void savePlayerSelection(Player player, AreaManager.Selection selection) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(selectionsFile);
        
        String path = "selections." + player.getUniqueId().toString();
        config.set(path + ".world", selection.getWorld().getName());
        config.set(path + ".minX", selection.getMinX());
        config.set(path + ".minY", selection.getMinY());
        config.set(path + ".minZ", selection.getMinZ());
        config.set(path + ".maxX", selection.getMaxX());
        config.set(path + ".maxY", selection.getMaxY());
        config.set(path + ".maxZ", selection.getMaxZ());
        
        try {
            config.save(selectionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家选区数据失败: " + e.getMessage());
        }
    }
    
    // 删除玩家选区
    public void deletePlayerSelection(Player player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(selectionsFile);
        
        String path = "selections." + player.getUniqueId().toString();
        config.set(path, null);
        
        try {
            config.save(selectionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("删除玩家选区数据失败: " + e.getMessage());
        }
    }
    
    // 加载玩家选区
    public AreaManager.Selection loadPlayerSelection(Player player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(selectionsFile);
        
        String path = "selections." + player.getUniqueId().toString();
        if (!config.contains(path)) {
            return null;
        }
        
        String worldName = config.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            return null;
        }
        
        int minX = config.getInt(path + ".minX");
        int minY = config.getInt(path + ".minY");
        int minZ = config.getInt(path + ".minZ");
        int maxX = config.getInt(path + ".maxX");
        int maxY = config.getInt(path + ".maxY");
        int maxZ = config.getInt(path + ".maxZ");
        
        Location point1 = new Location(world, minX, minY, minZ);
        Location point2 = new Location(world, maxX, maxY, maxZ);
        
        return new AreaManager.Selection(point1, point2);
    }
    
    // 保存刷新界面
    public void saveRefreshGUI(String selectionId, Inventory gui) {
        File file = new File(refreshGUIFolder, selectionId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // 保存物品栏内容
        for (int i = 0; i < gui.getSize(); i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                // 确保物品数量不超过最大堆叠数
                int maxStackSize = item.getType().getMaxStackSize();
                if (item.getAmount() > maxStackSize) {
                    item.setAmount(maxStackSize);
                    // 这里应该有提示信息，但由于这是后台操作，我们只在日志中记录
                    plugin.getLogger().info("已自动修正物品堆叠数量: " + item.getType() + " 数量: " + maxStackSize);
                }
                config.set("items." + i, item);
            }
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存刷新界面数据失败: " + e.getMessage());
        }
    }
    
    // 加载刷新界面
    public Inventory loadRefreshGUI(String selectionId) {
        File file = new File(refreshGUIFolder, selectionId + ".yml");
        
        if (!file.exists()) {
            return null;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Inventory gui = Bukkit.createInventory(null, 27, "区域刷新管理");
        
        // 加载物品栏内容
        if (config.contains("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack("items." + key);
                    if (item != null && slot >= 0 && slot < gui.getSize()) {
                        gui.setItem(slot, item);
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的槽位号
                }
            }
        }
        
        return gui;
    }
    
    // 保存刷新频率
    public void saveRefreshRate(String selectionId, int rate) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(refreshRatesFile);
        config.set(selectionId, rate);
        
        try {
            config.save(refreshRatesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存刷新频率数据失败: " + e.getMessage());
        }
    }
    
    // 加载所有刷新频率
    public Map<String, Integer> loadAllRefreshRates() {
        Map<String, Integer> rates = new HashMap<>();
        
        if (!refreshRatesFile.exists()) {
            return rates;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(refreshRatesFile);
        
        for (String key : config.getKeys(false)) {
            rates.put(key, config.getInt(key));
        }
        
        return rates;
    }
    
    // 备份刷新界面配置
    public boolean backupRefreshGUI(String selectionId, String backupName) {
        // 加载当前配置
        Inventory gui = loadRefreshGUI(selectionId);
        
        if (gui == null) {
            return false;
        }
        
        // 创建备份文件夹
        File backupDir = new File(backupsFolder, selectionId);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // 保存备份
        File backupFile = new File(backupDir, backupName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(backupFile);
        
        // 保存物品栏内容
        for (int i = 0; i < gui.getSize(); i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                config.set("items." + i, item);
            }
        }
        
        try {
            config.save(backupFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("备份刷新界面配置失败: " + e.getMessage());
            return false;
        }
    }
    
    // 加载备份刷新界面配置
    public Inventory loadBackupRefreshGUI(String selectionId, String backupName) {
        File backupFile = new File(new File(backupsFolder, selectionId), backupName + ".yml");
        
        if (!backupFile.exists()) {
            return null;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(backupFile);
        Inventory gui = Bukkit.createInventory(null, 27, "区域刷新管理");
        
        // 加载物品栏内容
        if (config.contains("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack("items." + key);
                    if (item != null && slot >= 0 && slot < gui.getSize()) {
                        gui.setItem(slot, item);
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的槽位号
                }
            }
        }
        
        return gui;
    }
    
    // 加载所有数据
    public void loadAllData() {
        // 这里可以加载其他需要在插件启动时加载的数据
        // 玩家选区数据在玩家加入时加载
    }
    
    // 保存所有数据
    public void saveAllData() {
        // 保存所有玩家的刷新界面
        plugin.getRefreshManager().saveAllData();
    }
    
    // 获取选区ID
    public String getSelectionId(AreaManager.Selection selection) {
        return selection.getWorld().getName() + ":" + 
               selection.getMinX() + "," + selection.getMinY() + "," + selection.getMinZ() + ":" +
               selection.getMaxX() + "," + selection.getMaxY() + "," + selection.getMaxZ();
    }
    
    /**
     * 设置玩家的活动数据文件
     */
    public void setActiveDataFile(Player player, String fileName) {
        activeDataFiles.put(player, fileName);
    }
    
    /**
     * 获取玩家的活动数据文件
     */
    public String getActiveDataFile(Player player) {
        return activeDataFiles.getOrDefault(player, null);
    }
    
    /**
     * 移除玩家的活动数据文件
     */
    public void removeActiveDataFile(Player player) {
        activeDataFiles.remove(player);
    }
    
    /**
     * 获取命名数据文件的路径
     */
    public File getDataFile(String fileName) {
        return new File(dataFilesFolder, fileName + ".yml");
    }
    
    /**
     * 列出所有可用的命名数据文件
     */
    public List<String> listDataFiles() {
        List<String> files = new ArrayList<>();
        File[] ymlFiles = dataFilesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles != null) {
            for (File file : ymlFiles) {
                files.add(file.getName().replace(".yml", ""));
            }
        }
        return files;
    }
    
    /**
     * 保存当前选区的容器和配置到命名数据文件
     */
    public boolean saveToDataFile(Player player, String fileName) {
        try {
            // 获取玩家的选区
            AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
            if (selection == null) {
                player.sendMessage("§c请先确认一个选区!");
                return false;
            }
            
            // 获取选区内的容器
            Set<Location> containers = plugin.getContainerManager().getSelectedContainers(player);
            if (containers.isEmpty()) {
                player.sendMessage("§c选区内没有选择容器!");
                return false;
            }
            
            // 获取选区的ID和刷新频率
            String selectionId = getSelectionId(selection);
            int refreshRate = plugin.getRefreshManager().getRefreshRate(selectionId);
            
            // 创建数据文件
            File dataFile = getDataFile(fileName);
            YamlConfiguration config = new YamlConfiguration();
            
            // 保存基本信息
            config.set("name", fileName);
            config.set("creation_time", System.currentTimeMillis());
            config.set("creator", player.getName());
            
            // 保存选区信息
            config.set("selection.world", selection.getWorld().getName());
            config.set("selection.min_x", selection.getMinX());
            config.set("selection.min_y", selection.getMinY());
            config.set("selection.min_z", selection.getMinZ());
            config.set("selection.max_x", selection.getMaxX());
            config.set("selection.max_y", selection.getMaxY());
            config.set("selection.max_z", selection.getMaxZ());
            
            // 保存刷新频率
            config.set("refresh_rate", refreshRate);
            
            // 保存容器信息
            List<String> containerList = new ArrayList<>();
            for (Location loc : containers) {
                containerList.add(serializeLocation(loc));
                
                // 保存容器的刷新限制配置
                ContainerManager.RefreshLimitConfig limitConfig = plugin.getContainerManager().getRefreshLimit(loc);
                if (limitConfig != null) {
                    String locKey = "containers." + serializeLocationKey(loc);
                    config.set(locKey + ".max_items", limitConfig.getMaxItems());
                    config.set(locKey + ".cooldown_time", limitConfig.getCooldownTime());
                }
            }
            config.set("containers.list", containerList);
            
            // 保存刷新GUI配置
            Inventory gui = plugin.getRefreshManager().getOpenRefreshGUI(player);
            if (gui != null) {
                List<String> guiItems = new ArrayList<>();
                for (int i = 0; i < gui.getSize(); i++) {
                    ItemStack item = gui.getItem(i);
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        try {
                            // 使用Spigot 1.16.5兼容的序列化方式
                            Map<String, Object> serializedMap = item.serialize();
                            String serializedItem = serializeMapToString(serializedMap);
                            guiItems.add(i + ":" + serializedItem);
                        } catch (Exception e) {
                            plugin.getLogger().warning("无法序列化物品: " + e.getMessage());
                        }
                    }
                }
                config.set("refresh_gui.items", guiItems);
            }
            
            // 保存文件
            config.save(dataFile);
            
            player.sendMessage("§a已成功保存数据到文件: " + fileName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("保存数据文件失败: " + e.getMessage());
            player.sendMessage("§c保存数据文件失败，请查看控制台日志!");
            return false;
        }
    }
    
    /**
     * 从命名数据文件加载容器和配置到当前选区
     */
    public boolean loadFromDataFile(Player player, String fileName) {
        try {
            // 获取数据文件
            File dataFile = getDataFile(fileName);
            if (!dataFile.exists()) {
                player.sendMessage("§c找不到数据文件: " + fileName);
                return false;
            }
            
            // 加载配置
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            // 如果玩家有选区，将数据应用到当前选区
            AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
            if (selection != null) {
                // 获取选区ID
                String selectionId = getSelectionId(selection);
                
                // 设置刷新频率
                int refreshRate = config.getInt("refresh_rate", 2000);
                plugin.getRefreshManager().setRefreshRate(player, refreshRate);
                
                // 加载刷新GUI配置
                List<String> guiItems = config.getStringList("refresh_gui.items");
                if (!guiItems.isEmpty()) {
                    // 创建新的GUI
                    Inventory gui = Bukkit.createInventory(player, 27, "区域刷新管理");
                    
                    for (String itemData : guiItems) {
                        try {
                            String[] parts = itemData.split(":", 2);
                            int slot = Integer.parseInt(parts[0]);
                            // 使用Spigot 1.16.5兼容的反序列化方式
                            Map<String, Object> serializedMap = deserializeStringToMap(parts[1]);
                            ItemStack item = ItemStack.deserialize(serializedMap);
                            gui.setItem(slot, item);
                        } catch (Exception e) {
                            plugin.getLogger().warning("无法反序列化物品: " + e.getMessage());
                        }
                    }
                    
                    // 保存GUI到数据
                    saveRefreshGUI(selectionId, gui);
                }
            }
            
            // 注册数据文件中的容器到系统中
            plugin.getContainerManager().registerDataFileContainers(fileName, config);
            
            player.sendMessage("§a已成功从文件加载数据: " + fileName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("加载数据文件失败: " + e.getMessage());
            player.sendMessage("§c加载数据文件失败，请查看控制台日志!");
            return false;
        }
    }
    
    // 辅助方法：序列化位置
    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    
    // 辅助方法：生成位置的键名
    private String serializeLocationKey(Location loc) {
        return loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
    
    /**
     * 将Map对象序列化为Base64编码的字符串
     */
    @SuppressWarnings("unchecked")
    private String serializeMapToString(Map<String, Object> map) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            
            // 转换Map为可序列化的形式
            Map<String, Serializable> serializableMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Serializable) {
                    serializableMap.put(entry.getKey(), (Serializable) entry.getValue());
                } else {
                    // 对于不可序列化的值，尝试转换为字符串
                    serializableMap.put(entry.getKey(), entry.getValue().toString());
                }
            }
            
            oos.writeObject(serializableMap);
            oos.flush();
            byte[] serializedBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(serializedBytes);
        }
    }
    
    /**
     * 将Base64编码的字符串反序列化为Map对象
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> deserializeStringToMap(String serializedString) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(serializedString));
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            
            Map<String, Serializable> serializableMap = (Map<String, Serializable>) ois.readObject();
            
            // 转换回原始的Map形式
            Map<String, Object> resultMap = new HashMap<>();
            for (Map.Entry<String, Serializable> entry : serializableMap.entrySet()) {
                resultMap.put(entry.getKey(), entry.getValue());
            }
            
            return resultMap;
        }
    }
}