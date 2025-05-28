package com.samsidere.PerylClient;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge; // Ajoute cette importation
import samsidere.package.mod.features.SandMiner; // Ajoute cette importation

@Mod(modid = MonMod.MODID, version = MonMod.VERSION, name = MonMod.NAME)
public class MonMod {

    public static final String MODID = "perylclient";
    public static final String VERSION = "1.0";
    public static final String NAME = "Peryl";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new SandMiner()); // Enregistre ton écouteur
        // Tu peux aussi enregistrer une commande ici pour toggleMining()
    }
}