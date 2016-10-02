package de.themoep.commandblockpermissions.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.themoep.commandblockpermissions.CommandBlockMode;
import de.themoep.commandblockpermissions.CommandBlockPermissions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;

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
public class CommandBlockPacketListener extends PacketAdapter {

    private final CommandBlockPermissions plugin;

    public CommandBlockPacketListener(CommandBlockPermissions plugin) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.CUSTOM_PAYLOAD);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        try {
            Channel channel = Channel.valueOf(event.getPacket().getStrings().read(0).replace('|', '_'));

            ByteArrayDataInput in = ByteStreams.newDataInput(event.getPacket().getByteArrays().read(0));
            switch (channel) {
                case MC_AdvCmd:
                case MC_AdvCdm:
                    byte type = in.readByte();
                    if (type == 0) { // Command Block
                        handlePluginMessage(event, in, false, false);
                    } else if (type == 1) { // Command Minecart
                        handlePluginMessage(event, in, false, true);
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Received plugin message from " + event.getPlayer().getName() + " on channel " + channel + " which's first byte wasn't 0 or 1 (" + type + ")");
                        return;
                    }
                    break;
                case MC_AutoCmd:
                    handlePluginMessage(event, in, true, false);
                    break;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a channel we want to listen on
        }
    }

    private void handlePluginMessage(PacketEvent event, ByteArrayDataInput in, boolean autoCmd, boolean minecart) {
        int x = 0;
        int y = 0;
        int z = 0;
        int entityId = 0;
        if (minecart) {
            entityId = in.readInt();
        } else {
            x = in.readInt();
            y = in.readInt();
            z = in.readInt();
        }
        String commandString = in.readUTF();
        boolean trackOutput = in.readBoolean();

        CommandBlockMode mode = CommandBlockMode.REDSTONE;
        boolean isConditional = false;
        boolean automatic = false;

        if (autoCmd) {
            mode = CommandBlockMode.valueOf(in.readUTF());
            isConditional = in.readBoolean();
            automatic = in.readBoolean();
        }

        if (!event.getPlayer().isOp() || plugin.checkOps()) {
            if (!commandString.isEmpty()) {
                Command command = plugin.getServer().getPluginCommand(commandString.split(" ")[0]);
                if (command != null) {
                    boolean hasPerm = plugin.usePlayerPermissions() &&
                            event.getPlayer().hasPermission(command.getPermission())
                            && !event.getPlayer().hasPermission("-commandblockpermissions.permission." + command.getPermission())
                            || event.getPlayer().hasPermission("commandblockpermissions.permission." + command.getPermission());
                    if (!hasPerm) {
                        plugin.warning(event.getPlayer().getName() + " doesn't have the permission to set the command '" + commandString + "'!");
                        event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to set the command " + command.getName() + " in Command " + (minecart ? "Minecarts" : "Blocks") + "!");
                        commandString = "cbp disabled " + event.getPlayer() + " " + commandString;
                    }
                } else {
                    plugin.getLogger().log(Level.WARNING, "Failed to check permissions for command '" + commandString + "'!");
                }
            }

            String optPerm = "commandblockpermissions.options.";

            if (!trackOutput && !event.getPlayer().hasPermission(optPerm + "disabletrackoutput")){
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to disable tracking of the output!");
                trackOutput = true;
            }

            if (!event.getPlayer().hasPermission(optPerm + "mode." + mode.toString().toLowerCase())) {
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to use the " + mode + " mode!");
                CommandBlockMode oldMode = mode;
                for (CommandBlockMode m : CommandBlockMode.values()) {
                    if (m == mode) {
                        continue;
                    }
                    if (event.getPlayer().hasPermission(optPerm + "mode." + mode.toString().toLowerCase())) {
                        mode = m;
                    }
                }
                if (oldMode == mode) {
                    mode = CommandBlockMode.REDSTONE;
                    if (!commandString.startsWith("cbp disabled ")) {
                        commandString = "cbp disabled " + event.getPlayer() + " " + commandString;
                    }
                }
            }

            if (isConditional && !event.getPlayer().hasPermission(optPerm + "conditional")) {
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to use the conditional mode!");
                isConditional = false;
            }
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        if (!autoCmd) {
            out.writeByte(minecart ? 1 : 0);
        }

        if (minecart) {
            out.writeInt(entityId);
        } else {
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(z);
        }
        out.writeUTF(commandString);
        out.writeBoolean(trackOutput);
        if (autoCmd) {
            out.writeUTF(mode.toString());
            out.writeBoolean(isConditional);
            out.writeBoolean(automatic);
        }

        event.getPacket().getByteArrays().write(0, out.toByteArray());
    }

    public enum Channel {
        MC_AdvCmd,
        MC_AdvCdm,
        MC_AutoCmd;
    }
}
