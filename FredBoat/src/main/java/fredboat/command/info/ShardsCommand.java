/*
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
 *
 */

package fredboat.command.info;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.main.BotController;
import fredboat.main.Config;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ShardsCommand extends Command implements IInfoCommand {

    private static final int SHARDS_PER_MESSAGE = 30;

    public ShardsCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        for (Message message : getShardStatus(context.msg)) {
            context.reply(message);
        }
    }

    public static List<Message> getShardStatus(@Nonnull Message input) {
        MessageBuilder mb = null;
        List<Message> messages = new ArrayList<>();

        //do a full report? or just a summary
        boolean full = false;
        String raw = input.getContentRaw().toLowerCase();
        if (raw.contains("full") || raw.contains("all")) {
            full = true;
        }

        List<JDA> shards = BotController.INS.getShardManager().getShards();
        int borkenShards = 0;
        int healthyGuilds = 0;
        int healthyUsers = 0;
        for (JDA shard : shards) {
            if (shard.getStatus() == JDA.Status.CONNECTED && !full) {
                healthyGuilds += shard.getGuildCache().size();
                healthyUsers += shard.getUserCache().size();
            } else {
                if (borkenShards % SHARDS_PER_MESSAGE == 0) {
                    if (mb != null) {
                        mb.append("```");
                        messages.add(mb.build());
                    }
                    mb = CentralMessaging.getClearThreadLocalMessageBuilder().append("```diff\n");
                }
                //noinspection ConstantConditions
                mb.append(shard.getStatus() == JDA.Status.CONNECTED ? "+" : "-")
                        .append(" ")
                        .append(shard.getShardInfo().getShardString())
                        .append(" ")
                        .append(shard.getStatus())
                        .append(" -- Guilds: ")
                        .append(String.format("%04d", shard.getGuildCache().size()))
                        .append(" -- Users: ")
                        .append(shard.getUserCache().size())
                        .append("\n");
                borkenShards++;
            }
        }
        if (mb != null && borkenShards % SHARDS_PER_MESSAGE != 0) {
            mb.append("```");
            messages.add(mb.build());
        }

        //healthy shards summary, contains sensible data only if we aren't doing a full report
        if (!full) {
            String content = String.format("+ %s of %s shards are %s -- Guilds: %s -- Users: %s", (shards.size() - borkenShards),
                    Config.getNumShards(), JDA.Status.CONNECTED, healthyGuilds, healthyUsers);
            messages.add(0, CentralMessaging.getClearThreadLocalMessageBuilder().append(TextUtils.asCodeBlock(content, "diff")).build());
        }

        return messages;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [full]\n#Show information about the shards of the bot as a summary or in a detailed report.";
    }
}
