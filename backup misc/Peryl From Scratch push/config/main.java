import java.io.*;
import java.util.*;
import org.lwjgl.input.Keyboard;

public class PerylClient {
    private static final String CONFIG_PATH = "config/PerylClient.json";
    private List<Waypoint> waypoints = new ArrayList<>();
    private boolean pathfindingEnabled = false;
    private boolean guiVisible = true;
    private boolean debugLogging = false;

    public PerylClient() {
        loadConfig();
    }

    public void togglePathfinding() {
        pathfindingEnabled = !pathfindingEnabled;
        saveConfig();
        System.out.println("Pathfinding " + (pathfindingEnabled ? "Enabled" : "Disabled"));
    }

    public void toggleGUI() {
        guiVisible = !guiVisible;
        saveConfig();
        System.out.println("GUI " + (guiVisible ? "Visible" : "Hidden"));
    }

    public void toggleDebugLogging() {
        debugLogging = !debugLogging;
        saveConfig();
        System.out.println("Debug Logging " + (debugLogging ? "Enabled" : "Disabled"));
    }

    public void addWaypoint(double x, double y, double z) {
        waypoints.add(new Waypoint(x, y, z));
        saveConfig();
        System.out.println("Added Waypoint: " + x + ", " + y + ", " + z);
    }

    public void clearWaypoints() {
        waypoints.clear();
        saveConfig();
        System.out.println("Cleared all waypoints.");
    }

    private void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Loaded: " + line);
            }
        } catch (IOException e) {
            System.out.println("Failed to load config.");
        }
    }

    private void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_PATH))) {
            writer.write("Pathfinding: " + pathfindingEnabled + "\n");
            writer.write("GUI Visible: " + guiVisible + "\n");
            writer.write("Debug Logging: " + debugLogging + "\n");
            writer.write("Waypoints: " + waypoints.size() + "\n");
        } catch (IOException e) {
            System.out.println("Failed to save config.");
        }
    }

    public void handleKeyPress(int key) {
        if (key == Keyboard.KEY_P) togglePathfinding();
        if (key == Keyboard.KEY_G) toggleGUI();
        if (key == Keyboard.KEY_D) toggleDebugLogging();
    }
}

class Waypoint {
    double x, y, z;

    public Waypoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}