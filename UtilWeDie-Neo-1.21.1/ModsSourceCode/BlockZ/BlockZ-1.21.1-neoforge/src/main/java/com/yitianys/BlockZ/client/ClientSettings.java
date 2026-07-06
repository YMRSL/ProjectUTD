package com.yitianys.BlockZ.client;

/**
 * 客户端会话状态。仅保留 KEEP 字段（DayZ 界面开关 / 权限）。
 *
 * <p>DROP（HUD/体力/感染/护理/倾身）相关字段已移除。{@code dayzHudEnabled}
 * 作为无害布尔保留：{@code ClientPacketHandler.handleDayzToggleState} 仍会写入它，
 * 但不再驱动任何 HUD 渲染。
 */
public class ClientSettings {
    public static boolean dayzEnabled = true;
    public static boolean dayzToggleAllowed = true;
    public static boolean dayzHudEnabled = true;
}
