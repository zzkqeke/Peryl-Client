package samsidere.perylclient.utils;


import cc.polyfrost.oneconfig.utils.Multithreading;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import samsidere.PerylClient.PerylClient;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PlayerUtils {
    private static final Random random = new Random();
    public static boolean pickaxeAbilityReady = false;

    @SubscribeEvent(receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        String message = StringUtils.removeFormatting(event.message.getUnformattedText());
        if (message.contains(":") || message.contains(">")) return;
        if (message.startsWith("You used your Mining Speed Boost Pickaxe Ability!")) {
            pickaxeAbilityReady = false;
        } else if (message.equals("Mining Speed Boost is now available!")) {
            Multithreading.schedule(() -> pickaxeAbilityReady = true, random.nextInt(500) + 500, TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        pickaxeAbilityReady = false;
    }

    public static void swingHand(MovingObjectPosition objectMouseOver) {
        if (objectMouseOver == null) {
            objectMouseOver = PerylClient.mc.objectMouseOver;
        }
        if (objectMouseOver != null && objectMouseOver.entityHit == null) {
            PerylClient.mc.thePlayer.swingItem();
        }
    }

    public static void rightClick() {
        if (!ReflectionUtils.invoke(PerylClient.mc, "func_147121_ag")) {
            ReflectionUtils.invoke(PerylClient.mc, "rightClickMouse");
        }
    }

    public static void leftClick() {
        if (!ReflectionUtils.invoke(PerylClient.mc, "func_147116_af")) {
            ReflectionUtils.invoke(PerylClient.mc, "clickMouse");
        }
    }

    public static void middleClick() {
        if (!ReflectionUtils.invoke(PerylClient.mc, "func_147112_ai")) {
            ReflectionUtils.invoke(PerylClient.mc, "middleClickMouse");
        }
    }
}
