package samsidere.perylclient.utils.pathfinding; // Changement du package

// Importation des classes de Minecraft pour gérer les blocs et les positions
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

// Importation des classes spécifiques au client modifié
import samsidere.perylclient.PerylClient; // Remplacement de GumTuneClient par PerylClient
import samsidere.perylclient.modules.player.PathFinding;
import samsidere.perylclient.utils.ModUtils;
import samsidere.perylclient.utils.RaytracingUtils;
import samsidere.perylclient.utils.VectorUtils;

// Importation des classes Java pour les listes et comparaisons
import java.util.ArrayList;
import java.util.Comparator;

// Définition de la classe principale gérant l'algorithme de recherche du chemin (Pathfinding)
public class AStarCustomPathfinder {
    private final Vec3 startVec3; // Position de départ du pathfinding
    private final Vec3 endVec3; // Position de destination
    private ArrayList<Vec3> path = new ArrayList<>(); // Stocke le chemin calculé
    private final ArrayList<Hub> hubs = new ArrayList<>(); // Stocke les hubs déjà explorés
    private final ArrayList<Hub> hubsToWork = new ArrayList<>(); // Liste des hubs à examiner
    private final double minDistanceSquared; // Distance minimale acceptée pour atteindre la destination
    public static long counter = 0; // Compteur du temps de calcul

    // Directions cardinales pour explorer les déplacements possibles
    private static final Vec3[] flatCardinalDirections = {
        new Vec3(1, 0, 0),
        new Vec3(-1, 0, 0),
        new Vec3(0, 0, 1),
        new Vec3(0, 0, -1)
    };

    // Constructeur de la classe prenant une position de départ et de destination
    public AStarCustomPathfinder(Vec3 startVec3, Vec3 endVec3, double minDistanceSquared) {
        this.startVec3 = VectorUtils.floorVec(startVec3); // Arrondit la position de départ
        this.endVec3 = VectorUtils.floorVec(endVec3); // Arrondit la position de destination
        this.minDistanceSquared = minDistanceSquared; // Distance minimale acceptée
    }

    // Retourne le chemin calculé
    public ArrayList<Vec3> getPath() {
        return path;
    }

    // Déclenche le calcul du chemin avec une limite de 2000 itérations
    public void compute() {
        compute(2000, 1);
    }

    // Algorithme de recherche du chemin basé sur A*
    public void compute(int loops, int depth) {
        counter = 0;
        PathFinding.renderHubs.clear(); // Nettoie les points de rendu du chemin
        path.clear();
        hubsToWork.clear();
        ArrayList<Vec3> initPath = new ArrayList<>();
        initPath.add(startVec3);
        hubsToWork.add(new Hub(startVec3, null, initPath, startVec3.squareDistanceTo(endVec3), 0.0, 0.0));

        // Boucle de recherche de chemin
        search:
        for (int i = 0; i < loops; ++i) {
            hubsToWork.sort(new CompareHub()); // Trie les hubs en fonction de leur proximité à la destination
            if (hubsToWork.isEmpty()) {
                break;
            }
            for (Hub hub : new ArrayList<>(hubsToWork)) {
                if (hubsToWork.size() > depth) {
                    break;
                }

                hubsToWork.remove(hub);
                hubs.add(hub);
                PathFinding.renderHubs.add(new BlockPos(VectorUtils.ceilVec(hub.getLoc())));

                // Recherche des blocs accessibles pour téléportation
                for (BlockPos blockPos : RaytracingUtils.getAllTeleportableBlocksNew(VectorUtils.ceilVec(hub.getLoc()).addVector(0.5, 1.62, 0.5), 16)) {
                    Vec3 loc = new Vec3(blockPos);
                    if (addHub(hub, loc, 0)) {
                        break search;
                    }
                }
            }
        }
        ModUtils.sendMessage("Done calculating path, searched " + PathFinding.renderHubs.size() + " blocks, took: " + counter + "ms");

        hubs.sort(new CompareHub());
        path = hubs.get(0).getPath();
    }

    // Vérifie si une position est valide pour le déplacement
    public static boolean checkPositionValidity(Vec3 loc) {
        return checkPositionValidity(new BlockPos((int) loc.xCoord, (int) loc.yCoord, (int) loc.zCoord));
    }

    // Vérifie si une position est téléportable
    private static boolean canTeleportTo(BlockPos blockPos) {
        IBlockState blockState = PerylClient.mc.theWorld.getBlockState(blockPos); // Modification ici
        Block block = blockState.getBlock();
        return block.isCollidable() && block != Blocks.carpet &&
               block.getCollisionBoundingBox(PerylClient.mc.theWorld, blockPos, blockState) != null;
    }

    // Classe interne représentant un "hub" (étape dans le chemin)
    private static class Hub {
        private Vec3 loc;
        private Hub parent;
        private ArrayList<Vec3> path;
        private double squareDistanceToFromTarget;
        private double cost;
        private double totalCost;

        public Hub(Vec3 loc, Hub parent, ArrayList<Vec3> path, double squareDistanceToFromTarget, double cost, double totalCost) {
            this.loc = loc;
            this.parent = parent;
            this.path = path;
            this.squareDistanceToFromTarget = squareDistanceToFromTarget;
            this.cost = cost;
            this.totalCost = totalCost;
        }

        public Vec3 getLoc() { return loc; }
        public Hub getParent() { return parent; }
        public ArrayList<Vec3> getPath() { return path; }
        public double getCost() { return cost; }
        public double getTotalCost() { return totalCost; }
    }

    // Comparateur de hubs pour le tri
    public static class CompareHub implements Comparator<Hub> {
        @Override
        public int compare(Hub o1, Hub o2) {
            return Double.compare(o1.getSquareDistanceToFromTarget() + o1.getTotalCost(), o2.getSquareDistanceToFromTarget() + o2.getTotalCost());
        }
    }
}