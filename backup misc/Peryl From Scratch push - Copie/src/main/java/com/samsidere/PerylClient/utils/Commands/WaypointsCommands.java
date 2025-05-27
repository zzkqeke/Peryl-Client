package com.samsidere.PerylClient.utils.Commands; // <--- MODIFIÉ

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.entity.player.EntityPlayerMP;
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

import com.samsidere.PerylClient.utils.ModWaypoint; // <--- MODIFIÉ
import com.samsidere.PerylClient.utils.Movements.pathfinder; // <--- MODIFIÉ

public class WaypointsCommands extends CommandBase { // <--- MODIFIÉ

    private static Map<String, ModWaypoint> waypoints = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    private static pathfinder playerPathFollowerInstance; // <--- MODIFIÉ

    public static void setPlayerPathFollower(pathfinder instance) { // <--- MODIFIÉ
        playerPathFollowerInstance = instance;
    }

    public static void initialize(File configDir) {
        configFile = new File(configDir, "perylclient_waypoints.json");
        loadWaypoints();
    }

    public static ModWaypoint getWaypoint(String name) {
        return waypoints.get(name.toLowerCase());
    }

    @Override
    public String getCommandName() {
        return "waypoint";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/waypoint <add/remove/list/goto> [name] [x] [y] [z]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            Minecraft mc = Minecraft.getMinecraft(); // Accès au client pour le pathfinder

            if (args.length == 0) {
                player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
                return;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "add":
                    if (args.length == 2) { // /waypoint add <name> (uses current pos)
                        String name = args[1].toLowerCase();
                        BlockPos currentPos = player.getPosition();
                        waypoints.put(name, new ModWaypoint(currentPos, name));
                        saveWaypoints();
                        player.addChatMessage(new ChatComponentText("Waypoint '" + name + "' added at " + currentPos.getX() + ", " + currentPos.getY() + ", " + currentPos.getZ()));
                    } else if (args.length == 5) { // /waypoint add <name> <x> <y> <z>
                        String name = args[1].toLowerCase();
                        try {
                            int x = Integer.parseInt(args[2]);
                            int y = Integer.parseInt(args[3]);
                            int z = Integer.parseInt(args[4]);
                            BlockPos pos = new BlockPos(x, y, z);
                            waypoints.put(name, new ModWaypoint(pos, name));
                            saveWaypoints();
                            player.addChatMessage(new ChatComponentText("Waypoint '" + name + "' added at " + x + ", " + y + ", " + z));
                        } catch (NumberFormatException e) {
                            player.addChatMessage(new ChatComponentText("Invalid coordinates. Usage: /waypoint add <name> [x] [y] [z]"));
                        }
                    } else {
                        player.addChatMessage(new ChatComponentText("Usage: /waypoint add <name> [x] [y] [z]"));
                    }
                    break;

                case "remove":
                    if (args.length == 2) {
                        String name = args[1].toLowerCase();
                        if (waypoints.remove(name) != null) {
                            saveWaypoints();
                            player.addChatMessage(new ChatComponentText("Waypoint '" + name + "' removed."));
                        } else {
                            player.addChatMessage(new ChatComponentText("Waypoint '" + name + "' not found."));
                        }
                    } else {
                        player.addChatMessage(new ChatComponentText("Usage: /waypoint remove <name>"));
                    }
                    break;

                case "list":
                    if (waypoints.isEmpty()) {
                        player.addChatMessage(new ChatComponentText("No waypoints saved."));
                    } else {
                        player.addChatMessage(new ChatComponentText("--- Saved Waypoints ---"));
                        waypoints.forEach((name, wp) -> player.addChatMessage(new ChatComponentText(" - " + wp.name + ": " + wp.x + ", " + wp.y + ", " + wp.z)));
                    }
                    break;

                case "goto":
                    if (args.length == 2) {
                        String name = args[1].toLowerCase();
                        ModWaypoint targetWaypoint = waypoints.get(name);
                        if (targetWaypoint != null) {
                            if (playerPathFollowerInstance != null) {
                                // Simple path pour l'exemple, à remplacer par un vrai A*
                                List<BlockPos> path = Arrays.asList(player.getPosition(), targetWaypoint.getBlockPos());
                                playerPathFollowerInstance.startPath(path);
                                player.addChatMessage(new ChatComponentText("Path started to waypoint '" + name + "'."));
                            } else {
                                player.addChatMessage(new ChatComponentText("Pathfinder not initialized."));
                            }
                        } else {
                            player.addChatMessage(new ChatComponentText("Waypoint '" + name + "' not found."));
                        }
                    } else {
                        player.addChatMessage(new ChatComponentText("Usage: /waypoint goto <name>"));
                    }
                    break;

                default:
                    player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
                    break;
            }
        }
    }

    private static void loadWaypoints() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Type type = new TypeToken<HashMap<String, ModWaypoint>>() {}.getType();
                waypoints = GSON.fromJson(reader, type);
                if (waypoints == null) {
                    waypoints = new HashMap<>(); // Initialise si le fichier est vide ou corrompu
                }
                System.out.println("Waypoints loaded: " + waypoints.size());
            } catch (IOException e) {
                System.err.println("Failed to load waypoints: " + e.getMessage());
                waypoints = new HashMap<>(); // Initialise en cas d'erreur
            }
        }
    }

    private static void saveWaypoints() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(waypoints, writer);
            System.out.println("Waypoints saved: " + waypoints.size());
        } catch (IOException e) {
            System.err.println("Failed to save waypoints: " + e.getMessage());
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true; // Everyone can use this command for now
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "add", "remove", "list", "goto");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("goto"))) {
            return getListOfStringsMatchingLastWord(args, waypoints.keySet().stream().collect(Collectors.toList()));
        }
        return null;
    }
}