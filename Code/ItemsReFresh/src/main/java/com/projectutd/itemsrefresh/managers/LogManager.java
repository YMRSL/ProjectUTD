package com.projectutd.itemsrefresh.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class LogManager {
    private final ItemsRefreshPlugin plugin;
    private final File logFolder;
    private final SimpleDateFormat dateFormat;
    private static final int MAX_LOG_LINES = 1000; // 每个日志文件的最大行数
    
    public LogManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        
        // 创建日志文件夹
        this.logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        
        // 初始化日期格式化器
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
    
    // 记录刷新行为
    public void logRefresh(Location containerLocation, ItemStack item, int slot) {
        String logMessage = formatLogEntry(containerLocation, item, slot);
        
        // 写入日志文件
        writeToLogFile(logMessage);
        
        // 如果需要，也可以写入控制台
        // plugin.getLogger().info(logMessage);
    }
    
    // 格式化日志条目
    private String formatLogEntry(Location containerLocation, ItemStack item, int slot) {
        String timestamp = dateFormat.format(new Date());
        long gameTick = getCurrentGameTick();
        
        String locationStr = containerLocation.getWorld().getName() + 
                            " (" + containerLocation.getBlockX() + "," + 
                            containerLocation.getBlockY() + "," + 
                            containerLocation.getBlockZ() + ")";
        
        String itemStr = item.getType().name() + " x" + item.getAmount();
        
        return "[" + timestamp + "] [Tick: " + gameTick + "] " +
               "位置: " + locationStr + ", " +
               "物品: " + itemStr + ", " +
               "刷新序号: " + slot;
    }
    
    // 获取当前游戏刻
    private long getCurrentGameTick() {
        return plugin.getServer().getWorlds().get(0).getFullTime();
    }
    
    // 写入日志文件
    private void writeToLogFile(String logMessage) {
        String fileName = "refresh_log_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        File logFile = new File(logFolder, fileName);
        
        try {
            // 检查文件是否超过最大行数
            if (logFile.exists() && getLineCount(logFile) >= MAX_LOG_LINES) {
                // 创建新的日志文件（添加序号）
                int counter = 1;
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                String extension = fileName.substring(fileName.lastIndexOf('.'));
                
                while (new File(logFolder, baseName + "_" + counter + extension).exists()) {
                    counter++;
                }
                
                logFile = new File(logFolder, baseName + "_" + counter + extension);
            }
            
            // 写入日志
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logMessage);
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "写入刷新日志失败: " + e.getMessage(), e);
        }
    }
    
    // 获取文件行数
    private int getLineCount(File file) throws IOException {
        return (int) Files.lines(Paths.get(file.getPath())).count();
    }
    
    // 查看最近的日志
    public void viewRecentLogs(Player player, int lines) {
        // 限制最大查看行数
        if (lines > 100) {
            lines = 100;
            player.sendMessage("§c已限制最大查看行数为100条!");
        }
        
        // 获取最新的日志文件
        File latestLogFile = getLatestLogFile();
        
        if (latestLogFile == null || !latestLogFile.exists()) {
            player.sendMessage("§c没有找到刷新日志文件!");
            return;
        }
        
        try {
            // 读取日志文件的最后N行
            List<String> recentLogs = getLastNLines(latestLogFile, lines);
            
            if (recentLogs.isEmpty()) {
                player.sendMessage("§c日志文件为空!");
                return;
            }
            
            // 发送日志给玩家
            player.sendMessage("§a最近 " + recentLogs.size() + " 条刷新日志:");
            for (String log : recentLogs) {
                player.sendMessage("§7" + log);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "读取刷新日志失败: " + e.getMessage(), e);
            player.sendMessage("§c读取日志失败，请查看控制台获取详细信息!");
        }
    }
    
    // 获取最新的日志文件
    private File getLatestLogFile() {
        File[] logFiles = logFolder.listFiles((dir, name) -> name.startsWith("refresh_log_") && name.endsWith(".log"));
        
        if (logFiles == null || logFiles.length == 0) {
            return null;
        }
        
        File latestFile = logFiles[0];
        for (File file : logFiles) {
            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }
        
        return latestFile;
    }
    
    // 获取文件的最后N行
    private List<String> getLastNLines(File file, int n) throws IOException {
        List<String> lines = new ArrayList<>();
        
        // 读取所有行
        List<String> allLines = Files.readAllLines(Paths.get(file.getPath()));
        
        // 如果行数少于n，返回所有行
        if (allLines.size() <= n) {
            lines.addAll(allLines);
        } else {
            // 否则返回最后n行
            lines.addAll(allLines.subList(allLines.size() - n, allLines.size()));
        }
        
        // 反转列表，使最新的日志显示在前面
        Collections.reverse(lines);
        
        return lines;
    }
}