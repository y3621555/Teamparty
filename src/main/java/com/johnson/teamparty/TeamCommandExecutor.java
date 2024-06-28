package com.johnson.teamparty;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamCommandExecutor implements CommandExecutor {
    private final Teamparty plugin;
    private final Map<UUID, UUID> pendingInvitations = new HashMap<>(); // 儲存待處理的邀請

    public TeamCommandExecutor(Teamparty plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("not_a_player"));
            return true;
        }

        Player player = (Player) sender;

        // 權限檢查
        if (!player.hasPermission("teamparty.use")) {
            player.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§a指令列表：");
            player.sendMessage("§e/team create <隊伍名稱>§7 - 創建隊伍");
            player.sendMessage("§e/team add <玩家名>§7 - 添加成員");
            player.sendMessage("§e/team leave§7 - 離開隊伍");
            player.sendMessage("§e/team gui§7 - 查看隊伍狀態");
            player.sendMessage("§e/team accept§7 - 接受隊伍邀請");
            player.sendMessage("§e/team reject§7 - 拒絕隊伍邀請");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessage("missing_team_name"));
                return true;
            }
            if (plugin.getPlayerTeam(player) != null) {
                player.sendMessage(plugin.getMessage("already_in_team"));
                return true;
            }
            String teamName = args[1];
            if (plugin.getTeams().containsKey(teamName)) {
                player.sendMessage(plugin.getMessage("team_name_exists"));
                return true;
            }
            Team team = new Team(teamName, player);
            plugin.addTeam(team);
            plugin.addPlayerToTeam(player, team);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team", teamName);
            player.sendMessage(plugin.getMessage("team_created", placeholders));
        } else if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessage("missing_player_name"));
                return true;
            }
            Team team = plugin.getPlayerTeam(player);
            if (team == null || !team.getLeader().equals(player)) {
                player.sendMessage(plugin.getMessage("not_leader_or_no_team"));
                return true;
            }
            if (team.getMembers().size() >= plugin.getMaxTeamMembers()) {
                player.sendMessage(plugin.getMessage("team_full_add"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getMessage("player_not_online"));
                return true;
            }
            if (plugin.getPlayerTeam(target) != null) {
                player.sendMessage(plugin.getMessage("player_in_other_team"));
                return true;
            }
            // 發送邀請
            pendingInvitations.put(target.getUniqueId(), player.getUniqueId());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            player.sendMessage(plugin.getMessage("invite_sent", placeholders));

            placeholders = new HashMap<>();
            placeholders.put("inviter", player.getName());
            placeholders.put("team", team.getName());
            TextComponent message = new TextComponent(plugin.getMessage("invite_message", placeholders));
            TextComponent accept = new TextComponent(plugin.getMessage("accept_invite"));
            accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team accept"));
            TextComponent reject = new TextComponent(plugin.getMessage("reject_invite"));
            reject.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team reject"));

            message.addExtra(accept);
            message.addExtra(" ");
            message.addExtra(reject);

            target.spigot().sendMessage(message);

            // 設置邀請過期時間
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (pendingInvitations.containsKey(target.getUniqueId())) {
                        pendingInvitations.remove(target.getUniqueId());
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("player", target.getName());
                        player.sendMessage(plugin.getMessage("invite_expired", placeholders));
                    }
                }
            }.runTaskLater(plugin, plugin.getInviteExpiryTime() * 20L); // 邀請過期時間
        } else if (args[0].equalsIgnoreCase("leave")) {
            Team team = plugin.getPlayerTeam(player);
            if (team == null) {
                player.sendMessage(plugin.getMessage("not_in_team"));
                return true;
            }
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
        } else if (args[0].equalsIgnoreCase("gui")) {
            Team team = plugin.getPlayerTeam(player);
            if (team == null) {
                player.sendMessage(plugin.getMessage("not_in_team"));
                return true;
            }
            plugin.getGuiListener().openTeamStatusGUI(player, team);
        } else if (args[0].equalsIgnoreCase("accept")) {
            UUID inviterId = pendingInvitations.remove(player.getUniqueId());
            if (inviterId == null) {
                player.sendMessage(plugin.getMessage("no_pending_invite"));
                return true;
            }
            Player inviter = Bukkit.getPlayer(inviterId);
            if (inviter == null) {
                player.sendMessage(plugin.getMessage("inviter_not_online"));
                return true;
            }
            Team team = plugin.getPlayerTeam(inviter);
            if (team == null) {
                player.sendMessage(plugin.getMessage("inviter_no_team"));
                return true;
            }
            if (team.getMembers().size() >= plugin.getMaxTeamMembers()) {
                player.sendMessage(plugin.getMessage("team_full_join"));
                return true;
            }
            plugin.addPlayerToTeam(player, team);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team", team.getName());
            player.sendMessage(plugin.getMessage("team_joined", placeholders));
            placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            inviter.sendMessage(plugin.getMessage("invite_accepted", placeholders));
        } else if (args[0].equalsIgnoreCase("reject")) {
            UUID inviterId = pendingInvitations.remove(player.getUniqueId());
            if (inviterId == null) {
                player.sendMessage(plugin.getMessage("no_pending_invite"));
                return true;
            }
            Player inviter = Bukkit.getPlayer(inviterId);
            if (inviter != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                inviter.sendMessage(plugin.getMessage("invite_rejected", placeholders));
            }
            player.sendMessage(plugin.getMessage("invite_rejected_self"));
        } else {
            player.sendMessage(plugin.getMessage("unknown_command"));
        }

        return true;
    }
}
