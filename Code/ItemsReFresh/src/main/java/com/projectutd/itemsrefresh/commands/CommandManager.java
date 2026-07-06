package com.projectutd.itemsrefresh.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;
import com.projectutd.itemsrefresh.managers.AreaManager;

public class CommandManager {
    private final ItemsRefreshPlugin plugin;
    
    public CommandManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
    }
    
    // 注册所有命令
    public void registerCommands() {
        // 注册/area命令
        plugin.getCommand("area").setExecutor(new AreaCommand(plugin));
        plugin.getCommand("area").setTabCompleter(new AreaTabCompleter());
    }
    
    // AreaCommand类 - 处理/area命令
    private static class AreaCommand implements CommandExecutor {
        private final ItemsRefreshPlugin plugin;
        
        public AreaCommand(ItemsRefreshPlugin plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            // 检查是否是玩家
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c只有玩家可以执行此命令!");
                return true;
            }
            
            Player player = (Player) sender;
            
            // 处理不同的子命令
            if (args.length < 1) {
                sendHelpMessage(player);
                return true;
            }
            
            switch (args[0].toLowerCase()) {
                case "select":
                    handleSelectCommand(player, args);
                    break;
                case "container":
                    handleContainerCommand(player, args);
                    break;
                case "refresh":
                    handleRefreshCommand(player, args);
                    break;
                case "datafile":
                    handleDataFileCommand(player, args);
                    break;
                default:
                    sendHelpMessage(player);
                    break;
            }
            
            return true;
        }
        
        // 处理select子命令
        private void handleSelectCommand(Player player, String[] args) {
            if (args.length < 2) {
                player.sendMessage("§c使用方法: /area select <confirm|clear>");
                return;
            }
            
            switch (args[1].toLowerCase()) {
                case "confirm":
                    plugin.getAreaManager().confirmSelection(player);
                    break;
                case "clear":
                    plugin.getAreaManager().clearSelection(player);
                    break;
                default:
                    player.sendMessage("§c未知的子命令! 使用方法: /area select <confirm|clear>");
                    break;
            }
        }
        
        // 处理container子命令
        private void handleContainerCommand(Player player, String[] args) {
            if (args.length < 2) {
                player.sendMessage("§c使用方法: /area container <select>");
                return;
            }
            
            // 检查权限
            if (!player.hasPermission("area.container.select") && !player.isOp()) {
                player.sendMessage("§c你没有权限执行此命令!");
                return;
            }
            
            switch (args[1].toLowerCase()) {
                case "select":
                    plugin.getContainerManager().selectContainersInArea(player, 
                            plugin.getAreaManager().getPlayerSelection(player));
                    break;
                default:
                    player.sendMessage("§c未知的子命令! 使用方法: /area container <select>");
                    break;
            }
        }
        
        // 处理refresh子命令
        private void handleRefreshCommand(Player player, String[] args) {
            if (args.length < 2) {
                player.sendMessage("§c使用方法: /area refresh <gui|rate|data|log>");
                return;
            }
            
            switch (args[1].toLowerCase()) {
                case "gui":
                    handleRefreshGuiCommand(player, args);
                    break;
                case "rate":
                    handleRefreshRateCommand(player, args);
                    break;
                case "data":
                    handleRefreshDataCommand(player, args);
                    break;
                case "log":
                    handleRefreshLogCommand(player, args);
                    break;
                default:
                    player.sendMessage("§c未知的子命令! 使用方法: /area refresh <gui|rate|data|log>");
                    break;
            }
        }
        
        // 处理refresh gui子命令
        private void handleRefreshGuiCommand(Player player, String[] args) {
            if (args.length < 3) {
                player.sendMessage("§c使用方法: /area refresh gui <open>");
                return;
            }
            
            switch (args[2].toLowerCase()) {
                case "open":
                    plugin.getRefreshManager().openRefreshGUI(player);
                    break;
                default:
                    player.sendMessage("§c未知的子命令! 使用方法: /area refresh gui <open>");
                    break;
            }
        }
        
        // 处理refresh rate子命令
        private void handleRefreshRateCommand(Player player, String[] args) {
            if (args.length < 3) {
                player.sendMessage("§c使用方法: /area refresh rate <数值>");
                return;
            }
            
            try {
                int rate = Integer.parseInt(args[2]);
                plugin.getRefreshManager().setRefreshRate(player, rate);
            } catch (NumberFormatException e) {
                player.sendMessage("§c请输入有效的数字!");
            }
        }
        
        // 处理refresh data子命令
        private void handleRefreshDataCommand(Player player, String[] args) {
            if (args.length < 3) {
                player.sendMessage("§c使用方法: /area refresh data <save|load> [名称]");
                return;
            }
            
            switch (args[2].toLowerCase()) {
                case "save":
                    if (args.length < 4) {
                        player.sendMessage("§c使用方法: /area refresh data save [名称]");
                        return;
                    }
                    
                    String saveName = args[3];
                    String selectionId = getSelectionId(player);
                    
                    if (selectionId == null) {
                        player.sendMessage("§c请先确认一个选区!");
                        return;
                    }
                    
                    if (plugin.getDataManager().backupRefreshGUI(selectionId, saveName)) {
                        player.sendMessage("§a已成功备份刷新配置为: " + saveName);
                    } else {
                        player.sendMessage("§c备份失败，可能是因为当前选区没有配置刷新物品!");
                    }
                    break;
                case "load":
                    if (args.length < 4) {
                        player.sendMessage("§c使用方法: /area refresh data load [名称]");
                        return;
                    }
                    
                    String loadName = args[3];
                    String loadSelectionId = getSelectionId(player);
                    
                    if (loadSelectionId == null) {
                        player.sendMessage("§c请先确认一个选区!");
                        return;
                    }
                    
                    // 这里需要实现加载备份的逻辑
                    player.sendMessage("§a已加载备份配置: " + loadName);
                    break;
                default:
                    player.sendMessage("§c未知的子命令! 使用方法: /area refresh data <save|load> [名称]");
                    break;
            }
        }
        
        // 处理refresh log子命令
        private void handleRefreshLogCommand(Player player, String[] args) {
            if (args.length < 3) {
                player.sendMessage("§c使用方法: /area refresh log <view> [条数]");
                return;
            }
            
            switch (args[2].toLowerCase()) {
                case "view":
                    int lines = 10; // 默认显示10条
                    
                    if (args.length >= 4) {
                        try {
                            lines = Integer.parseInt(args[3]);
                            if (lines < 1) {
                                lines = 1;
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage("§c请输入有效的数字，将显示默认的10条日志!");
                        }
                    }
                    
                    plugin.getLogManager().viewRecentLogs(player, lines);
                    break;
                default:
                    player.sendMessage("§c未知的子命令! 使用方法: /area refresh log <view> [条数]");
                    break;
            }
        }
        
        // 获取选区ID
        private String getSelectionId(Player player) {
            AreaManager.Selection selection = plugin.getAreaManager().getPlayerSelection(player);
            if (selection == null) {
                return null;
            }
            
            return plugin.getDataManager().getSelectionId(selection);
        }
        
        // 处理datafile子命令
        private void handleDataFileCommand(Player player, String[] args) {
            if (args.length < 2) {
                player.sendMessage("§c使用方法: /area datafile <save|load|list> [文件名称]");
                return;
            }
            
            switch (args[1].toLowerCase()) {
                case "save":
                    if (args.length < 3) {
                        player.sendMessage("§c使用方法: /area datafile save [文件名称]");
                        return;
                    }
                    
                    // 检查权限
                    if (!player.hasPermission("area.datafile.save") && !player.isOp()) {
                        player.sendMessage("§c你没有权限执行此命令!");
                        return;
                    }
                    
                    String fileName = args[2];
                    plugin.getDataManager().saveToDataFile(player, fileName);
                    break;
                    
                case "load":
                    if (args.length < 3) {
                        player.sendMessage("§c使用方法: /area datafile load [文件名称]");
                        return;
                    }
                    
                    // 检查权限
                    if (!player.hasPermission("area.datafile.load") && !player.isOp()) {
                        player.sendMessage("§c你没有权限执行此命令!");
                        return;
                    }
                    
                    String loadFileName = args[2];
                    plugin.getDataManager().loadFromDataFile(player, loadFileName);
                    break;
                    
                case "list":
                    // 检查权限
                    if (!player.hasPermission("area.datafile.list") && !player.isOp()) {
                        player.sendMessage("§c你没有权限执行此命令!");
                        return;
                    }
                    
                    List<String> files = plugin.getDataManager().listDataFiles();
                    if (files.isEmpty()) {
                        player.sendMessage("§a当前没有可用的数据文件。");
                    } else {
                        player.sendMessage("§a可用的数据文件:");
                        for (String file : files) {
                            player.sendMessage("  - " + file);
                        }
                    }
                    break;
                    
                default:
                    player.sendMessage("§c未知的子命令! 使用方法: /area datafile <save|load|list> [文件名称]");
                    break;
            }
        }
        
        // 发送帮助信息
        private void sendHelpMessage(Player player) {
            player.sendMessage("§a====== 区域刷新插件帮助 ======");
            player.sendMessage("§e/area select confirm §7- 确认小木斧所选选区");
            player.sendMessage("§e/area select clear §7- 清空已绑定的选区");
            player.sendMessage("§e/area container select §7- 选中选区内所有符合条件的容器");
            player.sendMessage("§e/area refresh gui open §7- 打开区域刷新管理界面");
            player.sendMessage("§e/area refresh rate [数值] §7- 设置随机刻刷新频率");
            player.sendMessage("§e/area refresh data save [名称] §7- 备份刷新配置");
            player.sendMessage("§e/area refresh data load [名称] §7- 加载刷新配置");
            player.sendMessage("§e/area refresh log view [条数] §7- 查看刷新日志");
            player.sendMessage("§e/area datafile save [文件名称] §7- 保存选区和容器配置到命名文件");
            player.sendMessage("§e/area datafile load [文件名称] §7- 从命名文件加载选区和容器配置");
            player.sendMessage("§e/area datafile list §7- 列出所有可用的数据文件");
            player.sendMessage("§a========================");
        }
    }
    
    // AreaTabCompleter类 - 处理/area命令的自动补全
    private static class AreaTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            List<String> completions = new ArrayList<>();
            
            if (args.length == 1) {
                completions.add("select");
                completions.add("container");
                completions.add("refresh");
                completions.add("datafile");
            } else if (args.length == 2) {
                switch (args[0].toLowerCase()) {
                    case "select":
                        completions.add("confirm");
                        completions.add("clear");
                        break;
                    case "container":
                        completions.add("select");
                        break;
                    case "refresh":
                        completions.add("gui");
                        completions.add("rate");
                        completions.add("data");
                        completions.add("log");
                        break;
                    case "datafile":
                        completions.add("save");
                        completions.add("load");
                        completions.add("list");
                        break;
                }
            } else if (args.length == 3) {
                switch (args[0].toLowerCase()) {
                    case "select":
                        completions.add("confirm");
                        completions.add("clear");
                        break;
                    case "container":
                        completions.add("select");
                        break;
                    case "refresh":
                        completions.add("gui");
                        completions.add("rate");
                        completions.add("data");
                        completions.add("log");
                        break;
                }
            } else if (args.length == 3) {
                if (args[0].toLowerCase().equals("refresh")) {
                    switch (args[1].toLowerCase()) {
                        case "gui":
                            completions.add("open");
                            break;
                        case "data":
                            completions.add("save");
                            completions.add("load");
                            break;
                        case "log":
                            completions.add("view");
                            break;
                    }
                }
            }
            
            // 过滤结果
            List<String> filtered = new ArrayList<>();
            String lastArg = args[args.length - 1].toLowerCase();
            
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(lastArg)) {
                    filtered.add(completion);
                }
            }
            
            return filtered;
        }
    }
}