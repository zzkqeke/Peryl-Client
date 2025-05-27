package com.samsidere.PerylClient.utils.Jobs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import com.samsidere.PerylClient.utils.Movements.pathfinder;
import com.samsidere.PerylClient.utils.ModWaypoint;
import com.samsidere.PerylClient.utils.Commands.WaypointsCommands;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Arrays;

public class JobController {

    private Minecraft mc = Minecraft.getMinecraft();
    private pathfinder pathFollower;
    private Queue<JobStep> currentJobQueue;
    private JobStep currentStep;
    private boolean active;

    private long stepStartTime;

    public JobController(pathfinder pathFollower) {
        this.pathFollower = pathFollower;
        this.currentJobQueue = new ConcurrentLinkedQueue<>();
        this.active = false;
    }

    public void startJob(List<JobStep> jobSteps) {
        if (active) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§cAnother job is already active. Stop it first."));
            return;
        }
        if (jobSteps == null || jobSteps.isEmpty()) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§cJob steps are empty or null."));
            return;
        }

        this.currentJobQueue = new ConcurrentLinkedQueue<>(jobSteps);
        this.active = true;
        mc.thePlayer.addChatMessage(new ChatComponentText("§aJob started with " + jobSteps.size() + " steps."));
        advanceToNextStep();
    }

    public void stopJob() {
        if (active) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§aJob stopped."));
            pathFollower.stop();
        }
        active = false;
        currentJobQueue.clear();
        currentStep = null;
    }

    public boolean isActive() {
        return active;
    }

    private void advanceToNextStep() {
        if (currentJobQueue.isEmpty()) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§aJob completed successfully!"));
            stopJob();
            return;
        }
        currentStep = currentJobQueue.poll();
        stepStartTime = System.currentTimeMillis();
        System.out.println("JobController: Advancing to next step: " + currentStep.action + (currentStep.waypointName != null ? " (" + currentStep.waypointName + ")" : ""));

        executeStep();
    }

    private void executeStep() {
        if (currentStep == null) {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            System.err.println("JobController: Player or World is null. Stopping job.");
            stopJob();
            return;
        }

        switch (currentStep.action) {
            case MOVE_TO_WAYPOINT:
                if (currentStep.waypointName != null) {
                    ModWaypoint waypoint = WaypointsCommands.getWaypoint(currentStep.waypointName);
                    if (waypoint != null) {
                        pathFollower.startPath(Arrays.asList(waypoint.getBlockPos()));
                    } else {
                        player.addChatMessage(new ChatComponentText("§cWaypoint '" + currentStep.waypointName + "' not found for job step."));
                        stopJob();
                    }
                } else if (currentStep.targetPos != null) {
                    pathFollower.startPath(Arrays.asList(currentStep.targetPos));
                } else {
                    player.addChatMessage(new ChatComponentText("§cMOVE_TO_WAYPOINT step requires a waypoint name or target position."));
                    stopJob();
                }
                break;
            case MINE_BLOCK:
                if (currentStep.blockToMine != null) {
                    player.addChatMessage(new ChatComponentText("§eMining specific block at " + currentStep.blockToMine + " (dummy)."));
                } else {
                    player.addChatMessage(new ChatComponentText("§cMINE_BLOCK step requires a target block position."));
                }
                advanceToNextStep();
                break;
            case MINE_WHILE_MOVING:
                pathFollower.setMiningWhileMoving(true);
                break;
            case USE_ITEM:
                if (currentStep.targetPos != null) {
                    mc.playerController.onPlayerRightClick(
                        player,
                        mc.theWorld,
                        player.getHeldItem(),
                        currentStep.targetPos,
                        EnumFacing.DOWN,
                        new Vec3(0.5D, 0.5D, 0.5D)
                    );
                    player.addChatMessage(new ChatComponentText("§aUsed item on block at " + currentStep.targetPos.getX() + "," + currentStep.targetPos.getY() + "," + currentStep.targetPos.getZ()));
                } else if (currentStep.customData != null && currentStep.customData.equals("chest_interaction")) {
                    player.addChatMessage(new ChatComponentText("§eInteracting with chest (dummy, need targetPos for real interaction)."));
                }
                advanceToNextStep();
                break;
            case WAIT:
                player.addChatMessage(new ChatComponentText("§aWaiting for " + currentStep.waitTicks + " ticks."));
                break;
            case DUMP_INVENTORY:
                player.addChatMessage(new ChatComponentText("§eDumping inventory (dummy, logic to be implemented)."));
                advanceToNextStep();
                break;
            default:
                player.addChatMessage(new ChatComponentText("§cUnknown job action: " + currentStep.action));
                stopJob();
                break;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && active && mc.thePlayer != null) {
            if (currentStep == null) {
                advanceToNextStep();
                return;
            }

            switch (currentStep.action) {
                case MOVE_TO_WAYPOINT:
                    if (!pathFollower.isActive()) {
                        System.out.println("JobController: Pathfinder finished for MOVE_TO_WAYPOINT step.");
                        advanceToNextStep();
                    }
                    break;
                case MINE_WHILE_MOVING:
                    if (!pathFollower.isActive()) {
                        System.out.println("JobController: Pathfinder (mining) finished for MINE_WHILE_MOVING step.");
                        pathFollower.setMiningWhileMoving(false);
                        advanceToNextStep();
                    }
                    break;
                case WAIT:
                    long elapsedTicks = (System.currentTimeMillis() - stepStartTime) / 50;
                    if (elapsedTicks >= currentStep.waitTicks) {
                        System.out.println("JobController: Wait time over. Advancing.");
                        advanceToNextStep();
                    }
                    break;
            }
        }
    }
}