package samsidere.perylclient.utils;


import net.minecraft.util.ChatComponentText;
import samsidere.PerylClient.PerylClient;

public class ModUtils {
    public static void sendMessage(Object object) {
        String message = "null";
        if (object != null) {
            message = object.toString().replace("&", "§");
        }
        if (PerylClient.mc.thePlayer != null) {
            PerylClient.mc.thePlayer.addChatMessage(new ChatComponentText("§7[§d" + PerylClient.NAME + "§7] §f" + message));
        }
    }
}
