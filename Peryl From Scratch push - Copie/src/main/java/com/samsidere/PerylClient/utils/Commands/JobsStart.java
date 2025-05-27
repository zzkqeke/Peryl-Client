package com.samsidere.PerylClient.utils.Commands; // <--- MODIFIÉ

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.client.Minecraft;

import com.samsidere.PerylClient.PerylClient; // <--- MODIFIÉ (Nom de la classe principale)
import com.samsidere.PerylClient.utils.Jobs.JobController; // <--- MODIFIÉ

import java.util.Arrays;
import java.util.List;

public class JobsStart extends CommandBase { // <--- MODIFIÉ (Nom de la classe)

    private final JobController jobController;
    private final PerylClient mainModClass; // <--- MODIFIÉ (Type de l'instance)

    public JobsStart(JobController jobController, PerylClient mainModClass) { // <--- MODIFIÉ
        this.jobController = jobController;
        this.mainModClass = mainModClass;
    }

    @Override
    public String getCommandName() {
        return "job";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/job <start <jobName>/stop/status>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;

            if (args.length == 0) {
                player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
                return;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "start":
                    if (args.length == 2) {
                        String jobName = args[1].toLowerCase();
                        switch (jobName) {
                            case "redsandfarm":
                                mainModClass.startRedSandFarmJob();
                                player.addChatMessage(new ChatComponentText("Starting Red Sand Farm Job."));
                                break;
                            case "inventorydump":
                                mainModClass.startInventoryDumpJob();
                                player.addChatMessage(new ChatComponentText("Starting Inventory Dump Job."));
                                break;
                            default:
                                player.addChatMessage(new ChatComponentText("Unknown job: " + jobName));
                                break;
                        }
                    } else {
                        player.addChatMessage(new ChatComponentText("Usage: /job start <jobName>"));
                    }
                    break;
                case "stop":
                    jobController.stopJob();
                    player.addChatMessage(new ChatComponentText("Job stopped."));
                    break;
                case "status":
                    if (jobController.isJobActive()) {
                        player.addChatMessage(new ChatComponentText("Job Status: Active"));
                    } else {
                        player.addChatMessage(new ChatComponentText("Job Status: Inactive"));
                    }
                    break;
                default:
                    player.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
                    break;
            }
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true; // Everyone can use this command for now
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "start", "stop", "status");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return getListOfStringsMatchingLastWord(args, "redsandfarm", "inventorydump");
        }
        return null;
    }
}