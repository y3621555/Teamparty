package com.johnson.teamparty;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class Team {
    private final String name;
    private final Set<Player> members = new HashSet<>();
    private Player leader;

    public Team(String name, Player leader) {
        this.name = name;
        this.leader = leader;
        members.add(leader);
    }

    public String getName() {
        return name;
    }

    public Player getLeader() {
        return leader;
    }

    public void setLeader(Player leader) {
        this.leader = leader;
    }

    public void addMember(Player player) {
        members.add(player);
    }

    public void removeMember(Player player) {
        members.remove(player);
    }

    public void removeMember(OfflinePlayer offlinePlayer) {
        members.removeIf(member -> member.getUniqueId().equals(offlinePlayer.getUniqueId()));
    }

    public boolean isMember(Player player) {
        return members.contains(player);
    }


    public Set<Player> getMembers() {
        return new HashSet<>(members); // 返回副本以避免外部修改
    }

    public boolean areMembersNearby(Player player, double radius) {
        for (Player member : members) {
            if (!member.equals(player) && member.getLocation().distance(player.getLocation()) <= radius) {
                return true;
            }
        }
        return false;
    }

    public void disband() {
        for (Player member : members) {
            member.sendMessage("§c隊長已離隊，隊伍已解散。");
        }
        members.clear();
    }
}
