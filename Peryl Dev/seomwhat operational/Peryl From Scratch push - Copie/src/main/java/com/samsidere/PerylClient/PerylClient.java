package com.samsidere.PerylClient;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = MonMod.MODID, version = MonMod.VERSION, name = MonMod.NAME)
public class MonMod {

    public static final String MODID = "perylclient";
    public static final String VERSION = "1.0";
    public static final String NAME = "PerylBot";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        //function iniatilize
    }
}