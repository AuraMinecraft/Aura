package net.aniby.aura.gamemaster.event.abyss;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.aniby.aura.gamemaster.CustomConfig;
import net.aniby.aura.tool.DoubleFrequencyMap;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AbyssManager {
    HashMap<Player, Player> invites = new HashMap<>();
    AuraGameMaster plugin;
    CustomConfig config;

    HashMap<String, Double> attributeMap(Attribute attribute) {
        HashMap<String, Double> map = new HashMap<>();
        ConfigurationSection attributes = config.getConfiguration().getConfigurationSection("attributes");
        assert attributes != null;
        ConfigurationSection section = attributes.getConfigurationSection(attribute.name());
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, section.getDouble(key));
            }
        }
        return map;
    }

    double getAttribute(Attribute attribute, String playerName) {
        ConfigurationSection section = config.getConfiguration().getConfigurationSection("attributes")
                .getConfigurationSection(attribute.name());
        if (section != null) {
            if (section.getKeys(false).contains(playerName))
                return section.getDouble(playerName);
        }
        return 0;
    }

    @SneakyThrows
    void setAttribute(Attribute attribute, String playerName, double value) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            assert attributeInstance != null;
            attributeInstance.setBaseValue(attributeInstance.getDefaultValue() + value);
        } else {
            List<String> balanceQueue = config.getConfiguration().getStringList("balance_queue");
            if (!balanceQueue.contains(playerName)) {
                balanceQueue.add(playerName);
                config.getConfiguration().set("balance_queue", balanceQueue);
            }
        }

        String attributeName = attribute.name();

        ConfigurationSection attributes = config.getConfiguration().getConfigurationSection("attributes");
        assert attributes != null;
        if (!attributes.contains(attributeName))
            attributes.createSection(attributeName);

        ConfigurationSection section = attributes.getConfigurationSection(attributeName);
        assert section != null;
        section.set(playerName, value);

        config.save();
    }

    @SneakyThrows
    void updateAttributes(@NotNull Player player) {
        String playerName = player.getName();

        List<String> balanceQueue = config.getConfiguration().getStringList("balance_queue");
        if (balanceQueue.contains(playerName)) {
            balanceQueue.remove(playerName);
            config.getConfiguration().set("balance_queue", balanceQueue);
            config.save();

            for (Attribute attribute : Attribute.values()) {
                HashMap<String, Double> map = attributeMap(attribute);
                if (!map.isEmpty() && map.containsKey(playerName)) {
                    AttributeInstance attributeInstance = player.getAttribute(attribute);
                    assert attributeInstance != null;
                    double value = map.get(playerName);
                    attributeInstance.setBaseValue(attributeInstance.getDefaultValue() + value);
                }
            }
            player.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "samovar_balance"));
        }
    }

    public void plusSamovar(Player player) {
        String playerName = player.getName();

        // Godly
        double attackDamage = getAttribute(Attribute.GENERIC_ATTACK_DAMAGE, playerName);
        if (attackDamage == 0) {
            setAttribute(Attribute.GENERIC_ATTACK_DAMAGE, playerName, 0.5);
            player.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "samovar_plus"));
        }

        // Compensate
        double health = getAttribute(Attribute.GENERIC_MAX_HEALTH, playerName);
        if (health < 0) {
            DoubleFrequencyMap frequencyMap = new DoubleFrequencyMap(attributeMap(Attribute.GENERIC_MAX_HEALTH));
            ArrayList<String> list = frequencyMap.getMostCommonNotLower(health);
            if (!list.isEmpty()) {
                String targetName = list.get(0);
                double newTargetHealth = frequencyMap.getFrequencies().get(targetName) - health;
                setAttribute(Attribute.GENERIC_MAX_HEALTH, targetName, newTargetHealth);
                Player target = Bukkit.getPlayer(targetName);
                if (target != null)
                    target.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "samovar_balance"));

                setAttribute(Attribute.GENERIC_MAX_HEALTH, playerName, 0);
                player.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "samovar_balance"));
            }
        }
    }

    public void minusSamovar(Player player) {
        String playerName = player.getName();

        // Godly
        double attackDamage = getAttribute(Attribute.GENERIC_ATTACK_DAMAGE, playerName);
        if (attackDamage == 0) {
            setAttribute(Attribute.GENERIC_ATTACK_DAMAGE, playerName, -0.4);
            player.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "samovar_minus"));
        }
    }

    public void plusAbyss(Player player) {
        String playerName = player.getName();

        // Compensate
        double health = getAttribute(Attribute.GENERIC_MAX_HEALTH, playerName);
        double attackDamage = getAttribute(Attribute.GENERIC_ATTACK_DAMAGE, playerName);
        if (attackDamage < 0) {
            setAttribute(Attribute.GENERIC_ATTACK_DAMAGE, playerName, 0);
            setAttribute(Attribute.GENERIC_MAX_HEALTH, playerName, health + attackDamage * -5);
            player.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "abyss_plus"));
        } else {
            double mappedHealth = attributeMap(Attribute.GENERIC_MAX_HEALTH).values().stream().mapToDouble(Double::doubleValue).sum();
            if (mappedHealth < 0) {
                setAttribute(Attribute.GENERIC_MAX_HEALTH, playerName, health + mappedHealth * -1);
                player.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "abyss_balance"));
            }
        }
    }

    public void minusAbyss(Player player) {
        String playerName = player.getName();

        // Godly
        double attackDamage = getAttribute(Attribute.GENERIC_MAX_HEALTH, playerName);
        if (attackDamage == 0) {
            setAttribute(Attribute.GENERIC_MAX_HEALTH, playerName, -2);
            player.sendMessage(CoreConfig.getMessage(config.getConfiguration(), "abyss_minus"));
        }
    }
}
