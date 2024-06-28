package com.johnson.teamparty;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GUIListener implements Listener {

    private final Teamparty plugin;

    public GUIListener(Teamparty plugin) {
        this.plugin = plugin;
    }

    public void openTeamStatusGUI(Player player, Team team) {
        Inventory gui = Bukkit.createInventory(null, 27, plugin.getMessage("gui_title"));

        // 設置隊長頭顱
        updateTeamStatusGUI(gui, team);

        // 設置離隊按鈕
        ItemStack leaveButton = new ItemStack(Material.BARRIER);
        ItemMeta leaveMeta = leaveButton.getItemMeta();
        leaveMeta.setDisplayName(plugin.getMessage("gui_leave_button"));
        leaveButton.setItemMeta(leaveMeta);
        gui.setItem(26, leaveButton);

        player.openInventory(gui);
    }

    public void updateTeamStatusGUI(Inventory gui, Team team) {
        plugin.getLogger().info("正在更新隊伍狀態GUI...");

        // 設置隊長頭顱
        ItemStack leaderHead = createPlayerHead(team.getLeader());
        ItemMeta leaderMeta = leaderHead.getItemMeta();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("leader", team.getLeader().getName());
        leaderMeta.setDisplayName(plugin.getMessage("gui_leader", placeholders));
        leaderHead.setItemMeta(leaderMeta);
        gui.setItem(0, leaderHead);

        // 設置隊員頭顱
        int slot = 1;
        for (Player member : team.getMembers()) {
            if (!member.equals(team.getLeader())) { // 不顯示隊長的頭顱在隊員中
                ItemStack memberHead = createPlayerHead(member);
                ItemMeta memberMeta = memberHead.getItemMeta();
                placeholders = new HashMap<>();
                placeholders.put("health", String.valueOf(member.getHealth()));
                placeholders.put("food", String.valueOf(member.getFoodLevel()));
                placeholders.put("status", member.isOnline() ? plugin.getMessage("gui_status_online") : plugin.getMessage("gui_status_offline"));
                List<String> lore = Arrays.asList(
                        plugin.getMessage("gui_member_health", placeholders),
                        plugin.getMessage("gui_member_food", placeholders),
                        plugin.getMessage("gui_member_status", placeholders));
                memberMeta.setLore(lore);
                placeholders = new HashMap<>();
                placeholders.put("member", member.getName());
                memberMeta.setDisplayName(plugin.getMessage("gui_member", placeholders));
                memberHead.setItemMeta(memberMeta);
                gui.setItem(slot++, memberHead);
                //plugin.getLogger().info("設置玩家 " + member.getName() + " 的頭顱到GUI中。");
            }
        }

        // 清空多餘的格子
        for (int i = slot; i < 26; i++) {
            gui.setItem(i, new ItemStack(Material.AIR));
        }

        //plugin.getLogger().info("隊伍狀態GUI更新完成。");
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        return head;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(plugin.getMessage("gui_title"))) {
            event.setCancelled(true); // 防止玩家取出物品
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                Player player = (Player) event.getWhoClicked();
                Team team = plugin.getPlayerTeam(player);
                if (team != null) {
                    if (team.getLeader().equals(player)) {
                        // 通知所有成員隊伍將被解散
                        for (Player member : team.getMembers()) {
                            member.sendMessage(plugin.getMessage("team_disbanded"));
                        }
                        // 移除所有成員
                        for (Player member : team.getMembers()) {
                            plugin.removePlayerFromTeam(member);
                        }
                        // 從隊伍列表中移除
                        plugin.getTeams().remove(team.getName());
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("team", team.getName());
                        player.sendMessage(plugin.getMessage("team_disband_left", placeholders));
                    } else {
                        plugin.removePlayerFromTeam(player);
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("team", team.getName());
                        player.sendMessage(plugin.getMessage("team_left", placeholders));
                    }
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(plugin.getMessage("gui_title"))) {
            // 這裡可以添加額外的關閉GUI時的處理邏輯
        }
    }
}
