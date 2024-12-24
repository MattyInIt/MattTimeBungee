package org.mattplugins.mattTimeBungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class MattTimeBungee extends Plugin implements Listener {

    private final Map<UUID, Long> playtime = new HashMap<>();
    private final Map<UUID, Long> loginTime = new HashMap<>();
    private final File playtimeFile = new File(getDataFolder(), "playtime.yml");
    private Yaml yaml;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new PlaytimeCommand());
        getProxy().getPluginManager().registerCommand(this, new PlaytimeTopCommand());
        loadPlaytime();
    }

    @Override
    public void onDisable() {
        savePlaytime();
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        loginTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long login = loginTime.remove(playerId);
        long sessionTime = System.currentTimeMillis() - login;
        playtime.put(playerId, playtime.getOrDefault(playerId, 0L) + sessionTime);
    }


    private void loadPlaytime() {
        if (!playtimeFile.exists()) {
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(playtimeFile)) {
            LoaderOptions options = new LoaderOptions();
            yaml = new Yaml(new Constructor(options), new Representer(new DumperOptions()), new DumperOptions());
            Map<String, Long> data = yaml.load(inputStream);
            if (data != null) {
                data.forEach((key, value) -> playtime.put(UUID.fromString(key), value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePlaytime() {
        try (FileWriter writer = new FileWriter(playtimeFile)) {
            yaml = new Yaml(new DumperOptions());
            Map<String, Long> data = new HashMap<>();
            playtime.forEach((key, value) -> data.put(key.toString(), value));
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class PlaytimeCommand extends Command {
        public PlaytimeCommand() {
            super("playtime");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length != 1) {
                sender.sendMessage(new TextComponent("Usage: /playtime <user>"));
                return;
            }
            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent("Player not found"));
                return;
            }
            UUID playerId = target.getUniqueId();
            long time = playtime.getOrDefault(playerId, 0L);
            sender.sendMessage(new TextComponent(target.getName() + " has played for " + (time / 1000) + " seconds."));
        }
    }

    public class PlaytimeTopCommand extends Command {
        public PlaytimeTopCommand() {
            super("playtimetop");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            List<Map.Entry<UUID, Long>> topPlayers = playtime.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            sender.sendMessage(new TextComponent("Top 10 players by playtime:"));
            for (int i = 0; i < topPlayers.size(); i++) {
                ProxiedPlayer player = getProxy().getPlayer(topPlayers.get(i).getKey());
                if (player != null) {
                    sender.sendMessage(new TextComponent((i + 1) + ". " + player.getName() + " - " + (topPlayers.get(i).getValue() / 1000) + " seconds"));
                }
            }
        }
    }
}