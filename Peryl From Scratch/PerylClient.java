import java.util.ArrayList;
import java.util.List;

public class WaypointManager {
    private List<Waypoint> waypoints = new ArrayList<>();
    private List<WaypointListener> listeners = new ArrayList<>();

    public void addWaypoint(double x, double y, double z) {
        Waypoint waypoint = new Waypoint(x, y, z);
        waypoints.add(waypoint);
        notifyListeners(waypoint, "added");
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
        notifyListeners(waypoint, "removed");
    }

    public void clearWaypoints() {
        waypoints.clear();
        notifyListeners(null, "cleared");
    }

    public void addListener(WaypointListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(Waypoint waypoint, String action) {
        for (WaypointListener listener : listeners) {
            listener.onWaypointUpdated(waypoint, action);
        }
    }
}

interface WaypointListener {
    void onWaypointUpdated(Waypoint waypoint, String action);
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