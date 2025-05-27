package com.samsidere.PerylClient.utils;

import net.minecraft.util.BlockPos;

public class ModWaypoint {
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
        return "ModWaypoint{" +
               "name='" + name + '\'' +
               ", x=" + x +
               ", y=" + y +
               ", z=" + z +
               '}';
    }
}