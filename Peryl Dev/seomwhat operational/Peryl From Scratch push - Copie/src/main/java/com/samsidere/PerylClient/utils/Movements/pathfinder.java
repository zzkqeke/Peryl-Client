package com.samsidere.PerylClient.utils.Movements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Arrays;

public class pathfinder {

    private Minecraft mc = Minecraft.getMinecraft();
    private Queue<BlockPos> currentPath;
    private BlockPos nextWaypoint;
    private boolean active;

    private final float lookSpeedFactor = 0.15f;
    private final double reachDistanceXZ = 0.5;
    private final double reachDistanceY = 1.0;

    private long ticksStuck = 0;
    private final long maxTicksStuck = 60;
    private BlockPos lastPosition = null;
    private final double minMovementThreshold = 0.05;

    private long recoveryTicks = 0;
    private RecoveryAction recoveryAction = RecoveryAction.NONE;
    private int recoveryAttempts = 0;
    private final int maxRecoveryAttempts = 3;

    private Random random = new Random();
    private long nextFidgetTick = 0;
    private final int fidgetMinDelay = 40;
    private final int fidgetMaxDelay = 100;

    private boolean miningWhileMoving = false;
    private BlockPos blockBeingMined = null;
    private float curBlockDamage = 0f;
    private int blockMineDelay = 0;

    private enum RecoveryAction {
        NONE, JUMP, STRAFE_LEFT, STRAFE_RIGHT
    }

    public pathfinder() {
        this.currentPath = new ConcurrentLinkedQueue<>();
        this.active = false;
    }

    public void startPath(List<BlockPos> path) {
        if (path == null || path.isEmpty()) {
            System.out.println("Pathfinder: Path is empty or null. Not starting.");
            stop();
            return;
        }
        this.currentPath = new ConcurrentLinkedQueue<>(path);
        this.active = true;
        this.ticksStuck = 0;
        this.lastPosition = null;
        this.recoveryAttempts = 0;
        this.nextWaypoint = null;
        System.out.println("Pathfinder: Path started with " + path.size() + " waypoints.");
        advanceToNextWaypoint();
    }

    public void stop() {
        if (active) {
            System.out.println("Pathfinder: Path stopped.");
        }
        active = false;
        currentPath.clear();
        nextWaypoint = null;
        releaseAllKeys();
        miningWhileMoving = false;
        stopMining();
    }

    public boolean isActive() {
        return active;
    }

    public void setMiningWhileMoving(boolean mining) {
        this.miningWhileMoving = mining;
        if (!mining) {
            stopMining();
        }
    }

    private void releaseAllKeys() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
    }

    private void stopMining() {
        if (blockBeingMined != null) {
            mc.playerController.resetBlockRemoving();
            blockBeingMined = null;
            curBlockDamage = 0f;
            blockMineDelay = 0;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        }
    }

    private void advanceToNextWaypoint() {
        if (currentPath.isEmpty()) {
            System.out.println("Pathfinder: Reached end of path.");
            stop();
            return;
        }
        nextWaypoint = currentPath.poll();
        System.out.println("Pathfinder: Moving to next waypoint: " + nextWaypoint.getX() + ", " + nextWaypoint.getY() + ", " + nextWaypoint.getZ());
        ticksStuck = 0;
        recoveryAction = RecoveryAction.NONE;
        recoveryAttempts = 0;
        lastPosition = null;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && active && mc.thePlayer != null && mc.theWorld != null) {
            EntityPlayerSP player = mc.thePlayer;
            PlayerControllerMP controller = mc.playerController;

            if (nextWaypoint == null) {
                advanceToNextWaypoint();
                if (!active) return;
            }

            if (lastPosition != null && player.getDistanceSq(lastPosition.getX() + 0.5, lastPosition.getY(), lastPosition.getZ() + 0.5) < minMovementThreshold * minMovementThreshold) {
                ticksStuck++;
                if (ticksStuck > maxTicksStuck && recoveryAction == RecoveryAction.NONE) {
                    System.out.println("Pathfinder: Stuck detected! Attempting recovery.");
                    recoveryAttempts++;
                    if (recoveryAttempts > maxRecoveryAttempts) {
                        System.err.println("Pathfinder: Failed to recover after " + maxRecoveryAttempts + " attempts. Stopping path.");
                        stop();
                        return;
                    }
                    int actionChoice = random.nextInt(3);
                    if (actionChoice == 0) recoveryAction = RecoveryAction.JUMP;
                    else if (actionChoice == 1) recoveryAction = RecoveryAction.STRAFE_LEFT;
                    else recoveryAction = RecoveryAction.STRAFE_RIGHT;
                    recoveryTicks = 0;
                }
            } else {
                ticksStuck = 0;
                if (recoveryAction != RecoveryAction.NONE && lastPosition != null && player.getDistanceSq(lastPosition.getX() + 0.5, lastPosition.getY(), lastPosition.getZ() + 0.5) >= minMovementThreshold * minMovementThreshold * 2) {
                    recoveryAction = RecoveryAction.NONE;
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                    System.out.println("Pathfinder: Recovery successful, resuming normal path.");
                }
            }
            lastPosition = new BlockPos(player.posX, player.posY, player.posZ);

            if (recoveryAction != RecoveryAction.NONE) {
                recoveryTicks++;
                if (recoveryTicks < 20) {
                    switch (recoveryAction) {
                        case JUMP:
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                            break;
                        case STRAFE_LEFT:
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
                            break;
                        case STRAFE_RIGHT:
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
                            break;
                    }
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                    return;
                } else {
                    recoveryAction = RecoveryAction.NONE;
                    recoveryTicks = 0;
                    releaseAllKeys();
                    System.out.println("Pathfinder: Recovery attempt finished.");
                }
            }

            double distSq = player.getDistanceSq(nextWaypoint.getX() + 0.5, nextWaypoint.getY(), nextWaypoint.getZ() + 0.5);
            if (distSq < reachDistanceXZ * reachDistanceXZ && Math.abs(player.posY - nextWaypoint.getY()) < reachDistanceY) {
                advanceToNextWaypoint();
                if (!active) return;
            }

            lookAt(nextWaypoint.getX() + 0.5, nextWaypoint.getY() + 0.5, nextWaypoint.getZ() + 0.5);

            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);

            BlockPos playerFeet = new BlockPos(player.posX, player.posY - 0.05, player.posZ);
            BlockPos blockAhead = playerFeet.offset(player.getHorizontalFacing());
            BlockPos blockAheadUp = blockAhead.up();

            if (mc.theWorld.getBlockState(blockAhead).getBlock() != Blocks.air && mc.theWorld.getBlockState(blockAheadUp).getBlock() == Blocks.air) {
                if (player.onGround && random.nextInt(10) == 0) {
                     KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                } else {
                     KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                }
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            }

            if (miningWhileMoving) {
                if (blockMineDelay > 0) {
                    blockMineDelay--;
                } else {
                    BlockPos blockToMine = null;
                    EnumFacing facing = player.getHorizontalFacing();

                    for (int i = 1; i <= 3; i++) {
                        BlockPos currentCheckPos = new BlockPos(player.posX, player.posY, player.posZ).offset(facing, i);
                        if (mc.theWorld.getBlockState(currentCheckPos).getBlock() != Blocks.air && mc.theWorld.getBlockState(currentCheckPos).getBlock().getMaterial().isSolid()) {
                            blockToMine = currentCheckPos;
                            break;
                        }
                        if (mc.theWorld.getBlockState(currentCheckPos.up()).getBlock() != Blocks.air && mc.theWorld.getBlockState(currentCheckPos.up()).getBlock().getMaterial().isSolid()) {
                            blockToMine = currentCheckPos.up();
                            break;
                        }
                    }

                    if (blockToMine == null) {
                        BlockPos belowPlayer = new BlockPos(player.posX, player.posY - 1, player.posZ);
                        Block blockBelow = mc.theWorld.getBlockState(belowPlayer).getBlock();
                        if (blockBelow != Blocks.air && blockBelow.getBlockHardness(mc.theWorld, belowPlayer) >= 0 && blockBelow.getMaterial().isSolid() &&
                            (blockBelow == Blocks.sand || blockBelow == Blocks.gravel || blockBelow == Blocks.dirt || blockBelow == Blocks.grass)) {
                            blockToMine = belowPlayer;
                        }
                    }

                    if (blockToMine != null && mc.theWorld.getBlockState(blockToMine).getBlock() != Blocks.air) {
                        Block targetBlock = mc.theWorld.getBlockState(blockToMine).getBlock();
                        if (targetBlock.getBlockHardness(mc.theWorld, blockToMine) >= 0 &&
                            !(targetBlock == Blocks.bedrock || targetBlock == Blocks.command_block || targetBlock == Blocks.end_portal_frame || targetBlock == Blocks.mob_spawner)) {

                            ItemStack heldItem = player.getHeldItem();
                            boolean hasCorrectTool = true;
                            if (heldItem != null) {
                                Item item = heldItem.getItem();
                                if (targetBlock == Blocks.stone || targetBlock == Blocks.cobblestone || targetBlock.getMaterial() == net.minecraft.block.material.Material.rock) {
                                    if (!(item instanceof ItemPickaxe)) {
                                        hasCorrectTool = false;
                                    }
                                } else if (targetBlock.getMaterial() == net.minecraft.block.material.Material.wood) {
                                    if (!(item instanceof ItemAxe)) {
                                        hasCorrectTool = false;
                                    }
                                }
                            }

                            if (hasCorrectTool) {
                                if (blockBeingMined == null || !blockBeingMined.equals(blockToMine)) {
                                    stopMining();
                                    blockBeingMined = blockToMine;
                                    curBlockDamage = 0f;
                                    mc.playerController.onPlayerDamageBlock(blockToMine, EnumFacing.DOWN);
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
                                    System.out.println("Pathfinder: Started mining block at " + blockToMine);
                                }

                                curBlockDamage += targetBlock.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, blockBeingMined);
                                mc.theWorld.sendBlockBreakProgress(player.getEntityId(), blockBeingMined, (int) (curBlockDamage * 10.0F) - 1);

                                if (curBlockDamage >= 1.0f) {
                                    mc.playerController.onPlayerDestroyBlock(blockBeingMined, EnumFacing.DOWN);
                                    System.out.println("Pathfinder: Broke block at " + blockBeingMined);
                                    stopMining();
                                    blockMineDelay = 5;
                                }
                            } else {
                                stopMining();
                                player.addChatMessage(new ChatComponentText("§cPathfinder: Missing correct tool to mine " + targetBlock.getLocalizedName()));
                            }
                        } else {
                            stopMining();
                        }
                    } else {
                        stopMining();
                    }
                }
            }

            if (System.currentTimeMillis() > nextFidgetTick) {
                player.rotationYaw += random.nextFloat() * 2.0F - 1.0F;
                player.rotationPitch += random.nextFloat() * 2.0F - 1.0F;
                player.rotationPitch = MathHelper.clamp_float(player.rotationPitch, -90.0F, 90.0F);
                nextFidgetTick = System.currentTimeMillis() + fidgetMinDelay + random.nextInt(fidgetMaxDelay - fidgetMinDelay);
            }
        }
    }

    private void lookAt(double x, double y, double z) {
        EntityPlayerSP player = mc.thePlayer;
        double dx = x - player.posX;
        double dy = y - (player.posY + player.getEyeHeight());
        double dz = z - player.posZ;

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, distXZ) * 180.0D / Math.PI);

        player.rotationYaw = interpolateRotation(player.rotationYaw, yaw);
        player.rotationPitch = interpolateRotation(player.rotationPitch, pitch);
    }

    private float interpolateRotation(float currentRotation, float targetRotation) {
        float diff = MathHelper.wrapAngleTo180_float(targetRotation - currentRotation);
        if (diff > lookSpeedFactor) {
            diff = lookSpeedFactor;
        }
        if (diff < -lookSpeedFactor) {
            diff = -lookSpeedFactor;
        }
        return currentRotation + diff;
    }
}