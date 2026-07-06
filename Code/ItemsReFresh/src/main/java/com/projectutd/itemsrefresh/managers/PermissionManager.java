package com.projectutd.itemsrefresh.managers;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import java.util.ArrayList;
import java.util.List;
import com.projectutd.itemsrefresh.ItemsRefreshPlugin;

public class PermissionManager {
    private final ItemsRefreshPlugin plugin;
    private final List<Permission> permissions = new ArrayList<>();
    
    // 定义插件的权限节点
    public static final String ADMIN_PERMISSION = "area.refresh.admin";
    public static final String EDIT_PERMISSION = "area.refresh.edit";
    public static final String CONTAINER_SELECT_PERMISSION = "area.container.select";
    public static final String LOG_VIEW_PERMISSION = "area.refresh.log.view";
    public static final String DATA_MANAGE_PERMISSION = "area.refresh.data.manage";
    
    public PermissionManager(ItemsRefreshPlugin plugin) {
        this.plugin = plugin;
        registerPermissions();
    }
    
    // 注册所有权限节点
    private void registerPermissions() {
        PluginManager pm = plugin.getServer().getPluginManager();
        
        // 管理员权限（包含所有子权限）
        Permission adminPerm = new Permission(ADMIN_PERMISSION, PermissionDefault.OP);
        permissions.add(adminPerm);
        
        // 编辑刷新界面权限
        Permission editPerm = new Permission(EDIT_PERMISSION, PermissionDefault.FALSE);
        editPerm.addParent(adminPerm, true);
        permissions.add(editPerm);
        
        // 选择容器权限
        Permission containerSelectPerm = new Permission(CONTAINER_SELECT_PERMISSION, PermissionDefault.FALSE);
        containerSelectPerm.addParent(adminPerm, true);
        permissions.add(containerSelectPerm);
        
        // 查看刷新日志权限
        Permission logViewPerm = new Permission(LOG_VIEW_PERMISSION, PermissionDefault.FALSE);
        logViewPerm.addParent(adminPerm, true);
        permissions.add(logViewPerm);
        
        // 管理数据备份权限
        Permission dataManagePerm = new Permission(DATA_MANAGE_PERMISSION, PermissionDefault.FALSE);
        dataManagePerm.addParent(adminPerm, true);
        permissions.add(dataManagePerm);
        
        // 注册所有权限到服务器
        for (Permission perm : permissions) {
            pm.addPermission(perm);
        }
    }
    
    // 检查玩家是否有指定权限
    public boolean hasPermission(org.bukkit.entity.Player player, String permission) {
        // 管理员默认拥有所有权限
        if (player.isOp() || player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        
        return player.hasPermission(permission);
    }
    
    // 获取所有注册的权限
    public List<Permission> getPermissions() {
        return permissions;
    }
    
    // 清理权限注册（插件卸载时调用）
    public void cleanup() {
        PluginManager pm = plugin.getServer().getPluginManager();
        
        for (Permission perm : permissions) {
            pm.removePermission(perm);
        }
        
        permissions.clear();
    }
    
    // 检查并添加缺少的权限节点
    public void checkPermissions() {
        PluginManager pm = plugin.getServer().getPluginManager();
        
        for (Permission perm : permissions) {
            if (!pm.getPermissions().contains(perm)) {
                pm.addPermission(perm);
            }
        }
    }
}