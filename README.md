# CommandBlockPermissions
Bukkit plugin to manage command blocks with permissions. This includes making them view only or defining permissions for certain commands/options only!

Currently only operators in creative mode can access command blocks. The plan is to extend this to normal players without op/creative.

Builds: http://ci.minebench.de/job/CommandBlockPermissions/

## WARNING!
**This is an highly experimental plugin without any real testing!**

**You will not make me responsible for any damage that may result by the usage of this plugin!**

## Config

```yaml
logWarnings: true
# Toggle logging of warnings to the console/log file
# To receive warnings ingame use the following permission:
# commandblockpermissions.receivewarnings

checkOps: true
# Check permissions of ops as command blocks will execute
# with a higher permission level than they would themselves

usePlayerPermissions: false
# Allow players the usage of commands for which they already
# have the permissions to. This can cause issues with some
# plugins as they check additional permissions in their command
# resolver which this plugin can't check for.
# Use commandblockpermissions.permission.<commandpermission> to
# allow players only access to certain commands in command blocks!
```

## Permissions
|           Permission           |                        Description                         |
| -------------------------------|------------------------------------------------------------|
| cbp.perm.<command.permission>  | Player has permission while setting command block command  |
| cbp.commandblock.place         | Place command blocks                                       |
| cbp.commandblock.break         | Break command blocks                                       |
| cbp.commandblock.access        | Access command blocks                                      |
| cbp.commandblock.change        | Change command blocks                                      |
| cbp.command                    | Access to the plugin command                               |
| cbp.command.reload             | Reload the plugin via /cbp reload                          |
| cbp.options.disabletrackoutput | Disable tracking of output                                 |
| cbp.options.conditional        | Set command blocks to conditional mode                     |
| cbp.options.mode.redstone      | Use the redstone mode                                      |
| cbp.options.mode.auto          | Use the automatic mode                                     |
| cbp.options.mode.sequence      | Use the sequence mode                                      |
| cbp.receivewarnings            | Get warnings when a player tries to set a disabled command |
