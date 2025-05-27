package com.samsidere.PerylClient.utils.Jobs;

import net.minecraft.util.BlockPos;

public class JobStep {
    public BlockPos targetPos;
    public String waypointName;
    public JobAction action;
    public long waitTicks;
    public String customData;
    public BlockPos blockToMine;

    public JobStep(BlockPos targetPos, String waypointName, JobAction action, long waitTicks, String customData, BlockPos blockToMine) {
        this.targetPos = targetPos;
        this.waypointName = waypointName;
        this.action = action;
        this.waitTicks = waitTicks;
        this.customData = customData;
        this.blockToMine = blockToMine;
    }

    public JobStep(JobAction action) {
        this(null, null, action, 0, null, null);
    }

    public JobStep(String waypointName, JobAction action) {
        this(null, waypointName, action, 0, null, null);
    }

    public JobStep(BlockPos targetPos, JobAction action) {
        this(targetPos, null, action, 0, null, null);
    }

    public JobStep(JobAction action, long waitTicks) {
        this(null, null, action, waitTicks, null, null);
    }

    public JobStep(JobAction action, String customData) {
        this(null, null, action, 0, customData, null);
    }

    public JobStep(JobAction action, BlockPos blockToMine) {
        this(null, null, action, 0, null, blockToMine);
    }

    public JobStep(String waypointName, JobAction action, String customData) {
        this(null, waypointName, action, 0, customData, null);
    }

    public JobStep(BlockPos targetPos, JobAction action, String customData) {
        this(targetPos, null, action, 0, customData, null);
    }
}