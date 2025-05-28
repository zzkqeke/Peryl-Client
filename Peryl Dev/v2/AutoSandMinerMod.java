package com.yourname.autosandminer;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
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

@Mod(AutoSandMinerMod.MODID)
public class AutoSandMinerMod {
    public static final String MODID = "autosandminer";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean isMiningActive = false;
    private int tickCounter = 0;
    private static final int ACTION_COOLDOWN_TICKS = 10; // Adjust for speed, 10 ticks = 0.5 seconds

    private BlockPos lastMinedPos = null; // To ensure we move after mining

    public static KeyMapping toggleMiningKey;

    public AutoSandMinerMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("AutoSandMiner Client Setup");
    }

    // Keybinding registration (new way for 1.19+)
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            toggleMiningKey = new KeyMapping(
                    "key.autosandminer.toggle",
                    GLFW.GLFW_KEY_K, // Default key K
                    "key.categories.autosandminer"
            );
            event.register(toggleMiningKey);
            LOGGER.info("Registered keybinds for AutoSandMiner");
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (toggleMiningKey.consumeClick()) {
            isMiningActive = !isMiningActive;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Auto Sand Miner: " + (isMiningActive ? "ON" : "OFF")),
                        true // true for overlay message
                );
            }
            if (!isMiningActive) {
                lastMinedPos = null; // Reset when turned off
            }
            LOGGER.info("Auto Sand Miner toggled: {}", isMiningActive);
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

            BlockPos playerPos = player.blockPosition(); // Position of player's feet

            // --- Mining Phase ---
            BlockPos blockToMine = playerPos.below(); // Block player is standing on

            if (lastMinedPos == null || !lastMinedPos.equals(blockToMine)) { // Only try to mine if we haven't just mined it or if we moved
                if (canMine(player, level, blockToMine)) {
                    // Simulate breaking the block
                    // For instant break blocks like sand, startDestroyBlock + destroyBlock is often enough
                    mc.gameMode.startDestroyBlock(blockToMine, Direction.DOWN); // Direction player is looking relative to block
                    if (mc.gameMode.destroyBlock(blockToMine)) {
                        LOGGER.debug("Mined sand at {}", blockToMine);
                        lastMinedPos = blockToMine.immutable(); // Mark as mined
                        // Player will fall onto this position after block is broken
                        // No explicit move needed here, gravity handles it.
                        // The next tick will handle moving to a new sand block.
                        return; // Wait for next tick cycle to move
                    } else {
                        LOGGER.warn("Failed to mine sand at {} or it wasn't instant.", blockToMine);
                        // Could be that the block isn't sand, or some other issue.
                        // isMiningActive = false; // Optionally stop if mining fails unexpectedly
                    }
                }
            }


            // --- Movement Phase (if we have mined the block we were on, or if we can't mine current) ---
            // This means the player is now standing where the sand *was*.
            // We need to find an adjacent block that *is* sand to move onto.
            if (lastMinedPos != null && lastMinedPos.equals(playerPos)) { // If player is now standing on the spot they just mined
                 // The player has fallen into the spot. Now find next target.
                BlockPos currentStandingPos = player.blockPosition(); // This is where the sand *was*

                for (Direction moveDir : Direction.Plane.HORIZONTAL) {
                    BlockPos nextPotentialStandPos = currentStandingPos.relative(moveDir);
                    BlockPos sandBelowNextPotentialPos = nextPotentialStandPos.below();

                    if (isSafeToStandAndMineFrom(player, level, nextPotentialStandPos, sandBelowNextPotentialPos)) {
                        // Teleport player to center of the block
                        // This is a bit abrupt. Smooth movement is much more complex.
                        player.setPos(nextPotentialStandPos.getX() + 0.5, nextPotentialStandPos.getY(), nextPotentialStandPos.getZ() + 0.5);
                        LOGGER.debug("Moved to {}", nextPotentialStandPos);
                        lastMinedPos = null; // Reset so we can mine the new block below us
                        return; // Moved, wait for next tick cycle
                    }
                }
                 LOGGER.debug("No suitable adjacent sand found to move to from {}. Turning off.", currentStandingPos);
                 isMiningActive = false; // No place to go
                 player.displayClientMessage(net.minecraft.network.chat.Component.literal("Auto Sand Miner: No more sand found."), true);
            } else if (lastMinedPos == null && !canMine(player, level, blockToMine)) {
                // If we haven't mined anything yet (e.g. on first activation) and can't mine current, try to move.
                // This logic is similar to above, finding an initial spot if not starting on sand.
                BlockPos currentStandingPos = player.blockPosition();
                 for (Direction moveDir : Direction.Plane.HORIZONTAL) {
                    BlockPos nextPotentialStandPos = currentStandingPos.relative(moveDir); // Where player would move
                    BlockPos sandBelowThat = nextPotentialStandPos.below(); // The sand they'd stand on

                    if(isSafeToStandAndMineFrom(player, level, nextPotentialStandPos, sandBelowThat)){
                        player.setPos(nextPotentialStandPos.getX() + 0.5, nextPotentialStandPos.getY(), nextPotentialStandPos.getZ() + 0.5);
                        LOGGER.debug("Initial move to {}", nextPotentialStandPos);
                        lastMinedPos = null;
                        return;
                    }
                }
                LOGGER.debug("Cannot mine current block {} and no suitable adjacent sand found. Turning off.", blockToMine);
                isMiningActive = false;
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Auto Sand Miner: Not on sand or no sand nearby."), true);
            }
        }
    }

    /**
     * Checks if the block at 'blockToMinePos' is sand and if it's safe to mine it
     * (i.e., player has headroom and won't fall into oblivion).
     */
    private boolean canMine(LocalPlayer player, Level level, BlockPos blockToMinePos) {
        BlockState stateToMine = level.getBlockState(blockToMinePos);
        // Using BlockTags.SAND is good practice as it includes red_sand too.
        if (!stateToMine.is(BlockTags.SAND)) {
            LOGGER.trace("Block at {} is not sand: {}", blockToMinePos, stateToMine.getBlock());
            return false;
        }

        // Check headroom for player (current position)
        BlockPos playerHeadPos = player.blockPosition().above();
        if (!level.isEmptyBlock(playerHeadPos) || !level.isEmptyBlock(playerHeadPos.above())) {
            LOGGER.trace("Not enough headroom for player at {}", player.blockPosition());
            return false;
        }

        // Check block below the sand to ensure it's solid (player will fall onto 'blockToMinePos' after mining)
        BlockPos posBelowSand = blockToMinePos.below();
        BlockState stateBelowSand = level.getBlockState(posBelowSand);
        // isSolid() is a bit lenient, isSolidRender is stricter and better for "can stand on"
        if (!stateBelowSand.isSolidRender(level, posBelowSand)) {
            LOGGER.trace("Block below sand at {} is not solid: {}", posBelowSand, stateBelowSand.getBlock());
            return false;
        }
        return true;
    }

    /**
     * Checks if it's safe for the player to move to 'targetPlayerPos',
     * assuming they will then mine 'sandBlockToMineUnderTarget'.
     */
    private boolean isSafeToStandAndMineFrom(LocalPlayer player, Level level, BlockPos targetPlayerPos, BlockPos sandBlockToMineUnderTarget) {
        // 1. Check headroom at targetPlayerPos
        if (!level.isEmptyBlock(targetPlayerPos.above()) || !level.isEmptyBlock(targetPlayerPos.above(2))) {
            LOGGER.trace("No headroom at proposed stand pos {}", targetPlayerPos);
            return false;
        }

        // 2. Check if the block the player would stand on (targetPlayerPos) is actually air or replaceable
        //    This is important because setPos moves the player *into* this block.
        //    Player usually occupies targetPlayerPos and targetPlayerPos.above().
        //    Here, targetPlayerPos is the feet position.
        if (!level.getBlockState(targetPlayerPos).canBeReplaced()) { // Player can't move into a solid block
             LOGGER.trace("Proposed stand pos {} is not air/replaceable.", targetPlayerPos);
             //return false; // This might be too restrictive if we allow standing on slabs etc.
                           // For now, assume we want to move *onto* an air block that is above sand.
                           // The crucial check is that sandBlockToMineUnderTarget IS sand.
        }


        // 3. Check if the block they would mine from that new position is sand
        BlockState sandState = level.getBlockState(sandBlockToMineUnderTarget);
        if (!sandState.is(BlockTags.SAND)) {
            LOGGER.trace("Proposed mine pos {} from {} is not sand: {}", sandBlockToMineUnderTarget, targetPlayerPos, sandState.getBlock());
            return false;
        }

        // 4. Check if the block *under* that sand is solid
        BlockPos posBelowSand = sandBlockToMineUnderTarget.below();
        BlockState stateBelowSand = level.getBlockState(posBelowSand);
        if (!stateBelowSand.isSolidRender(level, posBelowSand)) {
            LOGGER.trace("Block below proposed sand target {} is not solid: {}", posBelowSand, stateBelowSand.getBlock());
            return false;
        }

        return true;
    }
}