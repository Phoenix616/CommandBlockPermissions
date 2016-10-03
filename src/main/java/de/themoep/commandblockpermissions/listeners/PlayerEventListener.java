package de.themoep.commandblockpermissions.listeners;

import com.google.common.collect.ImmutableSet;
import de.themoep.commandblockpermissions.CommandBlockPermissions;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

/**
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public class PlayerEventListener implements Listener {
    private final CommandBlockPermissions plugin;

    private final static Set<Material> commandBlocks = ImmutableSet.of(Material.COMMAND, Material.COMMAND_CHAIN, Material.COMMAND_REPEATING);

    public PlayerEventListener(CommandBlockPermissions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isCommandBlock(event.getClickedBlock().getType())) {
                boolean allowed = !plugin.checkOps() && event.getPlayer().isOp() || event.getPlayer().hasPermission("commandblockpermissions.commandblock.access");
                event.setCancelled(!allowed);
            } else if (event.getItem() != null && isCommandBlock(event.getItem().getType())) {
                boolean allowed = !plugin.checkOps() && event.getPlayer().isOp() || event.getPlayer().hasPermission("commandblockpermissions.commandblock.place");
                event.setCancelled(!allowed);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isCommandBlock(event.getBlock().getType())) {
            boolean allowed = !plugin.checkOps() && event.getPlayer().isOp() || event.getPlayer().hasPermission("commandblockpermissions.commandblock.place");
            event.setCancelled(!allowed);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isCommandBlock(event.getBlock().getType())) {
            boolean allowed = !plugin.checkOps() && event.getPlayer().isOp() || event.getPlayer().hasPermission("commandblockpermissions.commandblock.break");
            event.setCancelled(!allowed);
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (isCommandBlock(event.getBlock().getType())) {
            boolean allowed = !plugin.checkOps() && event.getPlayer().isOp() || event.getPlayer().hasPermission("commandblockpermissions.commandblock.break");
            event.setCancelled(!allowed);
        }
    }

    private boolean isCommandBlock(Material type) {
        return type == Material.COMMAND || type == Material.COMMAND_CHAIN || type == Material.COMMAND_REPEATING;
    }
}
