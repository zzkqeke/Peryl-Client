package com.samsidere.PerylClient.utils.Commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

import com.samsidere.PerylClient.utils.Jobs.JobController;
import com.samsidere.PerylClient.PerylClient;
import java.util.List;

public class JobsStart extends CommandBase {

    private final JobController jobController;
    private final PerylClient perylClient;
    private final Minecraft mc = Minecraft.getMinecraft();

    public JobsStart(JobController jobController, PerylClient perylClient) {
        this.jobController = jobController;
        this.perylClient = perylClient;
    }

    @Override
    public String getCommandName() {
        return "job";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "§cUsage: /job start <jobName> | /job stop";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        System.out.println("DEBUG: Command sender type is: " + sender.getClass().getName());

        final EntityPlayerSP player; // Rendre player effectivement final
        if (sender instanceof EntityPlayerSP) {
            player = (EntityPlayerSP) sender;
        } else if (mc.thePlayer != null) {
            player = mc.thePlayer;
        } else {
            player = null; // Assurez-vous qu'elle est initialisée si aucune condition n'est remplie
        }

        if (player == null) {
            sender.addChatMessage(new ChatComponentText("§cThis command can only be run by a player, or the player instance is not available."));
            return;
        }

        if (args.length >= 1) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("start")) {
                if (args.length >= 2) {
                    String jobName = args[1].toLowerCase();
                    switch (jobName) {
                        case "redsandfarm":
                            player.addChatMessage(new ChatComponentText("§aStarting Red Sand Farm job..."));
                            perylClient.startRedSandFarmJob();
                            break;
                        case "inventorydump":
                            player.addChatMessage(new ChatComponentText("§aStarting Inventory Dump job..."));
                            perylClient.startInventoryDumpJob();
                            break;
                        default:
                            player.addChatMessage(new ChatComponentText("§cUnknown job: " + jobName + ". Available: redsandfarm, inventorydump."));
                            break;
                    }
                } else {
                    player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
                }
            } else if (subCommand.equals("stop")) {
                jobController.stopJob();
                player.addChatMessage(new ChatComponentText("§aJob stopped."));
            } else {
                player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            }
        } else {
            player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "start", "stop");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return getListOfStringsMatchingLastWord(args, "redsandfarm", "inventorydump");
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}