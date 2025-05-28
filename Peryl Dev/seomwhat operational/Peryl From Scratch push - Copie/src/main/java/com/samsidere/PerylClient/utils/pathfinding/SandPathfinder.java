package samsidere.perylclient.pathfinding; // Crée un nouveau package pour le pathfinding

import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockSand;
import net.minecraft.block.Block; // Ajouté pour Block
import net.minecraft.block.material.Material; // Ajouté pour Material
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class SandPathfinder {

    private static final int MAX_PATH_LENGTH = 200;
    private static final int MAX_NODES_EVALUATED = 5000;

    private static class PathNode {
        public BlockPos pos;
        public PathNode parent;
        public double gCost;
        public double hCost;
        public double fCost;

        public PathNode(BlockPos pos, PathNode parent, double gCost, double hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathNode pathNode = (PathNode) o;
            return pos.equals(pathNode.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    public static List<BlockPos> findPath(BlockPos startPos, BlockPos endPos, World world) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Set<PathNode> closedSet = new HashSet<>();

        PathNode startNode = new PathNode(startPos, null, 0, getDistance(startPos, endPos));
        openSet.add(startNode);

        int nodesEvaluated = 0;

        while (!openSet.isEmpty() && nodesEvaluated < MAX_NODES_EVALUATED) {
            PathNode currentNode = openSet.poll();
            nodesEvaluated++;

            if (currentNode.pos.equals(endPos)) {
                return reconstructPath(currentNode);
            }

            closedSet.add(currentNode);

            for (BlockPos neighborPos : getWalkableNeighbors(currentNode.pos, world)) {
                PathNode neighborNode = new PathNode(neighborPos, currentNode,
                        currentNode.gCost + getDistance(currentNode.pos, neighborPos),
                        getDistance(neighborPos, endPos)
                );

                if (closedSet.contains(neighborNode)) {
                    continue;
                }

                if (!openSet.contains(neighborNode) || neighborNode.gCost < currentNode.gCost) {
                    openSet.remove(neighborNode);
                    openSet.add(neighborNode);
                }
            }
        }
        return null;
    }

    private static double getDistance(BlockPos pos1, BlockPos pos2) {
        double dx = Math.abs(pos1.getX() - pos2.getX());
        double dy = Math.abs(pos1.getY() - pos2.getY());
        double dz = Math.abs(pos1.getZ() - pos2.getZ());
        return dx + dy + dz;
    }

    /**
     * Retourne une liste des BlockPos où le joueur peut se déplacer depuis la position actuelle.
     * Prend en compte les mouvements basiques : marche, chute, et saut d'un bloc.
     */
    private static List<BlockPos> getWalkableNeighbors(BlockPos currentPos, World world) {
        List<BlockPos> neighbors = new ArrayList<>();
        BlockPos[] cardinalDirections = {
                currentPos.north(),
                currentPos.east(),
                currentPos.south(),
                currentPos.west()
        };

        for (BlockPos move : cardinalDirections) {
            // Mouvement horizontal (marche normale)
            if (isWalkable(move, world)) {
                neighbors.add(move);
            }

            // Mouvement en montée (saut d'un bloc)
            BlockPos moveUp = move.up();
            if (isWalkable(moveUp, world) && !isSolid(currentPos.up(), world)) { // Ne pas essayer de sauter si un bloc est au-dessus de nous
                neighbors.add(moveUp);
            }

            // Mouvement en descente (chute d'un ou deux blocs)
            BlockPos moveDown = move.down();
            if (isWalkable(moveDown, world)) {
                neighbors.add(moveDown);
            }
            BlockPos moveDown2 = move.down(2); // Chute de 2 blocs
            if (isWalkable(moveDown2, world)) {
                neighbors.add(moveDown2);
            }
        }
        return neighbors;
    }

    // Vérifie si un bloc peut être marché dessus (est du sable et il y a de l'espace au-dessus)
    private static boolean isWalkable(BlockPos pos, World world) {
        // Le bloc sur lequel on marche doit être du sable
        if (!(world.getBlockState(pos.down()).getBlock() instanceof BlockSand)) {
            return false;
        }
        // Les deux blocs au-dessus de la position actuelle doivent être libres (air ou remplaçable)
        return world.isAirBlock(pos) && world.isAirBlock(pos.up());
    }

    // Vérifie si un bloc est solide (non traversable)
    private static boolean isSolid(BlockPos pos, World world) {
        Block block = world.getBlockState(pos).getBlock();
        return block.getMaterial().isSolid() && !block.isPassable(world, pos);
    }

    private static List<BlockPos> reconstructPath(PathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        PathNode currentNode = endNode;
        while (currentNode.parent != null) {
            path.add(currentNode.pos);
            currentNode = currentNode.parent;
        }
        Collections.reverse(path);
        if (path.size() > MAX_PATH_LENGTH) {
            return path.subList(0, MAX_PATH_LENGTH);
        }
        return path;
    }
}