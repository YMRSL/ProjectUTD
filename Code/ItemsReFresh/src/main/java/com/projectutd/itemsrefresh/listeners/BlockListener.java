package com.projectutd.itemsrefresh.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class BlockListener implements Listener {
    private final ItemsRefreshPlugin plugin;
    
    public BlockListener(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
    }
    
    // 处理方块破坏事件
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // 获取被破坏的方块位置
        Location location = event.getBlock().getLocation();
        
        // 检查是否是容器
        if (plugin.getContainerManager().isContainer(location)) {
            // 从容器列表中移除被破坏的容器
            plugin.getContainerManager().removeBrokenContainer(location);
            
            // 可以在这里添加日志记录，记录哪个玩家破坏了哪个容器
            // plugin.getLogger().info("玩家 " + event.getPlayer().getName() + " 破坏了容器: " + 
            //        location.getWorld().getName() + " (" + 
            //        location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")");
        }
    }
    
    // 可以在这里添加其他方块相关的事件处理方法
    // 例如方块放置、方块状态更新等
}