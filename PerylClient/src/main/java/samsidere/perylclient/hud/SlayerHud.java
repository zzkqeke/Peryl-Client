package samsidere.perylclient.hud;

import cc.polyfrost.oneconfig.hud.TextHud;
import samsidere.perylclient.GumTuneClient;
import samsidere.perylclient.modules.slayer.SlayerHandler;

import java.util.List;

public class SlayerHud extends TextHud {

    public SlayerHud() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (SlayerHandler.currentSlayerBossBar != null && GumTuneClient.mc.theWorld.loadedEntityList.contains(SlayerHandler.currentSlayerBossBar) && SlayerHandler.currentSlayerBossBar.hasCustomName()) {
            lines.add(SlayerHandler.currentSlayerBossBar.getCustomNameTag());
        }
    }
}
