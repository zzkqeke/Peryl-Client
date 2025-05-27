package com.samsidere.PerylClient.utils.Commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.samsidere.PerylClient.utils.ModWaypoint;
import com.samsidere.PerylClient.utils.Movements.pathfinder;


public class WaypointsCommands extends CommandBase {

    public static Map<String, ModWaypoint> waypoints = new HashMap<>();
    private static File waypointsFile;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static pathfinder playerPathFollower;
    private final Minecraft mc = Minecraft.getMinecraft();

    public static void initialize(File configDir) {
        waypointsFile = new File(configDir, "PerylClientWaypoints.json");
        loadWaypoints();
    }

    public static void setPlayerPathFollower(pathfinder pathFollower) {
        WaypointsCommands.playerPathFollower = pathFollower;
    }

    public static ModWaypoint getWaypoint(String name) {
        return waypoints.get(name.toLowerCase());
    }

    @Override
    public String getCommandName() {
        return "wp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "§cUsage: /wp add <name> | /wp remove <name> | /wp list | /wp goto <name> | /wp clear";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        System.out.println("DEBUG: Command sender type is: " + sender.getClass().getName());

        final EntityPlayerSP player; // Rendre player effectivement final
        if (sender instanceof EntityPlayerSP) {
            player = (EntityPlayerSP) sender;
        } else if (mc.thePlayer != null) {
            player = mc.thePlayer;
        } else {
            player = null; // Assurez-vous qu'elle est initialisée si aucune condition n'est remplie
        }

        if (player == null) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be run by a player, or the player instance is not available."));
            return;
        }

        if (args.length == 0) {
            player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                if (args.length >= 2) {
                    String name = args[1].toLowerCase();
                    BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
                    ModWaypoint newWaypoint = new ModWaypoint(pos, name);
                    waypoints.put(name, newWaypoint);
                    saveWaypoints();
                    player.addChatMessage(new ChatComponentText("§aWaypoint '" + name + "' added at X:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ()));
                } else {
                    player.addChatMessage(new ChatComponentText("§cUsage: /wp add <name>"));
                }
                break;
            case "remove":
                if (args.length >= 2) {
                    String name = args[1].toLowerCase();
                    if (waypoints.remove(name) != null) {
                        saveWaypoints();
                        player.addChatMessage(new ChatComponentText("§aWaypoint '" + name + "' removed."));
                    } else {
                        player.addChatMessage(new ChatComponentText("§cWaypoint '" + name + "' not found."));
                    }
                } else {
                    player.addChatMessage(new ChatComponentText("§cUsage: /wp remove <name>"));
                }
                break;
            case "list":
                if (waypoints.isEmpty()) {
                    player.addChatMessage(new ChatComponentText("§eNo waypoints saved yet."));
                } else {
                    player.addChatMessage(new ChatComponentText("§a--- Saved Waypoints ---"));
                    waypoints.forEach((name, wp) ->
                        player.addChatMessage(new ChatComponentText("§b" + wp.name + ": X:" + wp.x + " Y:" + wp.y + " Z:" + wp.z))
                    );
                }
                break;
            case "goto":
                if (args.length >= 2) {
                    String name = args[1].toLowerCase();
                    ModWaypoint targetWp = waypoints.get(name);
                    if (targetWp != null) {
                        if (playerPathFollower != null) {
                            List<BlockPos> path = Arrays.asList(targetWp.getBlockPos());
                            playerPathFollower.startPath(path);
                            player.addChatMessage(new ChatComponentText("§aGoing to waypoint: " + name));
                        } else {
                            player.addChatMessage(new ChatComponentText("§cPathfinder not initialized."));
                        }
                    } else {
                        player.addChatMessage(new ChatComponentText("§cWaypoint '" + name + "' not found."));
                    }
                } else {
                    player.addChatMessage(new ChatComponentText("§cUsage: /wp goto <name>"));
                }
                break;
            case "clear":
                waypoints.clear();
                saveWaypoints();
                player.addChatMessage(new ChatComponentText("§aAll waypoints cleared."));
                break;
            default:
                player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
                break;
        }
    }

    private static void saveWaypoints() {
        try (FileWriter writer = new FileWriter(waypointsFile)) {
            gson.toJson(waypoints, writer);
        } catch (IOException e) {
            System.err.println("Failed to save waypoints: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadWaypoints() {
        if (waypointsFile.exists()) {
            try (FileReader reader = new FileReader(waypointsFile)) {
                Type type = new TypeToken<Map<String, ModWaypoint>>() {}.getType();
                waypoints = gson.fromJson(reader, type);
                if (waypoints == null) {
                    waypoints = new HashMap<>();
                }
            } catch (IOException e) {
                System.err.println("Failed to load waypoints: " + e.getMessage());
                e.printStackTrace();
                waypoints = new HashMap<>();
            }
        } else {
            waypoints = new HashMap<>();
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "add", "remove", "list", "goto", "clear");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("goto")) {
                return getListOfStringsMatchingLastWord(args, waypoints.keySet().toArray(new String[0]));
            }
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}