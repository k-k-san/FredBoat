package fredboat.command.moderation;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;

import javax.annotation.Nonnull;

public class EnableCommandsCommand extends Command implements ICommandRestricted {

    public EnableCommandsCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (context.hasArguments()) {
            Command command = CommandRegistry.findCommand(context.args[0]);
            if (command == null) {
                context.reply("This command doesn't exist!");
                return;
            }

            if (CommandManager.disabledCommands.contains(command)) {
                CommandManager.disabledCommands.remove(command);
                context.reply(":ok_hand: Command `" + command.name + "` enabled!");
                return;
            }
            context.reply("This command is not disabled!");
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <command>\n#Re-enable a globally disabled command";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
