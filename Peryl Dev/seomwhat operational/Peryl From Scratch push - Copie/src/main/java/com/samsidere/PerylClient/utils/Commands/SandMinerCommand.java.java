// src/main/java/ton/package/mod/commands/SandMinerCommand.java

package samsidere.PerylClient.utils.Commands

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.client.Minecraft; // Nécessaire pour les messages chat
import ton.package.mod.features.SandMiner; // Assure-toi que ce chemin est correct

public class SandMinerCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "sandminer"; // La commande sera /sandminer
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/sandminer toggle - active ou désactive le bot de minage de sable.";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            SandMiner.toggleMining();
        } else {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        // Permet à n'importe quel joueur d'utiliser la commande
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "toggle");
        }
        return null;
    }
}