package samsidere.perylclient.utils;


import com.google.common.base.Predicates;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector3f;
import samsidere.PerylClient.PerylClient;
import samsidere.PerylClient.modules.player.PathFinding;
import samsidere.PerylClient.utils.pathfinding.AStarCustomPathfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RaytracingUtils {

    public static class RaytraceResult {
        public BlockPos blockPos;
        public IBlockState iBlockState;

        public RaytraceResult(BlockPos blockPos, IBlockState iBlockState) {
            this.blockPos = blockPos;
            this.iBlockState = iBlockState;
        }

        public BlockPos getBlockPos() {
            return this.blockPos;
        }

        public IBlockState getIBlockState() {
            return iBlockState;
        }
    }
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static ArrayList<BlockPos> getAllTeleportableBlocksNew(Vec3 vec, float range) {
        long timestamp = System.currentTimeMillis();
        BlockPos origin = new BlockPos(vec);
        Iterable<BlockPos> blocks = BlockPos.getAllInBox(origin.subtract(new Vec3i(range, 16, range)), origin.add(new Vec3i(range, 16, range)));
        ArrayList<BlockPos> validBlocks = (ArrayList<BlockPos>) StreamSupport.stream(
                blocks.spliterator(), false
        ).filter(blockPos ->
                mc.theWorld.getBlockState(blockPos).getBlock().isCollidable() && mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.carpet &&
                        mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.skull && mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.wall_sign &&
                        mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.standing_sign &&
                        !(mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockFence) &&
                        !(mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockFenceGate) &&
                        mc.theWorld.getBlockState(blockPos).getBlock().getCollisionBoundingBox(mc.theWorld, blockPos, mc.theWorld.getBlockState(blockPos)) != null &&
                        mc.theWorld.getBlockState(blockPos.add(0, 1, 0)).getBlock() == Blocks.air &&
                        mc.theWorld.getBlockState(blockPos.add(0, 2, 0)).getBlock() == Blocks.air &&
                        isBlockViewable(vec, blockPos)
        ).collect(Collectors.toList());
        AStarCustomPathfinder.counter += System.currentTimeMillis() - timestamp;
        return validBlocks;
    }

    public static boolean isBlockViewable(Vec3 from, BlockPos goal) {
        return isVecViewable(from, new Vec3(goal.getX() + 0.5, goal.getY() + 0.5, goal.getZ() + 0.5));
    }

    public static boolean isVecViewable(Vec3 from, Vec3 goal) {
        if (Double.isNaN(from.xCoord) || Double.isNaN(from.yCoord) || Double.isNaN(from.zCoord)) return false;
        if (Double.isNaN(goal.xCoord) || Double.isNaN(goal.yCoord) || Double.isNaN(goal.zCoord)) return false;

        int xGoal = MathHelper.floor_double(goal.xCoord);
        int yGoal = MathHelper.floor_double(goal.yCoord);
        int zGoal = MathHelper.floor_double(goal.zCoord);
        int xCurrent = MathHelper.floor_double(from.xCoord);
        int yCurrent = MathHelper.floor_double(from.yCoord);
        int zCurrent = MathHelper.floor_double(from.zCoord);

        int iter = 0;
        while (iter++ <= 200) {
            if (xCurrent == xGoal && yCurrent == yGoal && zCurrent == zGoal) {
                return true;
            }

            if (PerylClient.mc.theWorld.getBlockState(new BlockPos(xCurrent, yCurrent, zCurrent)).getBlock() != Blocks.air) {
                return false;
            }

            boolean flagX = true;
            boolean flagY = true;
            boolean flagZ = true;
            double fullNextX = 999.0;
            double fullNextY = 999.0;
            double fullNextZ = 999.0;

            if (xGoal > xCurrent) {
                fullNextX = (double) xCurrent + 1.0;
            } else if (xGoal < xCurrent) {
                fullNextX = (double) xCurrent + 0.0;
            } else {
                flagX = false;
            }
            if (yGoal > yCurrent) {
                fullNextY = (double) yCurrent + 1.0;
            } else if (yGoal < yCurrent) {
                fullNextY = (double) yCurrent + 0.0;
            } else {
                flagY = false;
            }
            if (zGoal > zCurrent) {
                fullNextZ = (double) zCurrent + 1.0;
            } else if (zGoal < zCurrent) {
                fullNextZ = (double) zCurrent + 0.0;
            } else {
                flagZ = false;
            }

            double nextX = 999.0;
            double nextY = 999.0;
            double nextZ = 999.0;
            double dx = goal.xCoord - from.xCoord;
            double dy = goal.yCoord - from.yCoord;
            double dz = goal.zCoord - from.zCoord;
            if (flagX) {
                nextX = (fullNextX - from.xCoord) / dx;
            }
            if (flagY) {
                nextY = (fullNextY - from.yCoord) / dy;
            }
            if (flagZ) {
                nextZ = (fullNextZ - from.zCoord) / dz;
            }
            if (nextX == -0.0) {
                nextX = -1.0E-4;
            }
            if (nextY == -0.0) {
                nextY = -1.0E-4;
            }
            if (nextZ == -0.0) {
                nextZ = -1.0E-4;
            }

            int xMod = 0;
            int yMod = 0;
            int zMod = 0;

            if (nextX < nextY && nextX < nextZ) {
                if (xGoal <= xCurrent) {
                    xMod = 1;
                }
                from = new Vec3(fullNextX, from.yCoord + dy * nextX, from.zCoord + dz * nextX);
            } else if (nextY < nextZ) {
                if (yGoal <= yCurrent) {
                    yMod = 1;
                }
                from = new Vec3(from.xCoord + dx * nextY, fullNextY, from.zCoord + dz * nextY);
            } else {
                if (zGoal <= zCurrent) {
                    zMod = 1;
                }
                from = new Vec3(from.xCoord + dx * nextZ, from.yCoord + dy * nextZ, fullNextZ);
            }

            xCurrent = MathHelper.floor_double(from.xCoord) - xMod;
            yCurrent = MathHelper.floor_double(from.yCoord) - yMod;
            zCurrent = MathHelper.floor_double(from.zCoord) - zMod;
        }

        return false;
    }

    public static boolean isPointOnLine(Vec3 from, Vec3 to, Vec3 point, float sideLength) {
        if (max(
                (point.xCoord - from.xCoord - sideLength) / (to.xCoord - from.xCoord),
                (point.yCoord - from.yCoord - sideLength) / (to.yCoord - from.yCoord),
                (point.zCoord - from.zCoord - sideLength) / (to.zCoord - from.zCoord)
        ) <= min(
                (point.xCoord - from.xCoord + sideLength) / (to.xCoord - from.xCoord),
                (point.yCoord - from.yCoord + sideLength) / (to.yCoord - from.yCoord),
                (point.zCoord - from.zCoord + sideLength) / (to.zCoord - from.zCoord)
        )) {
            System.out.println(point);
            return true;
        }
        return false;
    }

    public static double max(double... values) {
        return Arrays.stream(values).max().orElseThrow(NoSuchElementException::new);
    }

    public static double min(double... values) {
        return Arrays.stream(values).min().orElseThrow(NoSuchElementException::new);
    }

    public static ArrayList<BlockPos> getAllTeleportableBlocks(Vec3 vec, float range) {
        long timestamp = System.currentTimeMillis();
        BlockPos origin = new BlockPos(vec);
        Iterable<BlockPos> blocks = BlockPos.getAllInBox(origin.subtract(new Vec3i(range, 16, range)), origin.add(new Vec3i(range, 16, range)));
        ArrayList<BlockPos> validBlocks = (ArrayList<BlockPos>) StreamSupport.stream(
                blocks.spliterator(), false
        ).filter(blockPos ->
                        mc.theWorld.getBlockState(blockPos).getBlock().isCollidable() && mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.carpet &&
                        mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.skull && mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.wall_sign &&
                        mc.theWorld.getBlockState(blockPos).getBlock() != Blocks.standing_sign &&
                        !(mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockFence) &&
                        !(mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockFenceGate) &&
                        mc.theWorld.getBlockState(blockPos).getBlock().getCollisionBoundingBox(mc.theWorld, blockPos, mc.theWorld.getBlockState(blockPos)) != null &&
                        mc.theWorld.getBlockState(blockPos.add(0, 1, 0)).getBlock() == Blocks.air &&
                        mc.theWorld.getBlockState(blockPos.add(0, 2, 0)).getBlock() == Blocks.air &&
                        vec.distanceTo(new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.95, blockPos.getZ() + 0.5)) <= 61 &&
                        canBlockBeeSeenFromVec(vec, blockPos)
        ).collect(Collectors.toList());
        AStarCustomPathfinder.counter += System.currentTimeMillis() - timestamp;
        return validBlocks;
    }

    public static boolean canBlockBeeSeenFromVec(Vec3 from, BlockPos to) {
        return canVecBeSeenFromVec(from, new Vec3(
                to.getX() + 0.5,
                to.getY() + 0.95,
                to.getZ() + 0.5
        ), 0.1f) /*&& canVecBeSeenFromVec(from, new Vec3(
                to.getX() + 0.6,
                to.getY() + 0.95,
                to.getZ() + 0.6
        ), 0.2f)*/;
    }

    public static boolean canVecBeSeenFromVec(Vec3 from, Vec3 to, float step) {
        double dist = from.distanceTo(to);

        ModUtils.sendMessage(from);
        ModUtils.sendMessage(to);

        for (double d = step; d < dist; d += step) {

            Vec3 pos1 = new Vec3(
                    from.xCoord + (to.xCoord - from.xCoord) *  d / dist,
                    from.yCoord + (to.yCoord - from.yCoord) * d / dist,
                    from.zCoord + (to.zCoord - from.zCoord) * d / dist
            );

            if ((int) pos1.xCoord == (int) from.xCoord &&
                    (int) pos1.yCoord == (int) from.yCoord &&
                    (int) pos1.zCoord == (int) from.zCoord) {
                continue;
            }

            if ((int) pos1.xCoord == (int) to.xCoord &&
                    (int) pos1.yCoord == (int) to.yCoord &&
                    (int) pos1.zCoord == (int) to.zCoord) {
                continue;
            }

            PathFinding.points.add(pos1);

            if (mc.theWorld.getBlockState(new BlockPos(pos1)).getBlock() != Blocks.air) {
                return false;
            }
        }

        return true;
    }

    public static RaytraceResult raytraceToBlock(EntityPlayerSP player, float partialTicks, float dist, float step) {
        Vector3f pos = new Vector3f((float) player.posX, (float) player.posY + player.getEyeHeight(), (float) player.posZ);

        Vec3 lookVec3 = player.getLook(partialTicks);

        Vector3f look = new Vector3f((float) lookVec3.xCoord, (float) lookVec3.yCoord, (float) lookVec3.zCoord);
        look.scale(step / look.length());

        int stepCount = (int) Math.ceil(dist / step);

        for (int i = 0; i < stepCount; i++) {
            Vector3f.add(pos, look, pos);

            WorldClient world = mc.theWorld;
            BlockPos position = new BlockPos(pos.x, pos.y, pos.z);
            IBlockState state = world.getBlockState(position);

            if (state.getBlock() != Blocks.air) {
                //Back-step
                Vector3f.sub(pos, look, pos);
                look.scale(0.1f);

                for (int j = 0; j < 10; j++) {
                    Vector3f.add(pos, look, pos);

                    BlockPos position2 = new BlockPos(pos.x, pos.y, pos.z);
                    IBlockState state2 = world.getBlockState(position2);

                    if (state2.getBlock() != Blocks.air) {
                        return new RaytraceResult(position2, state2);
                    }
                }

                return new RaytraceResult(position, state);
            }
        }

        return null;
    }

    public static MovingObjectPosition raytraceToBlock(double reach) {
        Vec3 playerPos = mc.thePlayer.getPositionEyes(1f);
        Vec3 vec3 = mc.thePlayer.getLookVec();
        Vec3 vec32 = vec3.addVector(vec3.xCoord * reach, vec3.yCoord * reach, vec3.zCoord * reach);
        return mc.thePlayer.worldObj.rayTraceBlocks(playerPos, vec32, false, true, false);
    }

    public static MovingObjectPosition raytraceToBlock(float yaw, float pitch, double reach) {
        Vec3 vec3 = mc.thePlayer.getPositionEyes(1f);
        Vec3 vec31 = getVectorForRotation(yaw, pitch);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * reach, vec31.yCoord * reach, vec31.zCoord * reach);
        return mc.thePlayer.worldObj.rayTraceBlocks(vec3, vec32, false, true, false);
    }

    public static MovingObjectPosition raytrace(float yaw, float pitch, double reach) {
        MovingObjectPosition ray = raytraceToBlock(yaw, pitch, reach);
        double d1 = reach;
        Vec3 vec3 = mc.thePlayer.getPositionEyes(1f);
        if (ray != null) d1 = ray.hitVec.distanceTo(vec3);
        Vec3 vec31 = getVectorForRotation(yaw, pitch);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * reach, vec31.yCoord * reach, vec31.zCoord * reach);
        Entity pointedEntity = null;
        Vec3 vec33 = null;
        float f = 1.0F;
        List<Entity> list = mc
                .theWorld
                .getEntitiesInAABBexcluding(
                        mc.thePlayer,
                        mc.thePlayer.getEntityBoundingBox().addCoord(vec31.xCoord * reach, vec31.yCoord * reach, vec31.zCoord * reach).expand(f, f, f),
                        Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith)
                );
        double d2 = d1;

        for (Entity entity : list) {
            float f1 = entity.getCollisionBorderSize();
            AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand(f1, f1, f1);
            MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
            if (axisalignedbb.isVecInside(vec3)) {
                if (d2 >= 0.0) {
                    pointedEntity = entity;
                    vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                    d2 = 0.0;
                }
            } else if (movingobjectposition != null) {
                double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                if (d3 < d2 || d2 == 0.0) {
                    if (entity != entity.ridingEntity || entity.canRiderInteract()) {
                        pointedEntity = entity;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    } else if (d2 == 0.0) {
                        pointedEntity = entity;
                        vec33 = movingobjectposition.hitVec;
                    }
                }
            }
        }

        return pointedEntity != null && (d2 < d1 || ray == null) ? new MovingObjectPosition(pointedEntity, vec33) : ray;
    }

    public static Vec3 getVectorForRotation(float yaw, float pitch) {
        double f = Math.cos(-yaw * (Math.PI / 180.0) - (Math.PI));
        double f1 = Math.sin(-yaw * (Math.PI / 180.0) - Math.PI);
        double f2 = -Math.cos(-pitch * (Math.PI / 180.0));
        double f3 = Math.sin(-pitch * (Math.PI / 180.0));
        return new Vec3(f1 * f2, f3, f * f2);
    }
}
