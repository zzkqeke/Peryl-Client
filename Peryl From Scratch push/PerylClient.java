// Dans ta classe principale de mod Forge (par exemple, MyMod.java)
package com.yourmodid.yourmod; // Remplace par ton package

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent; // Pour les événements côté client si besoin
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// L'annotation @Mod doit correspondre à ton modid dans mods.toml
@Mod("yourmodid") // Remplace "yourmodid" par l'ID de ton mod
public class MyMod {

    // Crée une instance de ton path follower si tu en as besoin globalement
    public static PlayerPathFollower PLAYER_PATH_FOLLOWER; // Rendre public static pour un accès facile

    public MyMod() {
        // Enregistre les méthodes d'initialisation pour le mod
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup); // Si tu as des événements client
        
        // Enregistre l'écouteur d'événements pour les commandes (sur le bus d'événements de MinecraftForge)
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Initialise le système de waypoints ici, après la configuration commune du mod
        WaypointCommands.initialize();
        System.out.println("WaypointCommands initialized for Forge!");

        // Initialise le PlayerPathFollower
        PLAYER_PATH_FOLLOWER = new PlayerPathFollower();
        // Optionnel: Passer l'instance au système de commande si nécessaire pour le 'goto'
        // WaypointCommands.setPathFollower(PLAYER_PATH_FOLLOWER); // Si tu as ajouté cette méthode
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Enregistre l'événement de tick côté client pour le PlayerPathFollower
        // Le tick côté client est crucial pour les actions de mouvement
        // Dans Forge, tu peux utiliser TickEvent.ClientTickEvent
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        // Enregistre tes commandes via la méthode de WaypointCommands
        WaypointCommands.register(event.getDispatcher());
        System.out.println("Waypoint commands registered for Forge!");
    }

    // Gère le tick client pour le PlayerPathFollower
    @SubscribeEvent
    public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            if (PLAYER_PATH_FOLLOWER != null) {
                PLAYER_PATH_FOLLOWER.onClientTick();
            }
        }
    }
}