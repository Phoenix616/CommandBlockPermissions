package de.themoep.commandblockpermissions;

import com.google.common.io.ByteArrayDataInput;
import de.themoep.commandblockpermissions.listeners.CommandBlockPacketListener;

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
public class CommandBlockInfo {
    private CommandBlockMode mode = CommandBlockMode.REDSTONE;
    private boolean isConditional = false;
    private boolean automatic = false;
    private int x;
    private int y;
    private int z;
    private String command;
    private boolean trackOutput;

    public void loadFromAdvCmd(ByteArrayDataInput in) {

        x = in.readInt();
        y = in.readInt();
        z = in.readInt();
        command = in.readUTF();
        trackOutput = in.readBoolean();
    }

    public CommandBlockMode getMode() {
        return mode;
    }

    public boolean isConditional() {
        return isConditional;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public static CommandBlockInfo fromPluginMessage(CommandBlockPacketListener.Channel channel, ByteArrayDataInput in) {
        return null;
    }
}
