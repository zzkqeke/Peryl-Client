import java.util.ArrayList;
import java.util.List;

public class WaypointCommandSystem {
    private List<Waypoint> waypoints = new ArrayList<>();

    public void handleCommand(String command, double x, double y, double z) {
        switch (command.toLowerCase()) {
            case "/waypointadd":
                addWaypoint(x, y, z);
                break;
            case "/waypointremove":
                removeWaypoint(x, y, z);
                break;
            case "/waypointclear":
                clearWaypoints();
                break;
            case "/waypointlist":
                listWaypoints();
                break;
            default:
                System.out.println("Invalid command. Use: /waypointadd, /waypointremove, /waypointclear, /waypointlist.");
        }
    }

    private void addWaypoint(double x, double y, double z) {
        waypoints.add(new Waypoint(x, y, z));
        System.out.println("Waypoint added: " + x + ", " + y + ", " + z);
    }

    private void removeWaypoint(double x, double y, double z) {
        waypoints.removeIf(wp -> wp.x == x && wp.y == y && wp.z == z);
        System.out.println("Waypoint removed: " + x + ", " + y + ", " + z);
    }

    private void clearWaypoints() {
        waypoints.clear();
        System.out.println("All waypoints cleared.");
    }

    private void listWaypoints() {
        System.out.println("Current Waypoints:");
        for (Waypoint wp : waypoints) {
            System.out.println(wp);
        }
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
        return "(" + x + ", " + y + ", " + z + ")";
    }
}