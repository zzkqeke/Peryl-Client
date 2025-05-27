package com.samsidere.PerylClient.utils.Jobs; // <--- MODIFIÉ

import net.minecraft.util.BlockPos;

public class JobStep {
    public BlockPos targetPos;
    public String waypointName;
    public JobAction action;
    public String actionParameter;
    public long waitDurationMs;

    public JobStep(BlockPos targetPos, JobAction action) {
        this(targetPos, null, action, null, 0);
    }

    public JobStep(String waypointName, JobAction action) {
        this(null, waypointName, action, null, 0);
    }

    public JobStep(BlockPos targetPos, JobAction action, String actionParameter) {
        this(targetPos, null, action, actionParameter, 0);
    }

    public JobStep(String waypointName, JobAction action, String actionParameter) {
        this(null, waypointName, action, actionParameter, 0);
    }

    public JobStep(BlockPos targetPos, JobAction action, long waitDurationMs) {
        this(targetPos, null, action, null, waitDurationMs);
    }

    public JobStep(String waypointName, JobAction action, long waitDurationMs) {
        this(null, waypointName, action, null, waitDurationMs);
    }

    private JobStep(BlockPos targetPos, String waypointName, JobAction action, String actionParameter, long waitDurationMs) {
        this.targetPos = targetPos;
        this.waypointName = waypointName;
        this.action = action;
        this.actionParameter = actionParameter;
        this.waitDurationMs = waitDurationMs;
    }

    @Override
    public String toString() {
        String posInfo = (targetPos != null) ?
            "targetPos=" + targetPos.getX() + "," + targetPos.getY() + "," + targetPos.getZ() :
            (waypointName != null ? "waypointName='" + waypointName + "'" : "no_position");

        return "JobStep{" +
               posInfo +
               ", action=" + action +
               (actionParameter != null ? ", param='" + actionParameter + "'" : "") +
               (waitDurationMs > 0 ? ", wait=" + waitDurationMs + "ms" : "") +
               '}';
    }
}