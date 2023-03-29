package com.wojson.reports.report;

import com.wojson.reports.configuration.implementation.PluginConfiguration;
import com.wojson.reports.notification.NotificationAnnouncer;
import com.wojson.reports.report.discord.DiscordReportWebHook;
import com.wojson.reports.report.statistics.StatisticsService;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import panda.utilities.text.Formatter;

public class ReportAnnouncer {
    private final NotificationAnnouncer notificationAnnouncer;
    private final PluginConfiguration pluginConfiguration;
    private final DiscordReportWebHook discordReportWebHook;
    private final StatisticsService statisticsService;
    private final Server server;

    public ReportAnnouncer(
        NotificationAnnouncer notificationAnnouncer,
        PluginConfiguration pluginConfiguration,
        DiscordReportWebHook discordReportWebHook,
        StatisticsService statisticsService,
        Server server
    ) {
        this.notificationAnnouncer = notificationAnnouncer;
        this.pluginConfiguration = pluginConfiguration;
        this.discordReportWebHook = discordReportWebHook;
        this.statisticsService = statisticsService;
        this.server = server;
    }

    public void sendReport(Player author, Player reported, String message, String targetName) {
        for (Player player : this.server.getOnlinePlayers()) {
            if (player.hasPermission("eternalreports.recieve")) {
                String reportForAdministrator = this.pluginConfiguration.userMessages.reportForAdministrator;

                Formatter formatter = new Formatter()
                    .register("{USER}", targetName)
                    .register("{REASON}", message)
                    .register("{REPORTED_BY}", author.getName());

                this.notificationAnnouncer.announceMessage(player.getUniqueId(), formatter.format(reportForAdministrator));
            }
        }

        this.notificationAnnouncer.announceMessage(author.getUniqueId(), this.pluginConfiguration.userMessages.reportSend);

        if (this.pluginConfiguration.enabled) {
            this.discordReportWebHook.sendWebhookMessage(author, reported, message);
        }

        this.statisticsService.addReport();
    }
}
