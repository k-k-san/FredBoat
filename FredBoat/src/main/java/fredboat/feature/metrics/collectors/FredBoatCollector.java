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

package fredboat.feature.metrics.collectors;

import fredboat.audio.player.PlayerRegistry;
import fredboat.main.BotController;
import fredboat.main.BotMetrics;
import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import net.dv8tion.jda.core.JDA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by napster on 19.10.17.
 * <p>
 * Collects various FredBoat stats for prometheus
 */
public class FredBoatCollector extends Collector {

    @Override
    public List<MetricFamilySamples> collect() {

        List<MetricFamilySamples> mfs = new ArrayList<>();
        List<String> labelNames = Arrays.asList("shard", "entity");

        GaugeMetricFamily jdaEntities = new GaugeMetricFamily("fredboat_jda_entities",
                "Amount of JDA entities", labelNames);
        mfs.add(jdaEntities);

        GaugeMetricFamily playersPlaying = new GaugeMetricFamily("fredboat_playing_music_players",
                "Currently playing music players", labelNames);
        mfs.add(playersPlaying);

        CounterMetricFamily dockerPulls = new CounterMetricFamily("fredboat_docker_pulls",
                "Total fredboat docker image pulls as reported by the docker hub.", labelNames);
        mfs.add(dockerPulls);

        //global stats
        jdaEntities.addMetric(Arrays.asList("total", "User"), BotMetrics.getTotalUniqueUsersCount());
        jdaEntities.addMetric(Arrays.asList("total", "Guild"), BotMetrics.getTotalGuildsCount());
        jdaEntities.addMetric(Arrays.asList("total", "TextChannel"), BotMetrics.getTotalTextChannelsCount());
        jdaEntities.addMetric(Arrays.asList("total", "VoiceChannel"), BotMetrics.getTotalVoiceChannelsCount());
        jdaEntities.addMetric(Arrays.asList("total", "Category"), BotMetrics.getTotalCategoriesCount());
        jdaEntities.addMetric(Arrays.asList("total", "Emote"), BotMetrics.getTotalEmotesCount());
        jdaEntities.addMetric(Arrays.asList("total", "Role"), BotMetrics.getTotalRolesCount());
        playersPlaying.addMetric(Arrays.asList("total", "Players"), PlayerRegistry.playingCount());

        //docker stats
        int dockerPullsBotCount = BotMetrics.getDockerPullsBot();
        if (dockerPullsBotCount > 0) {
            dockerPulls.addMetric(Arrays.asList("total", "Bot"), dockerPullsBotCount);
        }
        int dockerPullsDbCount = BotMetrics.getDockerPullsDb();
        if (dockerPullsDbCount > 0) {
            dockerPulls.addMetric(Arrays.asList("total", "Db"), dockerPullsDbCount);
        }


        //per shard stats
        if(BotController.INS.getShardManager() == null) {
            return mfs; // This collector is invoked when we begin building the shard manager
        }
        for (JDA shard : BotController.INS.getShardManager().getShards()) {
            String shardId = Integer.toString(shard.getShardInfo().getShardId());
            jdaEntities.addMetric(Arrays.asList(shardId, "User"), shard.getUserCache().size());
            jdaEntities.addMetric(Arrays.asList(shardId, "Guild"), shard.getGuildCache().size());
            jdaEntities.addMetric(Arrays.asList(shardId, "TextChannel"), shard.getTextChannelCache().size());
            jdaEntities.addMetric(Arrays.asList(shardId, "VoiceChannel"), shard.getVoiceChannelCache().size());
            jdaEntities.addMetric(Arrays.asList(shardId, "Category"), shard.getCategoryCache().size());
            jdaEntities.addMetric(Arrays.asList(shardId, "Emote"), shard.getEmoteCache().size());
            jdaEntities.addMetric(Arrays.asList(shardId, "Role"), shard.getRoleCache().size());
        }

        return mfs;
    }
}
