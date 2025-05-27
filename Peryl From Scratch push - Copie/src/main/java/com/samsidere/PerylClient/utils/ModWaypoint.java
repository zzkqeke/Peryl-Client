package main.java.com.samsidere.PerylClient.utils; // <--- MODIFIÉ

import net.minecraft.util.BlockPos;

// Si le fichier s'appelle ModWaypoint.java
public class ModWaypoint { // <--- Assurez-vous que le nom de la classe est ModWaypoint
    public int x, y, z;
    public String name;

    public ModWaypoint() {}

    public ModWaypoint(BlockPos pos, String name) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.name = name;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public String toString() {
        return "Waypoint{" +
               "name='" + name + '\'' +
               ", x=" + x +
               ", y=" + y +
               ", z=" + z +
               '}';
    }
}