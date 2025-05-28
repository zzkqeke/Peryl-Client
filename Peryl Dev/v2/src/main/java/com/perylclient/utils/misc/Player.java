import com.perylclient.utils.misc.Vec3;

public class Player {
    private Vec3 position;
    private Vec3 deltaMovement; // Also known as velocity

    public Player(double x, double y, double z) {
        this.position = new Vec3(x, y, z);
        this.deltaMovement = new Vec3(0, 0, 0);
    }

    public Vec3 getPositionVec() {
        return this.position;
    }

    public void setPositionVec(Vec3 position) { // Good for testing/initial setup
        this.position = position;
    }

    public void setDeltaMovement(Vec3 deltaMovement) {
        this.deltaMovement = deltaMovement;
        System.out.println("Player deltaMovement set to: " + deltaMovement);
    }

    public Vec3 getDeltaMovement() {
        return this.deltaMovement;
    }

    // Example: A method to actually apply the movement in a game loop
    public void updatePositionFromDelta() {
        this.position = new Vec3(
            this.position.x + this.deltaMovement.x,
            this.position.y + this.deltaMovement.y,
            this.position.z + this.deltaMovement.z
        );
         System.out.println("Player new position: " + this.position);
    }
}