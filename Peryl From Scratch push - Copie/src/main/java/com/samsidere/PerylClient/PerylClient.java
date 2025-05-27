package com.samsidere.PerylClient; // <--- C'est bon

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.common.MinecraftForge;

// IMPORTS MODIFIÉS
import com.samsidere.PerylClient.utils.Jobs.JobController;     // <--- MODIFIÉ
import com.samsidere.PerylClient.utils.Jobs.JobStep;           // <--- MODIFIÉ
import com.samsidere.PerylClient.utils.Jobs.JobAction;         // <--- MODIFIÉ
import com.samsidere.PerylClient.utils.Movements.pathfinder;   // <--- MODIFIÉ (Nom de la classe est 'pathfinder', pas 'PlayerPathFollower')
import com.samsidere.PerylClient.utils.Commands.WaypointsCommands; // <--- MODIFIÉ (Nom de la classe est 'WaypointsCommands')
import com.samsidere.PerylClient.utils.Commands.JobsStart; // <--- MODIFIÉ (Nom de la classe est 'JobsStart')


import net.minecraft.util.BlockPos;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Mod(modid = PerylClient.MODID, name = PerylClient.NAME, version = PerylClient.VERSION, acceptedMinecraftVersions = "[1.8.9]")
public class PerylClient {

    public static final String MODID = "perylclient";
    public static final String NAME = "Peryl Client";
    public static final String VERSION = "1.0";

    private pathfinder pathFollower; // <--- MODIFIÉ (Type)
    private JobController jobController;

    public static PerylClient instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;
        File configDir = event.getModConfigurationDirectory();

        WaypointsCommands.initialize(configDir); // <--- MODIFIÉ (Nom de la classe)
        pathFollower = new pathfinder();         // <--- MODIFIÉ (Nom de la classe)
        WaypointsCommands.setPlayerPathFollower(pathFollower); // <--- MODIFIÉ (Nom de la classe)

        jobController = new JobController(pathFollower);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new WaypointsCommands()); // <--- MODIFIÉ (Nom de la classe)
        event.registerServerCommand(new JobsStart(jobController, this)); // <--- MODIFIÉ (Nom de la classe)
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (pathFollower != null) {
                pathFollower.onClientTick();
            }
            if (jobController != null) {
                jobController.onClientTick();
            }
        }
    }

    public void startRedSandFarmJob() {
        List<JobStep> redSandJobSequence = Arrays.asList(
            new JobStep("farm_start", JobAction.NONE),
            new JobStep("farm_end", JobAction.MINE_WHILE_MOVING),
            new JobStep("check_inventory_zone", JobAction.CHECK_INVENTORY_FULL),
            new JobStep("farm_start", JobAction.CHAT_MESSAGE, "Farm de sable rouge : je reviens !"),
            new JobStep("farm_start", JobAction.WAIT, 2000L),
            new JobStep("home", JobAction.NONE)
        );
        jobController.startJob(redSandJobSequence);
        System.out.println("Red Sand Farm Job Started!");
    }

    public void startInventoryDumpJob() {
        System.out.println("Starting Inventory Dump Job (not yet implemented fully)...");
        List<JobStep> dumpJobSequence = Arrays.asList(
            new JobStep("chest_location", JobAction.NONE),
            new JobStep("chest_location", JobAction.USE_ITEM)
        );
        jobController.startJob(dumpJobSequence);
    }
}