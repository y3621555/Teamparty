package com.johnson.teamparty;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Teamparty extends JavaPlugin {

    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private final Map<UUID, Long> disconnectTimes = new HashMap<>();
    private TeamAPI teamAPI;
    private GUIListener guiListener;
    private FileConfiguration messages;
    private FileConfiguration config;

    private int maxTeamMembers;
    private int disconnectTimeLimit;
    private int inviteExpiryTime;

    @Override
    public void onEnable() {
        // 插件啟動邏輯
        getLogger().info("TeamParty插件已啟動");

        // 加載config.yml和messages.yml
        loadConfig();
        loadMessages();

        // 註冊指令和事件
        getCommand("team").setExecutor(new TeamCommandExecutor(this));
        guiListener = new GUIListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerReconnectListener(this), this);

        // 初始化TeamAPI
        teamAPI = new TeamAPI(this, teams, playerTeams, disconnectTimes, maxTeamMembers, disconnectTimeLimit, inviteExpiryTime);
        // 向Bukkit註冊API接口
        getServer().getServicesManager().register(TeamAPI.class, teamAPI, this, ServicePriority.Normal);

        // 定時任務檢查斷線玩家
        new BukkitRunnable() {
            @Override
            public void run() {
                teamAPI.checkDisconnectedPlayers();
            }
        }.runTaskTimer(this, 20L, 20L); // 每秒檢查一次
    }

    @Override
    public void onDisable() {
        getLogger().info("TeamParty插件已停用");
    }

    private void loadConfig() {
        saveDefaultConfig();
        config = getConfig();
        maxTeamMembers = config.getInt("max_team_members", 5);
        disconnectTimeLimit = config.getInt("disconnect_time_limit", 1800); // 預設為30分鐘（1800秒）
        inviteExpiryTime = config.getInt("invite_expiry_time", 30);
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messages.getString("messages." + key);
        if (message != null && placeholders != null) {
            for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                message = message.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
            }
        }
        return message;
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    public Map<String, Team> getTeams() {
        return teams;
    }

    public Map<UUID, Team> getPlayerTeams() {
        return playerTeams;
    }

    public Map<UUID, Long> getDisconnectTimes() {
        return disconnectTimes;
    }

    public int getMaxTeamMembers() {
        return maxTeamMembers;
    }

    public int getDisconnectTimeLimit() {
        return disconnectTimeLimit;
    }

    public int getInviteExpiryTime() {
        return inviteExpiryTime;
    }

    public void addTeam(Team team) {
        teams.put(team.getName(), team);
    }

    public void addPlayerToTeam(Player player, Team team) {
        playerTeams.put(player.getUniqueId(), team);
        team.addMember(player);
    }

    public void removePlayerFromTeam(Player player) {
        teamAPI.removePlayerFromTeam(player.getUniqueId());
    }

    public Team getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public TeamAPI getTeamAPI() {
        return teamAPI;
    }

    public GUIListener getGuiListener() {
        return guiListener;
    }
}
