package com.projectutd.itemsrefresh.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Utils {
    // 格式化位置信息为字符串
    public static String formatLocation(Location loc) {
        if (loc == null) {
            return "无效位置";
        }
        
        World world = loc.getWorld();
        String worldName = world != null ? world.getName() : "未知世界";
        
        return worldName + " (" +
               loc.getBlockX() + "," +
               loc.getBlockY() + "," +
               loc.getBlockZ() + ")";
    }
    
    // 计算两个位置之间的向量
    public static Vector getVectorBetweenLocations(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || !loc1.getWorld().equals(loc2.getWorld())) {
            return null;
        }
        
        return loc2.toVector().subtract(loc1.toVector());
    }
    
    // 计算选区的体积
    public static int calculateVolume(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || !loc1.getWorld().equals(loc2.getWorld())) {
            return 0;
        }
        
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int length = maxZ - minZ + 1;
        
        return width * height * length;
    }
    
    // 格式化物品信息为字符串
    public static String formatItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "空气";
        }
        
        String name = item.getType().name();
        int amount = item.getAmount();
        
        return name + " x" + amount;
    }
    
    // 格式化时间戳为可读时间
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
    
    // 分割字符串为列表
    public static List<String> splitString(String input, String delimiter) {
        if (input == null || input.isEmpty()) {
            return Arrays.asList();
        }
        
        return Arrays.asList(input.split(delimiter));
    }
    
    // 发送消息给玩家（带颜色）
    public static void sendMessage(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(message);
        }
    }
    
    // 检查两个物品是否相同（类型和数据值）
    public static boolean isItemEqual(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return item1 == item2;
        }
        
        return item1.getType() == item2.getType() && 
               item1.getDurability() == item2.getDurability() && 
               item1.getItemMeta().equals(item2.getItemMeta());
    }
    
    // 获取安全的物品堆叠数量
    public static int getSafeStackSize(ItemStack item, int desiredSize) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        
        int maxStackSize = item.getType().getMaxStackSize();
        return Math.min(desiredSize, maxStackSize);
    }
    
    // 限制数值在指定范围内
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // 限制数值在指定范围内
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // 检查字符串是否是整数
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // 检查字符串是否是有效数字
    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}