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

package fredboat.command.music.control;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerLimitManager;
import fredboat.audio.player.VideoSelectionCache;
import fredboat.audio.player.VideoSelectionCache.VideoSelection;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.definitions.PermissionLevel;
import fredboat.definitions.SearchProvider;
import fredboat.main.Launcher;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.shared.constant.BotConstants;
import fredboat.util.TextUtils;
import fredboat.util.rest.TrackSearcher;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message.Attachment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class PlayCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PlayCommand.class);
    private final TrackSearcher trackSearcher;
    private final VideoSelectionCache videoSelectionCache;
    private final List<SearchProvider> searchProviders;
    private static final JoinCommand JOIN_COMMAND = new JoinCommand("");
    private static final String FILE_PREFIX = "file://";

    public PlayCommand(TrackSearcher trackSearcher, VideoSelectionCache videoSelectionCache,
                       List<SearchProvider> searchProviders, String name, String... aliases) {
        super(name, aliases);
        this.trackSearcher = trackSearcher;
        this.videoSelectionCache = videoSelectionCache;
        this.searchProviders = searchProviders;
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.invoker.getVoiceState().inVoiceChannel()) {
            context.reply(context.i18n("playerUserNotInChannel"));
            return;
        }

        if (!PlayerLimitManager.checkLimitResponsive(context, Launcher.getBotController().getPlayerRegistry())) return;

        if (!context.msg.getAttachments().isEmpty()) {
            GuildPlayer player = Launcher.getBotController().getPlayerRegistry().getOrCreate(context.guild);

            for (Attachment atc : context.msg.getAttachments()) {
                player.queue(atc.getUrl(), context);
            }
            
            player.setPause(false);
            
            return;
        }

        if (!context.hasArguments()) {
            GuildPlayer player = Launcher.getBotController().getPlayerRegistry().getExisting(context.guild);
            handleNoArguments(context, player);
            return;
        }

        if (TextUtils.isSplitSelect(context.rawArgs)) {
            SelectCommand.select(context, videoSelectionCache);
            return;
        }

        String url = StringUtils.strip(context.args[0], "<>");
        //Search youtube for videos and let the user select a video
        if (!url.startsWith("http") && !url.startsWith(FILE_PREFIX)) {
            searchForVideos(context);
            return;
        }
        if (url.startsWith(FILE_PREFIX)) {
            url = url.replaceFirst(FILE_PREFIX, ""); //LocalAudioSourceManager does not manage this itself
        }

        GuildPlayer player = Launcher.getBotController().getPlayerRegistry().getOrCreate(context.guild);
        player.queue(url, context);
        player.setPause(false);

        context.deleteMessage();
    }

    private void handleNoArguments(CommandContext context, @Nullable GuildPlayer player) {
        if (player == null || player.isQueueEmpty()) {
            context.reply(context.i18n("playQueueEmpty"));
        } else if (player.isPlaying()) {
            context.reply(context.i18n("playAlreadyPlaying"));
        } else if (player.getHumanUsersInCurrentVC().isEmpty() && context.guild.getSelfMember().getVoiceState().getChannel() != null) {
            context.reply(context.i18n("playVCEmpty"));
        } else if (context.guild.getSelfMember().getVoiceState().getChannel() == null) {
            // When we just want to continue playing, but the user is not in a VC
            JOIN_COMMAND.onInvoke(context);
            if (context.guild.getSelfMember().getVoiceState().getChannel() != null) {
                player.play();
                context.reply(context.i18n("playWillNowPlay"));
            }
        } else {
            player.play();
            context.reply(context.i18n("playWillNowPlay"));
        }
    }

    private void searchForVideos(CommandContext context) {
        //Now remove all punctuation
        String query = context.rawArgs.replaceAll(TrackSearcher.PUNCTUATION_REGEX, "");

        context.reply(context.i18n("playSearching").replace("{q}", query), outMsg -> {
            AudioPlaylist list;
            try {
                list = trackSearcher.searchForTracks(query, searchProviders);
            } catch (TrackSearcher.SearchingException e) {
                context.reply(context.i18n("playYoutubeSearchError"));
                log.error("YouTube search exception", e);
                return;
            }

            if (list == null || list.getTracks().isEmpty()) {
                CentralMessaging.editMessage(outMsg,
                        context.i18n("playSearchNoResults").replace("{q}", query)
                );

            } else {
                //Get at most 5 tracks
                List<AudioTrack> selectable = list.getTracks().subList(0, Math.min(TrackSearcher.MAX_RESULTS, list.getTracks().size()));

                VideoSelection oldSelection = videoSelectionCache.remove(context.invoker);
                if(oldSelection != null) {
                    oldSelection.deleteMessage();
                }

                MessageBuilder builder = CentralMessaging.getClearThreadLocalMessageBuilder();
                builder.append(context.i18nFormat("playSelectVideo", TextUtils.escapeMarkdown(context.getPrefix())));

                int i = 1;
                for (AudioTrack track : selectable) {
                    builder.append("\n**")
                            .append(String.valueOf(i))
                            .append(":** ")
                            .append(TextUtils.escapeAndDefuse(track.getInfo().title))
                            .append(" (")
                            .append(TextUtils.formatTime(track.getInfo().length))
                            .append(")");

                    i++;
                }

                CentralMessaging.editMessage(outMsg, builder.build());
                videoSelectionCache.put(context.invoker, selectable, outMsg);
            }
        });
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} <url> OR {0}{1} <search-term>\n#";
        return usage + context.i18nFormat("helpPlayCommand", BotConstants.DOCS_URL);
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
