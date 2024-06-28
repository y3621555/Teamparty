package com.johnson.teamparty;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TeamAPI {

    private final Teamparty plugin;
    private final Map<String, Team> teams;
    private final Map<UUID, Team> playerTeams;
    private final Map<UUID, Long> disconnectTimes;
    private final int maxTeamMembers;
    private final int disconnectTimeLimit; // 以秒為單位
    private final int inviteExpiryTime; // 以秒為單位

    public TeamAPI(Teamparty plugin, Map<String, Team> teams, Map<UUID, Team> playerTeams, Map<UUID, Long> disconnectTimes, int maxTeamMembers, int disconnectTimeLimit, int inviteExpiryTime) {
        this.plugin = plugin;
        this.teams = teams;
        this.playerTeams = playerTeams;
        this.disconnectTimes = disconnectTimes;
        this.maxTeamMembers = maxTeamMembers;
        this.disconnectTimeLimit = disconnectTimeLimit;
        this.inviteExpiryTime = inviteExpiryTime;
    }

    public boolean isPlayerInTeam(Player player) {
        return playerTeams.containsKey(player.getUniqueId());
    }

    public boolean areMembersNearby(Player player, double radius) {
        Team team = playerTeams.get(player.getUniqueId());
        if (team == null) {
            return false;
        }
        return team.areMembersNearby(player, radius);
    }

    public Team getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public void disbandTeam(Team team) {
        if (teams.containsKey(team.getName())) {
            for (Player member : team.getMembers()) {
                playerTeams.remove(member.getUniqueId());
                member.sendMessage("§c隊伍已被解散。");
            }
            teams.remove(team.getName());
            plugin.getLogger().info("隊伍 " + team.getName() + " 已被遣散。");
        }
    }

    public void createTeam(String teamName, Player leader) {
        if (teams.containsKey(teamName)) {
            throw new IllegalArgumentException("隊伍名稱已存在");
        }
        Team team = new Team(teamName, leader);
        teams.put(teamName, team);
        playerTeams.put(leader.getUniqueId(), team);
        plugin.getLogger().info("隊伍 " + teamName + " 已被創建，隊長為 " + leader.getName() + "。");
    }

    public void addPlayerToTeam(Player player, Team team) {
        if (playerTeams.containsKey(player.getUniqueId())) {
            throw new IllegalArgumentException("玩家已經在一個隊伍中");
        }
        if (team.getMembers().size() >= maxTeamMembers) {
            throw new IllegalArgumentException("隊伍成員已達上限");
        }
        team.addMember(player);
        playerTeams.put(player.getUniqueId(), team);
        plugin.getLogger().info("玩家 " + player.getName() + " 已加入隊伍 " + team.getName() + "。");
    }

    public void removePlayerFromTeam(UUID playerId) {
        Team team = playerTeams.remove(playerId);
        if (team != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                team.removeMember(player);
                plugin.getLogger().info("玩家 " + player.getName() + " 已被移出隊伍 " + team.getName() + "。");
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
                team.removeMember(offlinePlayer);
                plugin.getLogger().info("玩家 " + offlinePlayer.getName() + " 已被移出隊伍 " + team.getName() + " (離線狀態)。");
            }
            updateTeamStatusGUIForOnlineMembers(team);
        } else {
            plugin.getLogger().info("隊伍中找不到玩家 " + playerId + "。");
        }
    }

    public List<Team> getAllTeams() {
        return teams.values().stream().collect(Collectors.toList());
    }

    public Set<Player> getTeamMembers(Team team) {
        return team.getMembers();
    }

    public void changeTeamLeader(Team team, Player newLeader) {
        team.setLeader(newLeader);
        plugin.getLogger().info("隊伍 " + team.getName() + " 的新隊長為 " + newLeader.getName() + "。");
    }

    public String getTeamName(Team team) {
        return team.getName();
    }

    public List<String> getTeamMemberNames(Team team) {
        return team.getMembers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public boolean isLeader(Player player) {
        Team team = playerTeams.get(player.getUniqueId());
        return team != null && team.getLeader().equals(player);
    }

    public int getTeamSize(Team team) {
        return team.getMembers().size();
    }

    public void checkDisconnectedPlayers() {
        long currentTime = System.currentTimeMillis(); // 獲取當前時間（毫秒）
        long disconnectLimitMillis = TimeUnit.SECONDS.toMillis(disconnectTimeLimit); // 將秒轉換為毫秒

        for (Map.Entry<UUID, Long> entry : disconnectTimes.entrySet()) {
            UUID playerId = entry.getKey();
            long disconnectTime = entry.getValue();

            if (currentTime - disconnectTime > disconnectLimitMillis) {
                Team team = playerTeams.get(playerId);
                if (team != null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
                    String playerName = offlinePlayer.getName();

                    if (team.getLeader().getUniqueId().equals(playerId)) {
                        // 隊長斷線過久，遣散隊伍
                        for (Player member : team.getMembers()) {
                            member.sendMessage("§c隊伍已被遣散，因為隊長 " + playerName + " 離線過久。");
                        }
                        disbandTeam(team);
                    } else {
                        // 隊員斷線過久，移除隊員並通知其他成員
                        if (!offlinePlayer.isOnline()) {
                            removePlayerFromTeam(playerId);
                            for (Player member : team.getMembers()) {
                                member.sendMessage("§c玩家 " + playerName + " 因為離線過久已被移出隊伍。");
                            }
                            updateTeamStatusGUIForOnlineMembers(team);
                        }
                    }
                }
            }
        }

        // 移除已處理的斷線記錄
        disconnectTimes.entrySet().removeIf(entry -> currentTime - entry.getValue() > disconnectLimitMillis);
    }

    private void updateTeamStatusGUIForOnlineMembers(Team team) {
        for (Player member : team.getMembers()) {
            if (member.isOnline()) {
                InventoryView openInventory = member.getOpenInventory();
                if (openInventory.getTitle().equals(plugin.getMessage("gui_title"))) {
                    plugin.getGuiListener().updateTeamStatusGUI(openInventory.getTopInventory(), team);
                    //plugin.getLogger().info("更新玩家 " + member.getName() + " 的隊伍狀態GUI。");
                } else {
                    //plugin.getLogger().info("玩家 " + member.getName() + " 並未打開隊伍狀態GUI。");
                }
            } else {
                //plugin.getLogger().info("玩家 " + member.getName() + " 處於離線狀態。");
            }
        }
    }
}
