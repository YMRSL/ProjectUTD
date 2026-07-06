package com.projectutd.itemsrefresh;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.projectutd.itemsrefresh.commands.CommandManager;
import com.projectutd.itemsrefresh.listeners.BlockListener; 
import com.projectutd.itemsrefresh.listeners.PlayerInteractListener;
import com.projectutd.itemsrefresh.managers.AreaManager;
import com.projectutd.itemsrefresh.managers.ContainerManager;
import com.projectutd.itemsrefresh.managers.RefreshManager;
import com.projectutd.itemsrefresh.managers.DataManager;
import com.projectutd.itemsrefresh.managers.LogManager;

public class ItemsRefreshPlugin extends JavaPlugin {
    
    private static ItemsRefreshPlugin instance;
    private AreaManager areaManager;
    private ContainerManager containerManager;
    private RefreshManager refreshManager;
    private DataManager dataManager;
    private LogManager logManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 创建管理器实例
        areaManager = new AreaManager(this);
        containerManager = new ContainerManager(this);
        refreshManager = new RefreshManager(this);
        dataManager = new DataManager(this);
        logManager = new LogManager(this);
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        
        // 注册指令
        CommandManager commandManager = new CommandManager(this);
        commandManager.registerCommands();
        
        // 加载数据
        dataManager.loadAllData();
        
        // 启动随机刷新任务
        refreshManager.startRandomTickTask();
        
        Bukkit.getLogger().info("ItemsRefreshPlugin 已成功启用!");
    }
    
    @Override
    public void onDisable() {
        // 保存数据
        dataManager.saveAllData();
        
        // 停止随机刷新任务
        refreshManager.stopRandomTickTask();
        
        Bukkit.getLogger().info("ItemsRefreshPlugin 已成功停用!");
    }
    
    public static ItemsRefreshPlugin getInstance() {
        return instance;
    }
    
    public AreaManager getAreaManager() {
        return areaManager;
    }
    
    public ContainerManager getContainerManager() {
        return containerManager;
    }
    
    public RefreshManager getRefreshManager() {
        return refreshManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public LogManager getLogManager() {
        return logManager;
    }
}