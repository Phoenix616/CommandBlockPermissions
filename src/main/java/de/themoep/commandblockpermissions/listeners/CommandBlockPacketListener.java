package de.themoep.commandblockpermissions.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Charsets;
import de.themoep.commandblockpermissions.CommandBlockMode;
import de.themoep.commandblockpermissions.CommandBlockPermissions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private final Constructor<?> packetDataSerializer;
    private final CommandMap bukkitCommandMap;
    private Field b = null;

    public CommandBlockPacketListener(CommandBlockPermissions plugin) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.CUSTOM_PAYLOAD);
        this.plugin = plugin;

        String packageName = plugin.getServer().getClass().getPackage().getName();
        String serverVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
        packetDataSerializer = Class.forName("net.minecraft.server." + serverVersion + ".PacketDataSerializer").getConstructor(ByteBuf.class);

        Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
        commandMapField.setAccessible(true);
        bukkitCommandMap = (CommandMap) commandMapField.get(plugin.getServer());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        try {
            Channel channel = Channel.valueOf(event.getPacket().getStrings().read(0).replace('|', '_'));

            if (b == null) {
                b = event.getPacket().getHandle().getClass().getDeclaredField("b");
                b.setAccessible(true);
            }

            ByteBuf buf = (ByteBuf) b.get(event.getPacket().getHandle());

            switch (channel) {
                case MC_AdvCmd:
                case MC_AdvCdm:
                    byte type = buf.readByte();
                    if (type == 0) { // Command Block
                        handlePluginMessage(event, buf, false, false);
                    } else if (type == 1) { // Command Minecart
                        handlePluginMessage(event, buf, false, true);
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Received plugin message from " + event.getPlayer().getName() + " on channel " + channel + " which's first byte wasn't 0 or 1 (" + type + ")");
                        return;
                    }
                    break;
                case MC_AutoCmd:
                    handlePluginMessage(event, buf, true, false);
                    break;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a channel we want to listen on
        } catch (NoSuchFieldException | IllegalAccessException | IOException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void handlePluginMessage(PacketEvent event, ByteBuf buf, boolean autoCmd, boolean minecart) throws IllegalAccessException, IOException, InvocationTargetException, InstantiationException {
        if (!event.getPlayer().isOp() || plugin.checkOps()) {
            if (!event.getPlayer().hasPermission("cbp.commandblock.change")) {
                event.setCancelled(true);
                return;
            }
        }

        int x = 0;
        int y = 0;
        int z = 0;
        int entityId = 0;
        if (minecart) {
            entityId = buf.readInt();
        } else {
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
        }
        String commandString = readString(buf);

        boolean trackOutput = buf.readBoolean();

        CommandBlockMode mode = CommandBlockMode.REDSTONE;
        boolean isConditional = false;
        boolean automatic = false;

        if (autoCmd) {
            mode = CommandBlockMode.valueOf(readString(buf));
            isConditional = buf.readBoolean();
            automatic = buf.readBoolean();
        }

        if (!event.getPlayer().isOp() || plugin.checkOps()) {
            boolean wasOP = event.getPlayer().isOp();
            event.getPlayer().setOp(false);
            if (!commandString.isEmpty() && !commandString.toLowerCase().startsWith("cbp disabled ")) {
                String checkCommandString = commandString;
                if (checkCommandString.startsWith("/")) {
                    checkCommandString = checkCommandString.substring(1);
                }
                String[] commandArray = checkCommandString.split(" ");
                String commandName;
                Command command;
                boolean hasPerm = event.getPlayer().hasPermission("cbp.perm.*");
                List<String> deniedCommands = new ArrayList<>();
                do {
                    commandName = commandArray[0];
                    command = bukkitCommandMap.getCommand(commandArray[0]);
                    if (command != null) {
                        boolean tempHasPerm = plugin.usePlayerPermissions() &&
                                event.getPlayer().hasPermission(command.getPermission())
                                && !event.getPlayer().hasPermission("-cbp.perm." + command.getPermission())
                                || event.getPlayer().hasPermission("cbp.perm." + command.getPermission());
                        if (!tempHasPerm) deniedCommands.add(commandName);
                        hasPerm = hasPerm && tempHasPerm;
                        if ("execute".equalsIgnoreCase(command.getName())) {
                            int executeCommandLen = 5;
                            if ("detect".equalsIgnoreCase(commandArray[5]) && commandArray.length > 11) {
                                executeCommandLen = 11;
                            }
                            commandArray = Arrays.copyOfRange(commandArray, executeCommandLen, commandArray.length);
                        }
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Failed to check permissions for command '" + (commandString.length() > 210 ? commandString.substring(0, 200) + "..." : commandString) + "'!");
                    }
                } while ("execute".equalsIgnoreCase(commandName));
                if (!hasPerm) {
                    plugin.warning(event.getPlayer().getName() + " doesn't have the permission to set the command '" + commandString + "'!");
                    event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to set the command " + deniedCommands + " in Command " + (minecart ? "Minecarts" : "Blocks") + "!");
                    commandString = "cbp disabled " + event.getPlayer().getName() + " " + commandString;
                }
            }

            String optPerm = "cbp.options.";

            if (!trackOutput && !event.getPlayer().hasPermission(optPerm + "disabletrackoutput")) {
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to disable tracking of the output!");
                trackOutput = true;
            }

            if (!event.getPlayer().hasPermission(optPerm + "mode." + mode.toString().toLowerCase())) {
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to use the " + mode.getName() + "(" + mode.toString().toLowerCase() + ") mode!");
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
                        commandString = "cbp disabled " + event.getPlayer().getName() + " " + commandString;
                    }
                }
            }

            if (isConditional && !event.getPlayer().hasPermission(optPerm + "conditional")) {
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have the permission to use the conditional mode!");
                isConditional = false;
            }

            event.getPlayer().setOp(wasOP);
        }

        if (plugin.getServer().spigot().getConfig().getBoolean("settings.bungeecord")) {
            // BungeeCord seems to have an issue with some packet that the server sends
            // after manipulating the command block packet? Not sure why 'though, it works
            // fine without BungeeCord. Lets work around it anyways.

            // Fuck this shit, there is no CommandBlock api in Bukkit ;_;
            event.setCancelled(true);
            if (minecart) {

            } else {
                Block block = event.getPlayer().getWorld().getBlockAt(x, y, z);
                CommandBlock cb = (CommandBlock) block.getState();
            }

        } else {
            ByteBuf out = Unpooled.buffer();

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
            writeString(commandString, out);
            out.writeBoolean(trackOutput);
            if (autoCmd) {
                writeString(mode.toString(), out);
                out.writeBoolean(isConditional);
                out.writeBoolean(automatic);
            }

            Object data = packetDataSerializer.newInstance(out);

            b.set(event.getPacket().getHandle(), data);
        }
    }

    private String readString(ByteBuf buf) throws IOException {
        int maxLength = Short.MAX_VALUE;
        int length = readVarInt(buf);

        if (length > maxLength * 4) {
            throw new DecoderException("The received length of the encoded string buffer is too long! (length: " + length + ", allowed:" + maxLength * 4 + ")");
        } else if (length < 0) {
            throw new DecoderException("String buffer length is less than zero. Wat?");
        } else {
            byte[] tempByte = new byte[length];
            buf.readBytes(tempByte);
            String s = new String(tempByte, Charsets.UTF_8);

            if (s.length() > maxLength) {
                throw new DecoderException("The received string is too long! (length" + length + ", allowed" + maxLength + ")");
            } else {
                return s;
            }
        }
    }

    private ByteBuf writeString(String string, ByteBuf output) {
        int maxLength = Short.MAX_VALUE;
        byte[] bytes = string.getBytes(Charsets.UTF_8);

        if (bytes.length > maxLength) {
            throw new EncoderException("The length of the encoded string is too big (length: " + string.length() + ", allowed " + maxLength + ")");
        } else {
            writeVarInt(bytes.length, output);
            output.writeBytes(bytes);
            return output;
        }
    }

    private int readVarInt(ByteBuf buf) throws IOException {
        int out = 0;
        int bytes = 0;
        byte in;
        do {
            in = buf.readByte();

            out |= (in & 127) << (bytes++ * 7);

            if (bytes > 5) {
                throw new RuntimeException("The receives VarInt is too big (is " + bytes + ", allowed: 5)");
            }

        } while ((in & 128) == 128);

        return out;
    }

    private ByteBuf writeVarInt(int value, ByteBuf output) {
        do {
            int temp = value & 127;
            value >>>= 7;

            if (value != 0) {
                temp |= 128;
            }
            output.writeByte(temp);
        } while (value != 0);
        return output;
    }

    public enum Channel {
        MC_AdvCmd,
        MC_AdvCdm,
        MC_AutoCmd;
    }
}
