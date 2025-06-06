package samsidere.perylclient.hud;

import cc.polyfrost.oneconfig.hud.TextHud;
import samsidere.perylclient.GumTuneClient;
import samsidere.perylclient.config.GumTuneClientConfig;
import samsidere.perylclient.modules.qol.Trackers;
import samsidere.perylclient.utils.LocationUtils;
import samsidere.perylclient.utils.StringUtils;

import java.util.List;

public class TrackersHud extends TextHud {
    private final String[] powderStats = { "Powder Chests", "Gemstone Powder", "Mithril Powder" };

    public TrackersHud() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (GumTuneClient.mc.thePlayer == null) return;
        if (!GumTuneClientConfig.trackers) return;
        if (GumTuneClientConfig.powderChestTracker && LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS) {
            long uptime = Trackers.getUptime();

            for (String powderStat : powderStats) {
                lines.add(powderStat + ": " + (Trackers.powderChestStats.containsKey(powderStat) ? Trackers.powderChestStats.get(powderStat) : "0"));
                lines.add(powderStat + " / Hour: " + ((Trackers.powderChestStats.containsKey(powderStat) && uptime > 0) ? Math.round((float) Trackers.powderChestStats.get(powderStat) / uptime * 1000 * 3600) : "0"));
            }

            if (uptime > 0) {
                lines.add("Uptime: " + StringUtils.millisecondFormatTime(uptime));
            }
        }
    }
}
