package de.themoep.commandblockpermissions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.minecart.CommandMinecart;

import java.util.logging.Level;

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
public class CommandBlockPermissionsCommand implements CommandExecutor {
    private final CommandBlockPermissions plugin;

    public CommandBlockPermissionsCommand(CommandBlockPermissions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("cbp.command.reload")) {
                plugin.loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;
            }
        } else if (args.length > 1) {
            if ("disabled".equalsIgnoreCase(args[0])) {
                if (sender instanceof BlockCommandSender) {
                    Block block = ((BlockCommandSender) sender).getBlock();
                    plugin.warning("Command block in " + block.getWorld().getName() + " at " + block.getX() + " " + block.getY() + " " + block.getZ() + " tried to execute disabled command");
                } else if (sender instanceof CommandMinecart) {
                    Location loc = ((CommandMinecart) sender).getLocation();
                    plugin.warning("Command minecart in " + loc.getWorld().getName() + " at " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " tried to execute disabled command");
                } else {
                    sender.sendMessage("This internal command can only be executed by a Command Block/Minecart!");
                }
                return true;
            }
        }
        return false;
    }
}
