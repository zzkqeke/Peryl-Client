import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class HumanizedPathfinder {
    private double currentX, currentY, currentZ;
    private double targetX, targetY, targetZ;
    private double speed = 0.05; // Human-like movement speed
    private Random random = new Random();

    public HumanizedPathfinder(double startX, double startY, double startZ) {
        this.currentX = startX;
        this.currentY = startY;
        this.currentZ = startZ;
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public void updateMovement() {
        // Simulate human-like variation in movement speed
        double adjustedSpeed = speed + (random.nextDouble() * 0.02 - 0.01);

        // Move towards target with slight randomness
        currentX += (targetX - currentX) * adjustedSpeed + (random.nextDouble() * 0.005 - 0.0025);
        currentY += (targetY - currentY) * adjustedSpeed + (random.nextDouble() * 0.005 - 0.0025);
        currentZ += (targetZ - currentZ) * adjustedSpeed + (random.nextDouble() * 0.005 - 0.0025);

        // Simulate occasional jumps for obstacle handling
        if (random.nextInt(100) < 5) {
            currentY += 0.2;
        }
    }

    public boolean hasReachedTarget() {
        return Math.abs(currentX - targetX) < 0.1 &&
               Math.abs(currentY - targetY) < 0.1 &&
               Math.abs(currentZ - targetZ) < 0.1;
    }

    public String getCurrentPosition() {
        return "Current Position: (" + currentX + ", " + currentY + ", " + currentZ + ")";
    }
}

public class WaypointNavigation {
    private List<Waypoint> waypoints = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private HumanizedPathfinder pathfinder;

    public WaypointNavigation(double startX, double startY, double startZ) {
        this.pathfinder = new HumanizedPathfinder(startX, startY, startZ);
    }

    public void addWaypoint(double x, double y, double z) {
        waypoints.add(new Waypoint(x, y, z));
    }

    public void startPathfinding() {
        if (!waypoints.isEmpty()) {
            setNextWaypoint();
        }
    }

    private void setNextWaypoint() {
        if (currentWaypointIndex < waypoints.size()) {
            Waypoint nextWaypoint = waypoints.get(currentWaypointIndex);
            pathfinder.setTarget(nextWaypoint.x, nextWaypoint.y, nextWaypoint.z);
        }
    }

    public void updateMovement() {
        if (waypoints.isEmpty()) return;

        pathfinder.updateMovement();

        // Check if the pathfinder reached the current waypoint
        if (pathfinder.hasReachedTarget()) {
            currentWaypointIndex++;

            // Move to the next waypoint if available
            if (currentWaypointIndex < waypoints.size()) {
                setNextWaypoint();
            } else {
                System.out.println("Pathfinding complete!");
            }
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
}