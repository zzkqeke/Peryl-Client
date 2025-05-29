package com.perylclient; // 

import net.minecraft.world.phys.Vec3;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import net.minecraftforge.client.event.InputEvent; // Ensure this import is present
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.InputConstants;

// Changed class name and MODID
@Mod(PerylClientMod.MODID) //  class name reference
public class PerylClientMod { //  class name
    public static final String MODID = "perylclient"; //  MODID string
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean isMiningActive = false;
    private int tickCounter = 0;
    private static final int ACTION_COOLDOWN_TICKS = 10;
    private static final float MINING_SPEED_VARIATION = 0.15f;
    private static final float HEAD_MOVEMENT_SPEED = 0.12f;
    private static final float MOVEMENT_SPEED = 0.18f;
    private static final float MINING_PROGRESS_THRESHOLD = 85f;
    private static final int MAX_LOOK_AHEAD = 5;
    private static final float MINING_ANGLE_THRESHOLD = 2f;
    private static final float MOVEMENT_VARIATION = 0.02f;
    private static final float HEAD_VARIATION = 0.1f;

    private BlockPos lastMinedPos = null;
    private float targetYaw = 0f;
    private float targetPitch = 0f;
    private float currentMiningProgress = 0f;
    private boolean isLookingAtTarget = false;
    private Direction currentMiningDirection = null;
    private int failedMiningAttempts = 0;
    private static final int MAX_FAILED_ATTEMPTS = 3;

    public static KeyMapping toggleMiningKey;

    public PerylClientMod() { //  constructor name
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Peryl Client Setup"); // log message
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            toggleMiningKey = new KeyMapping(
                    "key.perylclient.toggleminer", //  key name
                    GLFW.GLFW_KEY_K,
                    "key.categories.perylclient" //  category name
            );
            event.register(toggleMiningKey);
            LOGGER.info("Registered keybinds for Peryl Client"); // log message
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        try {
            if (toggleMiningKey != null
                    && toggleMiningKey.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))
                    && event.getAction() == GLFW.GLFW_PRESS) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    isMiningActive = !isMiningActive;
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: " + (isMiningActive ? "ON" : "OFF")),
                            true
                    );
                    if (!isMiningActive) {
                        lastMinedPos = null;
                        currentMiningDirection = null;
                        isLookingAtTarget = false;
                    }
                    LOGGER.info("Peryl Client AutoMiner toggled: {}", isMiningActive);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in key handling: ", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && isMiningActive) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
                    isMiningActive = false;
                    return;
                }

                LocalPlayer player = mc.player;
                Level level = mc.level;

                // Reset failed attempts if we successfully mined
                if (lastMinedPos != null && !lastMinedPos.equals(player.blockPosition().below())) {
                    failedMiningAttempts = 0;
                }

                // Safety checks
                if (!isSafeToMine(player, level)) {
                    isMiningActive = false;
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: Stopped - Unsafe conditions"), true);
                    return;
                }

                tickCounter++;
                if (tickCounter < ACTION_COOLDOWN_TICKS) {
                    return;
                }
                tickCounter = 0;

                BlockPos playerPos = player.blockPosition();
                BlockPos blockToMine = playerPos.below();

                // Determine mining direction if not set
                if (currentMiningDirection == null) {
                    currentMiningDirection = findBestMiningDirection(player, level, playerPos);
                    if (currentMiningDirection == null) {
                        isMiningActive = false;
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: No suitable mining direction found"), true);
                        return;
                    }
                }

                // Smooth head movement with improved targeting
                if (!isLookingAtTarget) {
                    updatePlayerLook(player, blockToMine);
                }

                if (lastMinedPos == null || !lastMinedPos.equals(blockToMine)) {
                    if (canMine(player, level, blockToMine)) {
                        if (mineBlock(player, level, blockToMine, mc)) {
                            failedMiningAttempts = 0;
                        } else {
                            failedMiningAttempts++;
                            if (failedMiningAttempts >= MAX_FAILED_ATTEMPTS) {
                                currentMiningDirection = null;
                                failedMiningAttempts = 0;
                            }
                        }
                    } else {
                        currentMiningDirection = null;
                    }
                }

                // Find new mining spot if needed
                if (!canMine(player, level, blockToMine)) {
                    if (!findAndMoveToNewMiningSpot(player, level, playerPos)) {
                        isMiningActive = false;
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: No more sand found"), true);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error in mining logic: ", e);
                isMiningActive = false;
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: Error occurred, stopping"),
                        true
                    );
                }
            }
        }
    }

    private boolean isSafeToMine(LocalPlayer player, Level level) {
        return player != null && level != null && 
               player.onGround() && !player.isInWater() && 
               !player.isInLava() && !player.isOnFire() &&
               player.getHealth() > 0;
    }

    private Direction findBestMiningDirection(LocalPlayer player, Level level, BlockPos playerPos) {
        int maxSandCount = 0;
        Direction bestDir = null;
        
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int sandCount = 0;
            boolean hasSolidBase = true;
            
            for (int i = 0; i < MAX_LOOK_AHEAD; i++) {
                BlockPos checkPos = playerPos.relative(dir, i).below();
                BlockPos belowPos = checkPos.below();
                
                if (level.getBlockState(checkPos).is(BlockTags.SAND)) {
                    if (level.getBlockState(belowPos).isSolidRender(level, belowPos)) {
                        sandCount++;
                    } else {
                        hasSolidBase = false;
                        break;
                    }
                }
            }
            
            if (hasSolidBase && sandCount > maxSandCount) {
                maxSandCount = sandCount;
                bestDir = dir;
            }
        }
        
        return bestDir;
    }

    private void updatePlayerLook(LocalPlayer player, BlockPos targetPos) {
        float targetX = targetPos.getX() + 0.5f;
        float targetY = targetPos.getY() + 0.5f;
        float targetZ = targetPos.getZ() + 0.5f;
        
        double dx = targetX - player.getX();
        double dy = targetY - (player.getY() + player.getEyeHeight());
        double dz = targetZ - player.getZ();
        
        targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        
        // Add natural-looking variation
        targetYaw += (Math.random() - 0.5) * HEAD_VARIATION;
        targetPitch += (Math.random() - 0.5) * HEAD_VARIATION;
        
        float yawDiff = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDiff = Mth.wrapDegrees(targetPitch - player.getXRot());
        
        player.setYRot(player.getYRot() + yawDiff * HEAD_MOVEMENT_SPEED);
        player.setXRot(player.getXRot() + pitchDiff * HEAD_MOVEMENT_SPEED);
        
        if (Math.abs(yawDiff) < MINING_ANGLE_THRESHOLD && Math.abs(pitchDiff) < MINING_ANGLE_THRESHOLD) {
            isLookingAtTarget = true;
        }
    }

    private boolean mineBlock(LocalPlayer player, Level level, BlockPos blockToMine, Minecraft mc) {
        float miningSpeed = 1.0f + (float)(Math.random() * 2 - 1) * MINING_SPEED_VARIATION;
        currentMiningProgress += miningSpeed;
        
        if (currentMiningProgress >= MINING_PROGRESS_THRESHOLD) {
            if (mc.gameMode.destroyBlock(blockToMine)) {
                LOGGER.debug("Peryl Client: Mined sand at {}", blockToMine);
                lastMinedPos = blockToMine.immutable();
                currentMiningProgress = 0f;
                isLookingAtTarget = false;
                
                // Move forward with natural variation
                Vec3 moveDir = new Vec3(
                    currentMiningDirection.getStepX(),
                    0,
                    currentMiningDirection.getStepZ()
                ).normalize().scale(MOVEMENT_SPEED);
                
                moveDir = moveDir.add(
                    (Math.random() - 0.5) * MOVEMENT_VARIATION,
                    0,
                    (Math.random() - 0.5) * MOVEMENT_VARIATION
                );
                
                player.setDeltaMovement(moveDir);
                return true;
            }
        }
        return false;
    }

    private boolean findAndMoveToNewMiningSpot(LocalPlayer player, Level level, BlockPos playerPos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos nextPos = playerPos.relative(dir);
            BlockPos sandBelow = nextPos.below();
            
            if (isSafeToStandAndMineFrom(player, level, nextPos, sandBelow)) {
                Vec3 target = new Vec3(
                    nextPos.getX() + 0.5 + (Math.random() - 0.5) * HEAD_VARIATION,
                    nextPos.getY(),
                    nextPos.getZ() + 0.5 + (Math.random() - 0.5) * HEAD_VARIATION
                );
                Vec3 direction = target.subtract(player.position()).normalize().scale(MOVEMENT_SPEED);
                player.setDeltaMovement(direction);
                isLookingAtTarget = false;
                lastMinedPos = null;
                return true;
            }
        }
        return false;
    }

    private boolean canMine(LocalPlayer player, Level level, BlockPos blockToMinePos) {
        BlockState stateToMine = level.getBlockState(blockToMinePos);
        if (!stateToMine.is(BlockTags.SAND)) {
            LOGGER.trace("Block at {} is not sand: {}", blockToMinePos, stateToMine.getBlock()); // No change needed
            return false;
        }
        BlockPos playerHeadPos = player.blockPosition().above();
        if (!level.isEmptyBlock(playerHeadPos) || !level.isEmptyBlock(playerHeadPos.above())) {
            LOGGER.trace("Not enough headroom for player at {}", player.blockPosition()); // No change needed
            return false;
        }
        BlockPos posBelowSand = blockToMinePos.below();
        BlockState stateBelowSand = level.getBlockState(posBelowSand);
        if (!stateBelowSand.isSolidRender(level, posBelowSand)) {
            LOGGER.trace("Block below sand at {} is not solid: {}", posBelowSand, stateBelowSand.getBlock()); // No change needed
            return false;
        }
        return true;
    }

    private boolean isSafeToStandAndMineFrom(LocalPlayer player, Level level, BlockPos targetPlayerPos, BlockPos sandBlockToMineUnderTarget) {
        if (!level.isEmptyBlock(targetPlayerPos.above()) || !level.isEmptyBlock(targetPlayerPos.above(2))) {
           LOGGER.trace("No headroom at proposed stand pos {}", targetPlayerPos); // No change needed
            return false;
        }
        if (!level.getBlockState(targetPlayerPos).canBeReplaced()) {
           LOGGER.trace("Proposed stand pos {} is not air/replaceable.", targetPlayerPos); // No change needed
        }
        BlockState sandState = level.getBlockState(sandBlockToMineUnderTarget);
        if (!sandState.is(BlockTags.SAND)) {
            LOGGER.trace("Proposed mine pos {} from {} is not sand: {}", sandBlockToMineUnderTarget, targetPlayerPos, sandState.getBlock()); // No change needed
            return false;
        }
        BlockPos posBelowSand = sandBlockToMineUnderTarget.below();
        BlockState stateBelowSand = level.getBlockState(posBelowSand);
        if (!stateBelowSand.isSolidRender(level, posBelowSand)) {
            LOGGER.trace("Block below proposed sand target {} is not solid: {}", posBelowSand, stateBelowSand.getBlock()); // No change needed
            return false;
        }
        return true;
    }
}