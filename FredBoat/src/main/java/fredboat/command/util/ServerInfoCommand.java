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

package fredboat.command.util;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;

import javax.annotation.Nonnull;
import java.time.format.DateTimeFormatter;

/**
 * Created by midgard/Chromaryu/knight-ryu12 on 17/01/18.
 */
public class ServerInfoCommand extends Command implements IUtilCommand {

    public ServerInfoCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        Guild guild = context.guild;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        EmbedBuilder eb = CentralMessaging.getColoredEmbedBuilder();
        eb.setTitle(context.i18nFormat("serverinfoTitle", guild.getName()), null);
        eb.setThumbnail(guild.getIconUrl());

        long onlineMembers = guild.getMemberCache().stream()
                .filter(m -> m.getOnlineStatus() != OnlineStatus.OFFLINE)
                .count();

        eb.addField(context.i18n("serverinfoOnlineUsers"), String.valueOf(onlineMembers), true);
        eb.addField(context.i18n("serverinfoTotalUsers"), String.valueOf(guild.getMembers().size()), true);
        eb.addField(context.i18n("serverinfoRoles"), String.valueOf(guild.getRoles().size()), true);
        eb.addField(context.i18n("serverinfoText"), String.valueOf(guild.getTextChannels().size()), true);
        eb.addField(context.i18n("serverinfoVoice"), String.valueOf(guild.getVoiceChannels().size()), true);
        eb.addField(context.i18n("serverinfoCreationDate"), guild.getCreationTime().format(dtf), true);
        eb.addField(context.i18n("serverinfoGuildID"), guild.getId(), true);
        eb.addField(context.i18n("serverinfoVLv"), guild.getVerificationLevel().name(), true);
        eb.addField(context.i18n("serverinfoOwner"), guild.getOwner().getAsMention(), true);
        String prefix = TextUtils.shorten(TextUtils.escapeMarkdown(context.getPrefix()), MessageEmbed.VALUE_MAX_LENGTH);
        if (prefix.isEmpty()) {
            prefix = "No Prefix";
        }
        eb.addField(context.i18n("prefix"), prefix, true);

        context.reply(eb.build());
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpServerInfoCommand");
    }
}
