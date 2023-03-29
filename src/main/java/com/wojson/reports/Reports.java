package com.wojson.reports;

import com.wojson.reports.command.InvalidCommandUsage;
import com.wojson.reports.configuration.ConfigurationManager;
import com.wojson.reports.configuration.implementation.PluginConfiguration;
import com.wojson.reports.legacy.LegacyColorProcessor;
import com.wojson.reports.notification.NotificationAnnouncer;
import com.wojson.reports.report.ReportAnnouncer;
import com.wojson.reports.report.command.ReportCommand;
import com.wojson.reports.report.discord.DiscordReportWebHook;
import com.wojson.reports.report.statistics.StatisticsConfiguration;
import com.wojson.reports.report.statistics.StatisticsService;
import com.google.common.base.Stopwatch;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.adventure.platform.LiteBukkitAdventurePlatformFactory;
import dev.rollczi.litecommands.bukkit.tools.BukkitOnlyPlayerContextual;
import dev.rollczi.litecommands.bukkit.tools.BukkitPlayerArgument;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class Reports extends JavaPlugin {

    private AudienceProvider audienceProvider;
    private MiniMessage miniMessage;

    private NotificationAnnouncer notificationAnnouncer;
    private StatisticsService statisticsService;
    private ReportAnnouncer reportAnnouncer;
    private DiscordReportWebHook discordReportWebHook;

    private ConfigurationManager configurationManager;
    private PluginConfiguration pluginConfiguration;
    private StatisticsConfiguration statisticsConfiguration;

    private LiteCommands<CommandSender> liteCommands;

    @Override
    public void onEnable() {
        Stopwatch started = Stopwatch.createStarted();
        Server server = this.getServer();

        this.configurationManager = new ConfigurationManager(this.getDataFolder());
        this.pluginConfiguration = this.configurationManager.load(new PluginConfiguration());
        this.statisticsConfiguration = this.configurationManager.load(new StatisticsConfiguration());

        this.audienceProvider = BukkitAudiences.create(this);
        this.miniMessage = MiniMessage.builder()
                .postProcessor(new LegacyColorProcessor())
                .tags(TagResolver.standard())
                .build();

        this.notificationAnnouncer = new NotificationAnnouncer(this.audienceProvider, this.miniMessage);
        this.statisticsService = new StatisticsService(this.configurationManager, this.statisticsConfiguration);
        this.discordReportWebHook = new DiscordReportWebHook(this.pluginConfiguration);
        this.reportAnnouncer = new ReportAnnouncer(this.notificationAnnouncer, this.pluginConfiguration, this.discordReportWebHook, this.statisticsService, server);

        this.liteCommands = LiteBukkitAdventurePlatformFactory.builder(this.getServer(), "reports", this.audienceProvider, this.miniMessage)
                .argument(Player.class, new BukkitPlayerArgument<>(this.getServer(), this.miniMessage.deserialize(this.pluginConfiguration.userMessages.userNotFound)))
                .contextualBind(Player.class, new BukkitOnlyPlayerContextual<>(this.miniMessage.deserialize(this.pluginConfiguration.userMessages.onlyUserCommand)))
                .invalidUsageHandler(new InvalidCommandUsage(this.notificationAnnouncer, this.pluginConfiguration))

                .commandInstance(new ReportCommand(this.pluginConfiguration, this.notificationAnnouncer, this.configurationManager, this.reportAnnouncer))
                .register();

        Metrics metrics = new Metrics(this, 16483);
        metrics.addCustomChart(new SingleLineChart("users_reported_globally", () -> this.statisticsConfiguration.reports));

        long millis = started.elapsed(TimeUnit.MILLISECONDS);
        this.getLogger().info("Reports loaded in " + millis + "ms");
    }

    @Override
    public void onDisable() {
        if (this.liteCommands.getPlatform() != null) {
            this.liteCommands.getPlatform().unregisterAll();
        }

        if (this.audienceProvider != null) {
            this.audienceProvider.close();
        }
    }
}
