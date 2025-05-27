package com.samsidere.PerylClient;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.common.MinecraftForge;

import com.samsidere.PerylClient.utils.Jobs.JobController;
import com.samsidere.PerylClient.utils.Jobs.JobStep;
import com.samsidere.PerylClient.utils.Jobs.JobAction;
import com.samsidere.PerylClient.utils.Movements.pathfinder;
import com.samsidere.PerylClient.utils.Commands.WaypointsCommands;
import com.samsidere.PerylClient.utils.Commands.JobsStart;

import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Mod(modid = PerylClient.MODID, name = PerylClient.NAME, version = PerylClient.VERSION, acceptedMinecraftVersions = "[1.8.9]")
public class PerylClient {

    public static final String MODID = "perylclient";
    public static final String NAME = "Peryl Client";
    public static final String VERSION = "1.0";

    private pathfinder pathFollower;
    private JobController jobController;

    public static PerylClient instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;
        File configDir = event.getModConfigurationDirectory();

        WaypointsCommands.initialize(configDir);
        pathFollower = new pathfinder();
        WaypointsCommands.setPlayerPathFollower(pathFollower);
        MinecraftForge.EVENT_BUS.register(pathFollower);

        jobController = new JobController(pathFollower);
        MinecraftForge.EVENT_BUS.register(jobController);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new WaypointsCommands());
        event.registerServerCommand(new JobsStart(jobController, this));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Pas de logique spécifique ici pour l'instant
        }
    }

    public void startRedSandFarmJob() {
        System.out.println("PerylClient: Starting Red Sand Farm Job.");
        List<JobStep> redSandFarmSteps = Arrays.asList(
            new JobStep("spawn_farm", JobAction.MOVE_TO_WAYPOINT),
            new JobStep(JobAction.WAIT, 100L),
            new JobStep(JobAction.MINE_WHILE_MOVING)
        );
        jobController.startJob(redSandFarmSteps);
    }

    public void startInventoryDumpJob() {
        System.out.println("PerylClient: Starting Inventory Dump Job.");
        List<JobStep> inventoryDumpSteps = Arrays.asList(
            new JobStep("storage_chest", JobAction.MOVE_TO_WAYPOINT),
            new JobStep(JobAction.USE_ITEM, "chest_interaction"),
            new JobStep(JobAction.DUMP_INVENTORY)
        );
        jobController.startJob(inventoryDumpSteps);
    }
}