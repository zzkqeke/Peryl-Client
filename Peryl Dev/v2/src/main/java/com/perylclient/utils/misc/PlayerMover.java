import utils.misc.*;

public class PlayerMover {

    private double movementSpeed;

    /**
     * Default constructor, uses a default movement speed of 0.2.
     */
    public PlayerMover() {
        this.movementSpeed = 0.2; // Default speed from your example
    }

    /**
     * Constructor to set a custom movement speed.
     * @param movementSpeed The speed at which the player moves (scale factor).
     */
    public PlayerMover(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    /**
     * Calculates and sets the player's delta movement towards a target coordinate.
     * The target coordinates (nextX, nextZ) are assumed to be block centers,
     * hence the +0.5.
     *
     * @param player The player entity to move.
     * @param nextX  The target X-coordinate (e.g., center of a block).
     * @param y      The target Y-coordinate (player's current Y is often used if not changing elevation).
     * @param nextZ  The target Z-coordinate (e.g., center of a block).
     */
    public void movePlayerToTarget(Player player, double nextX, double y, double nextZ) {
        if (player == null) {
            System.err.println("PlayerMover: Player object is null. Cannot move.");
            return;
        }

        // Target is the center of the next block
        Vec3 target = new Vec3(nextX + 0.5, y, nextZ + 0.5);

        // Direction vector from player to target
        Vec3 playerPosition = player.getPositionVec();
        if (playerPosition == null) {
            System.err.println("PlayerMover: Player position is null. Cannot calculate direction.");
            return;
        }
        Vec3 direction = target.subtract(playerPosition);

        // Normalize to get a unit vector, then scale by speed
        // If the player is already at the target, direction might be (0,0,0)
        // Normalizing a zero vector can lead to NaN or errors depending on Vec3 implementation.
        // The provided Vec3.normalize() handles division by zero.
        Vec3 movementVector = direction.normalize().scale(this.movementSpeed);

        player.setDeltaMovement(movementVector);
    }

    // Optional: Getter and setter for movementSpeed if you want to change it after instantiation
    public double getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }
}