
public class Vec3 {
    public double x;
    public double y;
    public double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vec3 normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0) { // Avoid division by zero
            return new Vec3(0, 0, 0);
        }
        return new Vec3(x / length, y / length, z / length);
    }

    public Vec3 scale(double scalar) {
        return new Vec3(x * scalar, y * scalar, z * scalar);
    }

    @Override
    public String toString() {
        return "Vec3{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
