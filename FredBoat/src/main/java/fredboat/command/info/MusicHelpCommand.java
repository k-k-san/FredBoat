/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.command.info;

import fredboat.command.music.control.*;
import fredboat.command.music.info.*;
import fredboat.command.music.seeking.ForwardCommand;
import fredboat.command.music.seeking.RestartCommand;
import fredboat.command.music.seeking.RewindCommand;
import fredboat.command.music.seeking.SeekCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.messaging.internal.Context;
import fredboat.util.Emojis;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MusicHelpCommand extends Command implements IInfoCommand {

    public MusicHelpCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        invoke(context);
    }

    public static void invoke(@Nonnull CommandContext context) {
        final List<String> musicComms = getSortedMusicComms(context);

        StringBuilder out = new StringBuilder("< " + context.i18n("helpMusicCommandsHeader") + " >\n");
        for (String s : musicComms) {
            if (out.length() + s.length() >= 1990) {
                context.replyPrivate(TextUtils.asCodeBlock(out.toString(), "md"), null, null);
                out = new StringBuilder();
            }
            out.append(s).append("\n");
        }
        context.replyPrivate(TextUtils.asCodeBlock(out.toString(), "md"),
                success -> context.replyWithName(context.i18n("helpSent")),
                failure -> {
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(Emojis.EXCLAMATION + context.i18n("helpDmFailed"));
                    }
                }
        );
    }

    private static List<String> getSortedMusicComms(Context context) {
        List<Command> musicCommands = CommandRegistry.getCommandModule(CommandRegistry.Module.MUSIC).getDeduplicatedCommands();

        //dont explicitly show the youtube and soundcloud commands in this list, since they are just castrated versions
        // of the play command, which is "good enough" for this list
        musicCommands = musicCommands.stream()
                .filter(command -> !(command instanceof PlayCommand
                        && (command.name.equals(CommandInitializer.YOUTUBE_COMM_NAME)
                        || command.name.equals(CommandInitializer.SOUNDCLOUD_COMM_NAME))))
                .collect(Collectors.toList());

        musicCommands.sort(new MusicCommandsComparator());

        List<String> musicComms = new ArrayList<>();
        for (Command command : musicCommands) {
            String formattedHelp = HelpCommand.getFormattedCommandHelp(context, command, command.name);
            musicComms.add(formattedHelp);
        }

        return musicComms;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpMusicHelpCommand");
    }

    /**
     * Sort the commands in a sensible way to display them to the user
     */
    static class MusicCommandsComparator implements Comparator<Command> {

        @Override
        public int compare(Command o1, Command o2) {
            return getCommandRank(o1) - getCommandRank(o2);
        }

        /**
         * a container of smelly code
         * http://stackoverflow.com/a/2790215
         */
        private static int getCommandRank(Command c) {

            int result;

            if (c instanceof PlayCommand) {
                result = 10050;
            } else if (c instanceof ListCommand) {
                result = 10100;
            } else if (c instanceof NowplayingCommand) {
                result = 10150;
            } else if (c instanceof SkipCommand) {
                result = 10200;
            } else if (c instanceof VoteSkipCommand) {
                result = 10225;
            } else if (c instanceof StopCommand) {
                result = 10250;
            } else if (c instanceof PauseCommand) {
                result = 10300;
            } else if (c instanceof UnpauseCommand) {
                result = 10350;
            } else if (c instanceof JoinCommand) {
                result = 10400;
            } else if (c instanceof LeaveCommand) {
                result = 10450;
            } else if (c instanceof RepeatCommand) {
                result = 10500;
            } else if (c instanceof ShuffleCommand) {
                result = 10550;
            } else if (c instanceof ReshuffleCommand) {
                result = 10560;
            } else if (c instanceof ForwardCommand) {
                result = 10600;
            } else if (c instanceof RewindCommand) {
                result = 10650;
            } else if (c instanceof SeekCommand) {
                result = 10700;
            } else if (c instanceof RestartCommand) {
                result = 10750;
            } else if (c instanceof HistoryCommand) {
                result = 10775;
            } else if (c instanceof ExportCommand) {
                result = 10800;
            } else if (c instanceof PlaySplitCommand) {
                result = 10850;
            } else if (c instanceof SelectCommand) {
                result = 10900;
            } else if (c instanceof GensokyoRadioCommand) {
                result = 10950;
            } else if (c instanceof VolumeCommand) {
                result = 10970;
            } else if (c instanceof DestroyCommand) {
                result = 10985;
            } else {
                //everything else
                //newly added commands will land here, just add them to the giant if construct above to assign them a fixed place
                result = 10999;
            }
            return result;
        }
    }
}
