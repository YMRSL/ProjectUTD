package com.projectutd.itemsrefresh.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class PlayerInteractListener implements Listener {
    private final ItemsRefreshPlugin plugin;
    private static final String SELECTION_AXE_NAME = "§a小木斧";
    
    public PlayerInteractListener(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
    }
    
    // 处理玩家交互事件（主要是小木斧选区）
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否是创造模式或有管理员权限
        if (player.getGameMode() != GameMode.CREATIVE && !player.isOp()) {
            return;
        }
        
        // 检查玩家是否手持小木斧
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return;
        }
        
        // 检查物品名称是否为小木斧
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getDisplayName().equals(SELECTION_AXE_NAME)) {
            // 也可以通过检查是否有特定的lore来判断
            if (meta == null || !meta.hasLore() || !meta.getLore().contains("§7用于区域选择")) {
                return;
            }
        }
        
        // 获取被点击的方块
        Block clickedBlock = event.getClickedBlock();
        
        if (clickedBlock == null) {
            return;
        }
        
        // 处理不同的点击方式
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK:
                // 左键选择第一个点
                plugin.getAreaManager().setSelectionPoint1(player, clickedBlock.getLocation());
                event.setCancelled(true);
                break;
            case RIGHT_CLICK_BLOCK:
                // 右键选择第二个点
                plugin.getAreaManager().setSelectionPoint2(player, clickedBlock.getLocation());
                event.setCancelled(true);
                break;
            default:
                break;
        }
    }
    
    // 给予玩家小木斧
    public static void giveSelectionAxe(Player player) {
        ItemStack axe = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = axe.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(SELECTION_AXE_NAME);
            meta.setLore(Arrays.asList("§7用于区域选择", "§7左键: 设置第一个点", "§7右键: 设置第二个点"));
            
            // 设置为不可破坏
            meta.setUnbreakable(true);
            
            axe.setItemMeta(meta);
        }
        
        player.getInventory().addItem(axe);
        player.sendMessage("§a已获得小木斧! 左键设置第一个点，右键设置第二个点。");
    }
    
    // 检查物品是否是小木斧
    public static boolean isSelectionAxe(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getDisplayName().equals(SELECTION_AXE_NAME) || 
               (meta.hasLore() && meta.getLore().contains("§7用于区域选择"));
    }
}