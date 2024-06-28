package com.johnson.teamparty;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDisconnectListener implements Listener {
    private final Teamparty plugin;

    public PlayerDisconnectListener(Teamparty plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDisconnectTimes().put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
}
