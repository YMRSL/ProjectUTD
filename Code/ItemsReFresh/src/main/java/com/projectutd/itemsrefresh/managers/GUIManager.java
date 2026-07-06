package com.projectutd.itemsrefresh.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class GUIManager implements Listener {
    private final ItemsRefreshPlugin plugin;
    private static final int GUI_SIZE = 27; // 27格物品栏，与原版背包布局一致
    private static final String GUI_TITLE = "区域刷新管理界面";
    
    public GUIManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    // 打开刷新管理界面
    public void openRefreshGUI(Player player) {
        // 创建新的GUI界面
        Inventory gui = Bukkit.createInventory(player, GUI_SIZE, GUI_TITLE);
        
        // 加载该玩家当前选区的配置
        AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
        if (selection != null) {
            String selectionId = plugin.getDataManager().getSelectionId(selection);
            Inventory savedGUI = plugin.getDataManager().loadRefreshGUI(selectionId);
            if (savedGUI != null) {
                gui.setContents(savedGUI.getContents());
            }
        }
        
        // 打开GUI界面
        player.openInventory(gui);
        player.sendMessage("§a已打开区域刷新管理界面");
    }
    
    // 关闭刷新管理界面
    public void closeRefreshGUI(Player player) {
        if (isRefreshGUI(player.getOpenInventory().getTopInventory())) {
            player.closeInventory();
        }
    }
    
    // 检查是否是刷新管理界面
    private boolean isRefreshGUI(Inventory inventory) {
        // 在Spigot API 1.17.1中，Inventory没有getTitle()方法
        // 我们通过物品栏大小和用途来判断
        return inventory != null && inventory.getSize() == GUI_SIZE && inventory.getType() == org.bukkit.event.inventory.InventoryType.CHEST;
    }
    
    // 处理GUI点击事件
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        
        // 检查是否点击的是刷新管理界面
        if (!isRefreshGUI(event.getView().getTopInventory()) || clickedInventory == null) {
            return;
        }
        
        // 取消所有点击事件，防止物品被移动
        event.setCancelled(true);
        
        // 检查玩家是否有编辑权限
        if (!player.hasPermission("area.refresh.edit")) {
            player.sendMessage("§c你没有权限编辑此界面");
            return;
        }
        
        // 只有点击顶部界面时才处理（防止玩家点击自己的物品栏）
        if (clickedInventory.equals(event.getView().getTopInventory())) {
            int slot = event.getSlot();
            
            // 如果是右键点击，清空该槽位
            if (event.isRightClick()) {
                clickedInventory.setItem(slot, null);
                return;
            }
            
            // 获取玩家手持的物品
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null) {
                // 复制物品到槽位（使用clone避免引用问题）
                ItemStack itemToPlace = cursorItem.clone();
                
                // 检查物品堆叠数量是否超过最大堆叠数
                int maxStackSize = itemToPlace.getType().getMaxStackSize();
                if (itemToPlace.getAmount() > maxStackSize) {
                    itemToPlace.setAmount(maxStackSize);
                    player.sendMessage("§e已自动修正物品堆叠数量");
                }
                
                clickedInventory.setItem(slot, itemToPlace);
            }
        }
    }
    
    // 处理GUI关闭事件
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // 检查是否是刷新管理界面
        if (!isRefreshGUI(inventory)) {
            return;
        }
        
        // 获取玩家当前选区
        AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
        if (selection != null) {
            // 保存界面内容
            String selectionId = plugin.getDataManager().getSelectionId(selection);
            plugin.getDataManager().saveRefreshGUI(selectionId, inventory);
            player.sendMessage("§a刷新界面配置已保存");
        }
    }
    
    // 清空指定选区的刷新界面
    public void clearGUIContents(String areaId) {
        // 创建一个空的GUI并保存
        Inventory emptyGUI = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        plugin.getDataManager().saveRefreshGUI(areaId, emptyGUI);
    }
}