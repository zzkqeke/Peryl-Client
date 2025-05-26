package samsidere.perylclient.utils;


import net.minecraft.client.network.NetworkPlayerInfo;
import samsidere.PerylClient.PerylClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TabListUtils {

    public static boolean tabListContains(String string) {
        return tabListContains(string, getTabList());
    }

    public static boolean tabListContains(String string, List<String> tabList) {
        return tabList.stream().map(line -> StringUtils.removeFormatting(cleanSB(line))).anyMatch(line -> line.contains(string));
    }

    public static List<String> getTabList() {
        if (PerylClient.mc.thePlayer != null) {
            return PerylClient.mc.thePlayer.sendQueue.getPlayerInfoMap().stream()
                    .map(PerylClient.mc.ingameGUI.getTabList()::getPlayerName)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public static String cleanSB(String scoreboard) {
        char[] nvString = StringUtils.removeFormatting(scoreboard).toCharArray();
        StringBuilder cleaned = new StringBuilder();

        for (char c : nvString) {
            if ((int) c > 20 && (int) c < 127) {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }
}
