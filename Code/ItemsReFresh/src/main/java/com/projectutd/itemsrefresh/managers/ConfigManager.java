package com.projectutd.itemsrefresh.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.List;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class ConfigManager {
    private final ItemsRefreshPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    public ConfigManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    // 加载配置文件
    public void loadConfig() {
        // 如果配置文件不存在，创建默认配置
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 检查配置项是否存在，如果不存在则设置默认值
        checkAndSetDefaults();
    }
    
    // 检查并设置默认配置
    private void checkAndSetDefaults() {
        boolean configChanged = false;
        
        // 随机刻触发频率配置
        if (!config.contains("random-tick-frequency")) {
            config.set("random-tick-frequency", 2000); // 默认每游戏刻有1/2000概率对单个容器触发随机刻
            configChanged = true;
        }
        
        // 最大选区范围配置
        if (!config.contains("max-selection-size")) {
            config.set("max-selection-size", 1000000); // 默认最大选区范围为1000000格
            configChanged = true;
        }
        
        // 可检测的容器类型配置
        if (!config.contains("allowed-containers")) {
            config.set("allowed-containers", List.of("CHEST", "BARREL", "ITEM_FRAME"));
            configChanged = true;
        }
        
        // 如果配置有更改，保存配置文件
        if (configChanged) {
            saveConfig();
        }
    }
    
    // 保存配置文件
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 获取配置项
    public FileConfiguration getConfig() {
        return config;
    }
    
    // 获取随机刻触发频率
    public int getRandomTickFrequency() {
        return config.getInt("random-tick-frequency", 2000);
    }
    
    // 设置随机刻触发频率
    public void setRandomTickFrequency(int frequency) {
        config.set("random-tick-frequency", frequency);
        saveConfig();
    }
    
    // 获取最大选区范围
    public int getMaxSelectionSize() {
        return config.getInt("max-selection-size", 1000000);
    }
    
    // 获取允许的容器类型列表
    public List<String> getAllowedContainers() {
        return config.getStringList("allowed-containers");
    }
    
    // 重载配置文件
    public void reloadConfig() {
        loadConfig();
    }
}