package com.johnson.teamparty;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerReconnectListener implements Listener {
    private final Teamparty plugin;

    public PlayerReconnectListener(Teamparty plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getDisconnectTimes().remove(event.getPlayer().getUniqueId());
    }
}