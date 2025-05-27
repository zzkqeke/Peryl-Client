import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
// CHANGEMENT ICI : Pour Forge, utiliser FMLPaths
import net.minecraftforge.fml.loading.FMLPaths; // Importation pour Forge

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaypointCommands {

    private static final Map<String, ModWaypoint> waypoints = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "waypoints.json";
    private static Path configPath;

    // Méthode d'initialisation pour charger les waypoints au démarrage du mod
    public static void initialize() {
        // CHANGEMENT ICI : Obtenir le dossier de configuration du mod pour Forge
        configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME); // Pour Forge
        loadWaypoints();
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("waypoint")
                .then(CommandManager.literal("add")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(WaypointCommands::addWaypoint)))
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(WaypointCommands::removeWaypoint)))
                .then(CommandManager.literal("list")
                    .executes(WaypointCommands::listWaypoints))
                .then(CommandManager.literal("goto")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(WaypointCommands::gotoWaypoint)))
                .then(CommandManager.literal("modify")
                    .then(CommandManager.argument("oldName", StringArgumentType.word())
                        .then(CommandManager.literal("rename")
                            .then(CommandManager.argument("newName", StringArgumentType.word())
                                .executes(WaypointCommands::renameWaypoint))))
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .then(CommandManager.literal("movehere")
                            .executes(WaypointCommands::moveWaypointHere))))
                .then(CommandManager.literal("save")
                    .executes(WaypointCommands::saveWaypointsCommand))
        );
    }

    private static int addWaypoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        BlockPos playerPos = context.getSource().getPlayer().getBlockPos();

        if (waypoints.containsKey(name)) {
            context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + name + "' already exists!"), false);
            return 0;
        }

        ModWaypoint newWaypoint = new ModWaypoint(playerPos, name);
        waypoints.put(name, newWaypoint);
        saveWaypoints();
        context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + name + "' added at " + playerPos.getX() + "," + playerPos.getY() + "," + playerPos.getZ()), false);
        return 1;
    }

    private static int removeWaypoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        if (!waypoints.containsKey(name)) {
            context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + name + "' not found!"), false);
            return 0;
        }

        waypoints.remove(name);
        saveWaypoints();
        context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + name + "' removed."), false);
        return 1;
    }

    private static int listWaypoints(CommandContext<ServerCommandSource> context) {
        if (waypoints.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No waypoints defined yet."), false);
            return 0;
        }

        context.getSource().sendFeedback(() -> Text.literal("--- Waypoints ---"), false);
        waypoints.forEach((name, waypoint) -> {
            // Utiliser getPosition() si tu as modifié ModWaypoint pour stocker x, y, z
            BlockPos pos = (waypoint.position != null) ? waypoint.position : new BlockPos(waypoint.x, waypoint.y, waypoint.z); // Ajustement si ModWaypoint a changé
            context.getSource().sendFeedback(() -> Text.literal("- " + waypoint.name + " @ " + pos.getX() + "," + pos.getY() + "," + pos.getZ()), false);
        });
        return 1;
    }

    private static int gotoWaypoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        if (!waypoints.containsKey(name)) {
            context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + name + "' not found!"), false);
            return 0;
        }

        ModWaypoint targetWaypoint = waypoints.get(name);
        BlockPos playerPos = context.getSource().getPlayer().getBlockPos();

        // Utiliser getPosition() si tu as modifié ModWaypoint pour stocker x, y, z
        BlockPos targetPos = (targetWaypoint.position != null) ? targetWaypoint.position : new BlockPos(targetWaypoint.x, targetWaypoint.y, targetWaypoint.z); // Ajustement

        context.getSource().sendFeedback(() -> Text.literal("Simulating path to waypoint '" + name + "' at " + targetPos + ". (PathFollower integration needed)"), false);
        return 1;
    }

    private static int renameWaypoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");

        if (!waypoints.containsKey(oldName)) {
            context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + oldName + "' not found!"), false);
            return 0;
        }
        if (waypoints.containsKey(newName)) {
            context.getSource().sendFeedback(() -> Text.literal("Waypoint with new name '" + newName + "' already exists!"), false);
            return 0;
        }

        ModWaypoint waypointToRename = waypoints.remove(oldName);
        waypointToRename.name = newName;
        waypoints.put(newName, waypointToRename);
        saveWaypoints();
        context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + oldName + "' renamed to '" + newName + "'."), false);
        return 1;
    }

    private static int moveWaypointHere(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        BlockPos playerPos = context.getSource().getPlayer().getBlockPos();

        if (!waypoints.containsKey(name)) {
            context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + name + "' not found!"), false);
            return 0;
        }

        ModWaypoint waypointToMove = waypoints.get(name);
        // Si tu as modifié ModWaypoint pour stocker x, y, z directement:
        // waypointToMove.x = playerPos.getX();
        // waypointToMove.y = playerPos.getY();
        // waypointToMove.z = playerPos.getZ();
        waypointToMove.position = playerPos; // Sinon, garde ça

        saveWaypoints();
        context.getSource().sendFeedback(() -> Text.literal("Waypoint '" + name + "' moved to your current position: " + playerPos.getX() + "," + playerPos.getY() + "," + playerPos.getZ()), false);
        return 1;
    }

    private static int saveWaypointsCommand(CommandContext<ServerCommandSource> context) {
        saveWaypoints();
        context.getSource().sendFeedback(() -> Text.literal("Waypoints saved to " + configPath.getFileName()), false);
        return 1;
    }

    // --- Méthodes de Persistance ---

    private static void saveWaypoints() {
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(waypoints.values(), writer);
            System.out.println("Waypoints saved to " + configPath);
        } catch (IOException e) {
            System.err.println("Failed to save waypoints: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadWaypoints() {
        if (!configPath.toFile().exists()) {
            System.out.println("Waypoint config file not found, creating new one: " + configPath);
            saveWaypoints();
            return;
        }

        try (FileReader reader = new FileReader(configPath.toFile())) {
            Type listType = new TypeToken<ArrayList<ModWaypoint>>() {}.getType();
            List<ModWaypoint> loadedList = GSON.fromJson(reader, listType);

            waypoints.clear();
            if (loadedList != null) {
                for (ModWaypoint waypoint : loadedList) {
                    waypoints.put(waypoint.name, waypoint);
                }
            }
            System.out.println("Waypoints loaded from " + configPath + ". Total: " + waypoints.size());
        } catch (IOException e) {
            System.err.println("Failed to load waypoints: " + e.getMessage());
            e.printStackTrace();
        }
    }
}