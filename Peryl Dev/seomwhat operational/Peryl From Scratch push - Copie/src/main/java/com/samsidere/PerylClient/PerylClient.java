// src/main/java/com/samsidere/PerylClient/MonMod.java

package com.samsidere.PerylClient; // Corrected package declaration

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent; // For command registration
import net.minecraftforge.common.MinecraftForge;

import com.samsidere.PerylClient.features.SandMiner; // Corrected import for SandMiner
import com.samsidere.PerylClient.commands.SandMinerCommand; // Import your command class

@Mod(modid = MonMod.MODID, version = MonMod.VERSION, name = MonMod.NAME, acceptableRemoteVersions = "*")
public class MonMod {

    public static final String MODID = "perylclient";
    public static final String VERSION = "1.0";
    public static final String NAME = "Peryl";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new SandMiner()); // Register your event listener
        System.out.println(NAME + ": SandMiner enregistré."); // Simple log for confirmation
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // Register the command when the server (integrated client) starts
        event.registerServerCommand(new SandMinerCommand());
        System.out.println(NAME + ": Commande /sandminer enregistrée."); // Simple log for confirmation
    }
}