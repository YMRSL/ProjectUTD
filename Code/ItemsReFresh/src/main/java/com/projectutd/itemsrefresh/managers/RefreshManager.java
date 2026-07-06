package com.projectutd.itemsrefresh.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Barrel;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class RefreshManager {
    private final ItemsRefreshPlugin plugin;
    private final Map<Player, Inventory> refreshGUIs;
    private final Map<String, Double> refreshRates; // 选区ID -> 刷新频率
    private BukkitTask randomTickTask;
    private int defaultRefreshRate = 2000; // 默认每游戏刻有1/2000概率对单个容器触发随机刻
    
    public RefreshManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        this.refreshGUIs = new HashMap<>();
        this.refreshRates = new HashMap<>();
    }
    
    // 打开区域刷新管理界面
    public void openRefreshGUI(Player player) {
        AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
        
        if (selection == null) {
            player.sendMessage("§c请先确认一个选区!");
            return;
        }
        
        // 获取或创建该选区的刷新界面
        String selectionId = getSelectionId(selection);
        Inventory gui = plugin.getDataManager().loadRefreshGUI(selectionId);
        
        if (gui == null) {
            // 创建新的界面
            gui = Bukkit.createInventory(player, 27, "区域刷新管理");
        }
        
        // 保存界面引用
        refreshGUIs.put(player, gui);
        
        // 打开界面
        player.openInventory(gui);
    }
    
    // 关闭区域刷新管理界面
    public void closeRefreshGUI(Player player) {
        Inventory gui = refreshGUIs.remove(player);
        
        if (gui != null) {
            AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
            
            if (selection != null) {
                // 保存界面数据
                String selectionId = getSelectionId(selection);
                plugin.getDataManager().saveRefreshGUI(selectionId, gui);
            }
        }
    }
    
    // 检查玩家是否可以编辑界面
    public boolean canEditGUI(Player player) {
        return player.hasPermission("area.refresh.edit") || player.isOp();
    }
    
    // 开始随机刻刷新任务
    public void startRandomTickTask() {
        // 每游戏刻执行一次
        randomTickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processRandomTicks, 1L, 1L);
    }
    
    // 停止随机刻刷新任务
    public void stopRandomTickTask() {
        if (randomTickTask != null) {
            randomTickTask.cancel();
            randomTickTask = null;
        }
    }
    
    // 处理随机刻
    private void processRandomTicks() {
        // 遍历所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
            
            if (selection == null) {
                continue;
            }
            
            // 获取该选区的刷新频率
            String selectionId = getSelectionId(selection);
            int rate = (int) Math.round(refreshRates.getOrDefault(selectionId, (double) defaultRefreshRate));
            
            // 获取选区内的容器
            Set<Location> containers = plugin.getContainerManager().getSelectedContainers(player);
            
            // 对每个容器进行随机刻判定
            for (Location containerLoc : containers) {
                // 判断是否触发随机刻
                if (new Random().nextInt(rate) == 0) {
                    // 执行刷新
                    performRefresh(selectionId, containerLoc);
                }
            }
        }
    }
    
    // 执行刷新操作
    private void performRefresh(String selectionId, Location containerLoc) {
        // 检查容器是否可以刷新（考虑刷新限制）
        if (!plugin.getContainerManager().canRefreshContainer(containerLoc)) {
            return; // 容器达到刷新限制，不执行刷新
        }
        
        // 加载刷新界面配置
        Inventory gui = plugin.getDataManager().loadRefreshGUI(selectionId);
        
        if (gui == null || gui.isEmpty()) {
            return; // 界面为空，不执行刷新
        }
        
        // 随机选择一个物品栏
        Random random = new Random();
        int slot = random.nextInt(gui.getSize());
        ItemStack item = gui.getItem(slot);
        
        if (item == null || item.getType() == Material.AIR) {
            return; // 所选物品栏为空，不执行刷新
        }
        
        // 确保物品数量不超过最大堆叠数
        int maxStackSize = item.getType().getMaxStackSize();
        if (item.getAmount() > maxStackSize) {
            item.setAmount(maxStackSize);
        }
        
        // 复制物品，避免修改原物品
        ItemStack refreshItem = item.clone();
        
        // 处理不同类型的容器
        BlockState state = containerLoc.getBlock().getState();
        boolean refreshed = false;
        
        if (state instanceof Chest || state instanceof Barrel || state instanceof Furnace) {
            // 处理箱子、木桶和熔炉
            Inventory containerInventory;
            
            if (state instanceof Chest) {
                containerInventory = ((Chest) state).getInventory();
            } else if (state instanceof Barrel) {
                containerInventory = ((Barrel) state).getInventory();
            } else {
                // 熔炉
                containerInventory = ((Furnace) state).getInventory();
            }
            
            // 检查容器是否有空间
            if (containerInventory.firstEmpty() != -1) {
                // 添加物品
                containerInventory.addItem(refreshItem);
                refreshed = true;
            }
        } else {
            // 检查是否是ItemFrame或GlowItemFrame实体
            Collection<Entity> nearbyEntities = containerLoc.getWorld().getNearbyEntities(containerLoc, 0.5, 0.5, 0.5);
            for (Entity entity : nearbyEntities) {
                if (entity instanceof ItemFrame) {
                    ItemFrame frame = (ItemFrame) entity;
                    if (frame.getItem().getType() == Material.AIR) {
                        // 只有当物品展示框为空时才添加物品
                        frame.setItem(refreshItem);
                        refreshed = true;
                        break;
                    }
                }
            }
        }
        
        // 如果成功刷新，记录刷新信息和限制
        if (refreshed) {
            // 记录刷新日志
            plugin.getLogManager().logRefresh(containerLoc, refreshItem, slot);
            // 更新刷新限制计数
            plugin.getContainerManager().recordContainerRefresh(containerLoc, refreshItem.getAmount());
        }
    }
    
    // 设置刷新频率
    public void setRefreshRate(Player player, int rate) {
        // 验证频率范围
        if (rate < 1 || rate > 10000) {
            player.sendMessage("§c刷新频率必须在1到10000之间!");
            return;
        }
        
        AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
        
        if (selection == null) {
            player.sendMessage("§c请先确认一个选区!");
            return;
        }
        
        String selectionId = getSelectionId(selection);
        refreshRates.put(selectionId, (double) rate);
        
        // 保存刷新频率
        plugin.getDataManager().saveRefreshRate(selectionId, rate);
        
        player.sendMessage("§a已设置刷新频率为1/" + rate + "!");
    }
    
    // 获取刷新频率
    public int getRefreshRate(String selectionId) {
        return (int) Math.round(refreshRates.getOrDefault(selectionId, (double) defaultRefreshRate));
    }
    
    // 获取选区ID
    private String getSelectionId(AreaManager.Selection selection) {
        return selection.getWorld().getName() + ":" + 
               selection.getMinX() + "," + selection.getMinY() + "," + selection.getMinZ() + ":" +
               selection.getMaxX() + "," + selection.getMaxY() + "," + selection.getMaxZ();
    }
    
    // 保存所有刷新数据
    public void saveAllData() {
        // 保存所有玩家的刷新界面
        for (Map.Entry<Player, Inventory> entry : refreshGUIs.entrySet()) {
            Player player = entry.getKey();
            Inventory gui = entry.getValue();
            
            AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
            
            if (selection != null) {
                String selectionId = getSelectionId(selection);
                plugin.getDataManager().saveRefreshGUI(selectionId, gui);
            }
        }
        
        // 保存所有刷新频率
        for (Map.Entry<String, Double> entry : refreshRates.entrySet()) {
            plugin.getDataManager().saveRefreshRate(entry.getKey(), (int) Math.round(entry.getValue()));
        }
    }
    
    // 加载所有刷新数据
    public void loadAllData() {
        // 加载所有刷新频率
        Map<String, Integer> rates = plugin.getDataManager().loadAllRefreshRates();
        
        for (Map.Entry<String, Integer> entry : rates.entrySet()) {
            refreshRates.put(entry.getKey(), (double) entry.getValue());
        }
    }
    
    /**
     * 获取玩家打开的刷新GUI
     */
    public Inventory getOpenRefreshGUI(Player player) {
        return refreshGUIs.get(player);
    }
}