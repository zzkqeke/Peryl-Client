package com.samsidere.PerylClient.utils.Jobs; // <--- MODIFIÉ

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

// IMPORTS MODIFIÉS
import com.samsidere.PerylClient.utils.Movements.pathfinder;      // <--- MODIFIÉ (Nom de la classe est 'pathfinder')
import com.samsidere.PerylClient.utils.ModWaypoint;              // <--- MODIFIÉ (Nom de la classe est 'ModWaypoint' et est dans 'utils')
import com.samsidere.PerylClient.utils.Commands.WaypointsCommands; // <--- MODIFIÉ (Nom de la classe est 'WaypointsCommands')

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JobController {

    private final pathfinder pathFollower; // <--- MODIFIÉ (Type)
    private Queue<JobStep> currentJobQueue;
    private JobStep currentStep;
    private Minecraft mc;

    private boolean jobActive = false;
    private long stepStartTime = 0;

    public JobController(pathfinder pathFollower) { // <--- MODIFIÉ (Type)
        this.pathFollower = pathFollower;
        this.mc = Minecraft.getMinecraft();
    }

    public void startJob(List<JobStep> jobSequence) {
        if (jobSequence == null || jobSequence.isEmpty()) {
            System.out.println("JobController: Cannot start empty job.");
            return;
        }
        stopJob();
        this.currentJobQueue = new ConcurrentLinkedQueue<>(jobSequence);
        this.jobActive = true;
        System.out.println("JobController: Job started with " + jobSequence.size() + " steps.");
        advanceToNextStep();
    }

    public void stopJob() {
        if (jobActive) {
            System.out.println("JobController: Job stopped.");
        }
        jobActive = false;
        currentJobQueue = null;
        currentStep = null;
        pathFollower.stop();
        pathFollower.setMiningWhileMoving(false);
    }

    public boolean isJobActive() {
        return jobActive;
    }

    public void onClientTick() {
        if (!jobActive || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (pathFollower.isActive()) {
            return;
        }

        if (currentStep != null) {
            handleCurrentStepAction();
        } else {
            advanceToNextStep();
        }
    }

    private void advanceToNextStep() {
        if (currentJobQueue.isEmpty()) {
            System.out.println("JobController: All steps completed. Job finished.");
            stopJob();
            return;
        }

        currentStep = currentJobQueue.poll();
        stepStartTime = System.currentTimeMillis();
        System.out.println("JobController: Advancing to next step: " + currentStep.action + " at " +
                           (currentStep.targetPos != null ? currentStep.targetPos : currentStep.waypointName));

        BlockPos actualTargetPos = null;

        if (currentStep.waypointName != null && !currentStep.waypointName.isEmpty()) {
            ModWaypoint wp = WaypointsCommands.getWaypoint(currentStep.waypointName); // <--- MODIFIÉ (Nom de la classe)
            if (wp != null) {
                actualTargetPos = wp.getBlockPos();
                System.out.println("JobController: Resolved waypoint '" + currentStep.waypointName + "' to " + actualTargetPos);
            } else {
                System.err.println("JobController: Waypoint '" + currentStep.waypointName + "' not found. Skipping step.");
                advanceToNextStep();
                return;
            }
        } else if (currentStep.targetPos != null) {
            actualTargetPos = currentStep.targetPos;
        }

        pathFollower.setMiningWhileMoving(currentStep.action == JobAction.MINE_WHILE_MOVING);

        if (actualTargetPos != null) {
            if (!mc.thePlayer.getPosition().equals(actualTargetPos) || currentStep.action == JobAction.MINE_WHILE_MOVING) {
                List<BlockPos> simulatedPath = List.of(mc.thePlayer.getPosition(), actualTargetPos);
                pathFollower.startPath(simulatedPath);
            } else {
                System.out.println("JobController: Already at target position " + actualTargetPos + ". Executing action directly.");
            }
        } else {
            System.out.println("JobController: Step without target position or waypoint. Executing action directly.");
        }
    }

    private void handleCurrentStepAction() {
        EntityPlayerSP player = mc.thePlayer;
        World world = mc.theWorld;

        if (player == null || world == null) return;

        if (currentStep.action == JobAction.WAIT) {
            long elapsedTime = System.currentTimeMillis() - stepStartTime;
            if (elapsedTime < currentStep.waitDurationMs) {
                return;
            } else {
                System.out.println("JobController: Wait action completed after " + currentStep.waitDurationMs + "ms.");
            }
        }

        switch (currentStep.action) {
            case NONE:
            case MINE_WHILE_MOVING:
                System.out.println("JobController: Action " + currentStep.action + " completed (or handled by pathfinder).");
                break;

            case MINE_BLOCK:
                BlockPos minePos = currentStep.targetPos;
                if (minePos == null && currentStep.waypointName != null) {
                    ModWaypoint wp = WaypointsCommands.getWaypoint(currentStep.waypointName); // <--- MODIFIÉ
                    if (wp != null) minePos = wp.getBlockPos();
                }

                if (minePos != null && world.getBlockState(minePos).getBlock() != Blocks.air) {
                    System.out.println("JobController: Simulating mining at " + minePos);
                    mc.playerController.onPlayerDamageBlock(minePos, Minecraft.getMinecraft().thePlayer.getHorizontalFacing());
                    mc.playerController.onPlayerDestroyBlock(minePos, Minecraft.getMinecraft().thePlayer.getHorizontalFacing());
                } else {
                    System.out.println("JobController: No block to mine at " + minePos + ". Skipping.");
                }
                break;

            case USE_ITEM:
                BlockPos usePos = currentStep.targetPos;
                if (usePos == null && currentStep.waypointName != null) {
                    ModWaypoint wp = WaypointsCommands.getWaypoint(currentStep.waypointName); // <--- MODIFIÉ
                    if (wp != null) usePos = wp.getBlockPos();
                }

                System.out.println("JobController: Simulating item use.");
                if (usePos != null) {
                    mc.playerController.onPlayerRightClick(player, world, player.getHeldItem(), usePos, player.getHorizontalFacing(), player.getLook(1.0f));
                    System.out.println("JobController: Simulating right-click at " + usePos);
                } else {
                    mc.playerController.sendUseItem(player, world, player.getHeldItem());
                    System.out.println("JobController: Simulating right-click (air).");
                }
                break;

            case CHAT_MESSAGE:
                if (currentStep.actionParameter != null && !currentStep.actionParameter.isEmpty()) {
                    player.sendChatMessage(currentStep.actionParameter);
                    System.out.println("JobController: Sent chat message: " + currentStep.actionParameter);
                }
                break;

            case CHECK_INVENTORY_FULL:
                if (player.inventory.mainInventory != null) {
                    boolean inventoryFull = true;
                    for (ItemStack stack : player.inventory.mainInventory) {
                        if (stack == null) {
                            inventoryFull = false;
                            break;
                        }
                    }
                    if (inventoryFull) {
                        System.out.println("JobController: Inventory is full! Stopping current job and activating alternative job (if defined).");
                        stopJob();
                    } else {
                        System.out.println("JobController: Inventory is not full. Continuing job.");
                    }
                }
                break;

            default:
                System.out.println("JobController: Unknown action " + currentStep.action + ". Skipping step.");
                break;
        }

        if (currentStep.action != JobAction.WAIT || (System.currentTimeMillis() - stepStartTime) >= currentStep.waitDurationMs) {
            currentStep = null;
        }
    }
}