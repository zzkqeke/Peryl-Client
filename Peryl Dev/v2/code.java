// New package declaration
package com.yourname.perylclient; // <--- CHANGED

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


// Changed class name and MODID
@Mod(PerylClientMod.MODID) // <--- CHANGED class name reference
public class PerylClientMod { // <--- CHANGED class name
    public static final String MODID = "perylclient"; // <--- CHANGED MODID string
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean isMiningActive = false;
    private int tickCounter = 0;
    private static final int ACTION_COOLDOWN_TICKS = 10;

    private BlockPos lastMinedPos = null;

    public static KeyMapping toggleMiningKey;

    public PerylClientMod() { // <--- CHANGED constructor name
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Peryl Client Setup"); // <--- Optional: Changed log message
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            toggleMiningKey = new KeyMapping(
                    "key.perylclient.toggleminer", // <--- CHANGED key name
                    GLFW.GLFW_KEY_K,
                    "key.categories.perylclient" // <--- CHANGED category name
            );
            event.register(toggleMiningKey);
            LOGGER.info("Registered keybinds for Peryl Client"); // <--- Optional: Changed log message
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (toggleMiningKey.consumeClick()) {
            isMiningActive = !isMiningActive;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: " + (isMiningActive ? "ON" : "OFF")), // <--- Changed message
                        true
                );
            }
            if (!isMiningActive) {
                lastMinedPos = null;
            }
            LOGGER.info("Peryl Client AutoMiner toggled: {}", isMiningActive); // <--- Changed log message
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && isMiningActive) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            Level level = mc.level;

            if (player == null || level == null || mc.gameMode == null) {
                return;
            }

            tickCounter++;
            if (tickCounter < ACTION_COOLDOWN_TICKS) {
                return;
            }
            tickCounter = 0;

            BlockPos playerPos = player.blockPosition();
            BlockPos blockToMine = playerPos.below();

            if (lastMinedPos == null || !lastMinedPos.equals(blockToMine)) {
                if (canMine(player, level, blockToMine)) {
                    mc.gameMode.startDestroyBlock(blockToMine, Direction.DOWN);
                    if (mc.gameMode.destroyBlock(blockToMine)) {
                        LOGGER.debug("Peryl Client: Mined sand at {}", blockToMine); // <--- Changed log
                        lastMinedPos = blockToMine.immutable();
                        return;
                    } else {
                        LOGGER.warn("Peryl Client: Failed to mine sand at {} or it wasn't instant.", blockToMine); // <--- Changed log
                    }
                }
            }

            if (lastMinedPos != null && lastMinedPos.equals(playerPos)) {
                BlockPos currentStandingPos = player.blockPosition();
                for (Direction moveDir : Direction.Plane.HORIZONTAL) {
                    BlockPos nextPotentialStandPos = currentStandingPos.relative(moveDir);
                    BlockPos sandBelowNextPotentialPos = nextPotentialStandPos.below();

                    if (isSafeToStandAndMineFrom(player, level, nextPotentialStandPos, sandBelowNextPotentialPos)) {
                        player.setPos(nextPotentialStandPos.getX() + 0.5, nextPotentialStandPos.getY(), nextPotentialStandPos.getZ() + 0.5);
                        LOGGER.debug("Peryl Client: Moved to {}", nextPotentialStandPos); // <--- Changed log
                        lastMinedPos = null;
                        return;
                    }
                }
                LOGGER.debug("Peryl Client: No suitable adjacent sand found to move to from {}. Turning off.", currentStandingPos); // <--- Changed log
                isMiningActive = false;
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: No more sand found."), true); // <--- Changed message
            } else if (lastMinedPos == null && !canMine(player, level, blockToMine)) {
                BlockPos currentStandingPos = player.blockPosition();
                for (Direction moveDir : Direction.Plane.HORIZONTAL) {
                    BlockPos nextPotentialStandPos = currentStandingPos.relative(moveDir);
                    BlockPos sandBelowThat = nextPotentialStandPos.below();

                    if(isSafeToStandAndMineFrom(player, level, nextPotentialStandPos, sandBelowThat)){
                        player.setPos(nextPotentialStandPos.getX() + 0.5, nextPotentialStandPos.getY(), nextPotentialStandPos.getZ() + 0.5);
                        LOGGER.debug("Peryl Client: Initial move to {}", nextPotentialStandPos); // <--- Changed log
                        lastMinedPos = null;
                        return;
                    }
                }
                LOGGER.debug("Peryl Client: Cannot mine current block {} and no suitable adjacent sand found. Turning off.", blockToMine); // <--- Changed log
                isMiningActive = false;
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Peryl Client - AutoMiner: Not on sand or no sand nearby."), true); // <--- Changed message
            }
        }
    }

    private boolean canMine(LocalPlayer player, Level level, BlockPos blockToMinePos) {
        BlockState stateToMine = level.getBlockState(blockToMinePos);
        if (!stateToMine.is(BlockTags.SAND)) {
            // LOGGER.trace("Block at {} is not sand: {}", blockToMinePos, stateToMine.getBlock()); // No change needed
            return false;
        }
        BlockPos playerHeadPos = player.blockPosition().above();
        if (!level.isEmptyBlock(playerHeadPos) || !level.isEmptyBlock(playerHeadPos.above())) {
            // LOGGER.trace("Not enough headroom for player at {}", player.blockPosition()); // No change needed
            return false;
        }
        BlockPos posBelowSand = blockToMinePos.below();
        BlockState stateBelowSand = level.getBlockState(posBelowSand);
        if (!stateBelowSand.isSolidRender(level, posBelowSand)) {
            // LOGGER.trace("Block below sand at {} is not solid: {}", posBelowSand, stateBelowSand.getBlock()); // No change needed
            return false;
        }
        return true;
    }

    private boolean isSafeToStandAndMineFrom(LocalPlayer player, Level level, BlockPos targetPlayerPos, BlockPos sandBlockToMineUnderTarget) {
        if (!level.isEmptyBlock(targetPlayerPos.above()) || !level.isEmptyBlock(targetPlayerPos.above(2))) {
            // LOGGER.trace("No headroom at proposed stand pos {}", targetPlayerPos); // No change needed
            return false;
        }
        if (!level.getBlockState(targetPlayerPos).canBeReplaced()) {
            // LOGGER.trace("Proposed stand pos {} is not air/replaceable.", targetPlayerPos); // No change needed
        }
        BlockState sandState = level.getBlockState(sandBlockToMineUnderTarget);
        if (!sandState.is(BlockTags.SAND)) {
            // LOGGER.trace("Proposed mine pos {} from {} is not sand: {}", sandBlockToMineUnderTarget, targetPlayerPos, sandState.getBlock()); // No change needed
            return false;
        }
        BlockPos posBelowSand = sandBlockToMineUnderTarget.below();
        BlockState stateBelowSand = level.getBlockState(posBelowSand);
        if (!stateBelowSand.isSolidRender(level, posBelowSand)) {
            // LOGGER.trace("Block below proposed sand target {} is not solid: {}", posBelowSand, stateBelowSand.getBlock()); // No change needed
            return false;
        }
        return true;
    }
}