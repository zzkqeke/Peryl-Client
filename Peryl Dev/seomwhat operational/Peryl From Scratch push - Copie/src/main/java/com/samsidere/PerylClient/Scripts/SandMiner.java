package samsidere.PerylClient.Scripts

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.block.BlockSand;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.List;
import samsidere.perylclient.pathfinding.SandPathfinder;
import net.minecraft.util.MathHelper; // Pour des calculs mathématiques utiles

public class SandMiner {

    private static boolean isMiningActive = false;
    private static int miningTickCounter = 0;
    private static final int TICKS_PER_ACTION = 5; // Fréquence des actions du bot

    // Clés de mouvement
    private static final KeyBinding keyBindForward = Minecraft.getMinecraft().gameSettings.keyBindForward;
    private static final KeyBinding keyBindRight = Minecraft.getMinecraft().gameSettings.keyBindRight;
    private static final KeyBinding keyBindLeft = Minecraft.getMinecraft().gameSettings.keyBindLeft;
    private static final KeyBinding keyBindAttack = Minecraft.getMinecraft().gameSettings.keyBindAttack;
    private static final KeyBinding keyBindJump = Minecraft.getMinecraft().gameSettings.keyBindJump; // Pour les sauts

    // Variables pour le mouvement circulaire/spirale
    private static int blocksToMove = 1;
    private static int blocksMoved = 0;
    private static int turnsMade = 0;
    private static float initialYaw;

    // Variables pour le pathfinder et le mouvement humainisé
    private static List<BlockPos> currentPath = null;
    private static int pathIndex = 0;
    private static boolean needsPath = false;

    // Paramètres pour le mouvement smooth
    private static final float YAW_SPEED = 10.0F; // Vitesse de rotation (degrés par tick)
    private static final float MIN_YAW_DIFF = 1.0F; // Différence d'angle minimale pour commencer à tourner
    private static final double MIN_DIST_TO_WAYPOINT = 0.2; // Distance minimale pour considérer un waypoint atteint

    public static void toggleMining() {
        isMiningActive = !isMiningActive;
        Minecraft.getMinecraft().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§a[SandMiner] Minage " + (isMiningActive ? "activé" : "désactivé") + "."));

        if (isMiningActive) {
            initialYaw = Minecraft.getMinecraft().thePlayer.rotationYaw;
            blocksToMove = 1;
            blocksMoved = 0;
            turnsMade = 0;
            currentPath = null;
            pathIndex = 0;
            needsPath = false;
        } else {
            releaseAllKeys();
        }
    }

    private static void releaseAllKeys() {
        KeyBinding.setKeyBindState(keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(keyBindRight.getKeyCode(), false);
        KeyBinding.setKeyBindState(keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(keyBindJump.getKeyCode(), false);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();
            if (isMiningActive && mc.thePlayer != null) {
                miningTickCounter++;

                // Toujours relâcher les touches au début du tick pour éviter les appuis constants
                releaseAllKeys();

                if (miningTickCounter >= TICKS_PER_ACTION) {
                    miningTickCounter = 0; // Réinitialise le compteur d'action

                    BlockPos playerBlockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                    BlockPos playerBlockPosBelow = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
                    IBlockState blockStateBelow = mc.theWorld.getBlockState(playerBlockPosBelow);

                    // --- Logique du Pathfinder (Priorité 1) ---
                    if (currentPath != null && pathIndex < currentPath.size()) {
                        BlockPos nextWaypoint = currentPath.get(pathIndex);

                        // Calcul de la direction et rotation progressive
                        double deltaX = nextWaypoint.getX() + 0.5 - mc.thePlayer.posX;
                        double deltaZ = nextWaypoint.getZ() + 0.5 - mc.thePlayer.posZ;
                        float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
                        targetYaw = normalizeYaw(targetYaw);

                        float yawDiff = normalizeYaw(targetYaw - mc.thePlayer.rotationYaw);

                        // Rotation progressive
                        if (Math.abs(yawDiff) > MIN_YAW_DIFF) {
                            if (yawDiff > 0) {
                                mc.thePlayer.rotationYaw += Math.min(yawDiff, YAW_SPEED);
                            } else {
                                mc.thePlayer.rotationYaw += Math.max(yawDiff, -YAW_SPEED);
                            }
                            mc.thePlayer.rotationYaw = normalizeYaw(mc.thePlayer.rotationYaw); // Normalise après l'ajustement
                        }

                        // Mouvement d'avancement
                        double distToWaypoint = mc.thePlayer.getDistance(nextWaypoint.getX() + 0.5, nextWaypoint.getY(), nextWaypoint.getZ() + 0.5);

                        if (distToWaypoint > MIN_DIST_TO_WAYPOINT && Math.abs(yawDiff) < 30) { // Avance seulement si à peu près face au waypoint
                            KeyBinding.setKeyBindState(keyBindForward.getKeyCode(), true);
                        }

                        // Gérer le saut si le waypoint est plus haut
                        if (nextWaypoint.getY() > mc.thePlayer.posY + 0.1 && mc.thePlayer.onGround) {
                            KeyBinding.setKeyBindState(keyBindJump.getKeyCode(), true);
                        } else {
                            KeyBinding.setKeyBindState(keyBindJump.getKeyCode(), false); // Relâche le saut si pas nécessaire
                        }

                        // Vérifier si le waypoint est atteint (ou presque)
                        if (distToWaypoint < MIN_DIST_TO_WAYPOINT) {
                            pathIndex++;
                            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§a[SandMiner] Waypoint " + (pathIndex) + "/" + currentPath.size() + " atteint."));
                        }

                        // Si le chemin est terminé
                        if (pathIndex >= currentPath.size()) {
                            currentPath = null;
                            pathIndex = 0;
                            needsPath = false;
                            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§a[SandMiner] Chemin terminé, reprise du minage en spirale."));
                            releaseAllKeys();
                        }
                    }
                    // --- Logique de recherche de Pathfinder (Priorité 2) ---
                    else if (needsPath) {
                        mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§e[SandMiner] Recherche d'un chemin de sable..."));
                        BlockPos targetSandPos = findNearbySand(playerBlockPos, mc.theWorld, 15); // Cherche à 15 blocs

                        if (targetSandPos != null) {
                            currentPath = SandPathfinder.findPath(playerBlockPos, targetSandPos, mc.theWorld);
                            if (currentPath != null && !currentPath.isEmpty()) {
                                pathIndex = 0;
                                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§a[SandMiner] Chemin trouvé. Longueur : " + currentPath.size()));
                            } else {
                                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§c[SandMiner] Aucun chemin de sable valide trouvé."));
                                needsPath = false;
                            }
                        } else {
                            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§c[SandMiner] Pas de sable proche pour un chemin."));
                            needsPath = false;
                        }
                    }
                    // --- Logique de Minage en Spirale (Priorité 3) ---
                    else {
                        if (blockStateBelow.getBlock() instanceof BlockSand) {
                            KeyBinding.setKeyBindState(keyBindAttack.getKeyCode(), true); // Minage
                            // Le relâcher est géré par releaseAllKeys au début du prochain tick d'action

                            // Mouvement de spirale
                            KeyBinding.setKeyBindState(keyBindForward.getKeyCode(), true);
                            blocksMoved++;

                            if (blocksMoved >= blocksToMove) {
                                blocksMoved = 0;

                                // Rotation progressive pour la spirale
                                float targetSpiralYaw = normalizeYaw(mc.thePlayer.rotationYaw + 90.0F);
                                mc.thePlayer.rotationYaw = smoothRotate(mc.thePlayer.rotationYaw, targetSpiralYaw, YAW_SPEED);

                                turnsMade++;
                                if (turnsMade % 2 == 0) {
                                    blocksToMove++;
                                }
                            }
                        } else {
                            // Plus de sable, déclenche le pathfinder
                            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§c[SandMiner] Plus de sable sous le joueur. Initialisation du pathfinder."));
                            needsPath = true;
                        }
                    }
                }
            } else {
                releaseAllKeys(); // S'assurer que tout est relâché si le mod est désactivé
            }
        }
    }

    // Normalise l'angle yaw entre -180 et 180
    private float normalizeYaw(float yaw) {
        yaw %= 360.0F;
        if (yaw >= 180.0F) yaw -= 360.0F;
        if (yaw < -180.0F) yaw += 360.0F;
        return yaw;
    }

    // Rotation douce vers un angle cible
    private float smoothRotate(float currentYaw, float targetYaw, float speed) {
        float yawDiff = normalizeYaw(targetYaw - currentYaw);
        if (Math.abs(yawDiff) < speed) {
            return targetYaw;
        } else if (yawDiff > 0) {
            return currentYaw + speed;
        } else {
            return currentYaw - speed;
        }
    }

    /**
     * Recherche un bloc de sable proche dans un rayon donné.
     * Inclut une vérification rudimentaire de "passabilité" au-dessus du sable trouvé.
     */
    private static BlockPos findNearbySand(BlockPos center, World world, int radius) {
        for (int yOffset = -radius; yOffset <= radius; yOffset++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    BlockPos searchPos = center.add(xOffset, yOffset, zOffset);
                    if (world.getBlockState(searchPos).getBlock() instanceof BlockSand) {
                        // Vérifie si le bloc au-dessus est libre pour que le joueur puisse y aller
                        // Le joueur a besoin de 2 blocs de hauteur libre pour marcher
                        if (world.isAirBlock(searchPos.up()) && world.isAirBlock(searchPos.up(2))) {
                            return searchPos;
                        }
                    }
                }
            }
        }
        return null;
    }
}