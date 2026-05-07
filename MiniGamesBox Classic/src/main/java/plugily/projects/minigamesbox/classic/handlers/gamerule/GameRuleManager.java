package plugily.projects.minigamesbox.classic.handlers.gamerule;

import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.classic.PluginMain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @version 2.0
 * @Author Hai
 * @Date 2026/5/4 15:09
 * @description
 */
public class GameRuleManager {
    private final PluginMain plugin;

    private final Map<GameRule<?>, Object> gameRules = new LinkedHashMap<>();


    public GameRuleManager(PluginMain plugin) {
        this.plugin = plugin;
        init();
    }

    public void init() {
        ConfigurationSection gameRuleSection = plugin.getConfig().getConfigurationSection("GameRule");
        if (gameRuleSection != null) {
            plugin.getLogger().info("开始加载游戏规则配置...");
            for (String key : gameRuleSection.getKeys(false)) {
                Object value = gameRuleSection.get(key);
                plugin.getLogger().info("读取配置项: " + key + " = " + value + " (类型: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");

                GameRule<?> gameRuleEnum = convertToGameRule(key);
                if (gameRuleEnum != null) {
                    if (validateValueType(gameRuleEnum, value)) {
                        gameRules.put(gameRuleEnum, value);
                        plugin.getLogger().info("成功加载世界规则: " + key + " 值为: " + value);
                    } else {
                        plugin.getLogger().warning("规则 " + key + " 的值类型不匹配，期望: " + gameRuleEnum.getType().getSimpleName() + "，实际: " + (value != null ? value.getClass().getSimpleName() : "null"));
                    }
                } else {
                    plugin.getLogger().warning("无法识别的游戏规则: " + key + "，已跳过");
                }
            }
        }else {
            plugin.getLogger().info("没有配置世界规则");
        }

        plugin.getLogger().info("成功加载的规则数量: " + gameRules.size());
    }

    private GameRule<?> convertToGameRule(String name) {
        try {
            GameRule<?> gameRule = GameRule.getByName(name);

            if (gameRule == null) {
                String camelCaseName = convertToCamelCase(name);
                plugin.getLogger().info("规则 '" + name + "' 未找到，尝试转换为: " + camelCaseName);
                gameRule = GameRule.getByName(camelCaseName);
            }

            plugin.getLogger().info("尝试转换规则: " + name + ", 结果: " + (gameRule != null ? "找到" : "未找到"));

            if (gameRule != null) {
                plugin.getLogger().info("规则 " + name + " 的类型: " + gameRule.getType().getSimpleName());
                return gameRule;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("转换 GameRule 失败: " + name + ", 错误: " + e.getMessage());
        }
        return null;
    }

    /**
     * 将下划线命名转换为小驼峰命名
     * 例如: DO_IMMEDIATE_RESPAWN -> doImmediateRespawn
     */
    private String convertToCamelCase(String underscoreName) {
        if (underscoreName == null || underscoreName.isEmpty()) {
            return underscoreName;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < underscoreName.length(); i++) {
            char c = underscoreName.charAt(i);

            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    /**
     * 验证配置值的类型是否与 GameRule 期望的类型匹配
     */
    private boolean validateValueType(GameRule<?> gameRule, Object value) {
        if (value == null) {
            return false;
        }

        Class<?> expectedType = gameRule.getType();

        // Boolean 类型检查
        if (expectedType.equals(Boolean.class)) {
            return value instanceof Boolean || value instanceof String;
        }
        // Integer 类型检查
        else if (expectedType.equals(Integer.class)) {
            if (value instanceof Number) {
                return true;
            }
            if (value instanceof String) {
                try {
                    Integer.parseInt((String) value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        }
        // String 类型检查
        else if (expectedType.equals(String.class)) {
            return true;
        }

        return expectedType.isInstance(value);
    }

    /**
     * 转换值为正确的类型
     */
    private Object convertValue(GameRule<?> gameRule, Object value) {
        Class<?> expectedType = gameRule.getType();

        if (expectedType.equals(Boolean.class)) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
            return Boolean.valueOf(value.toString());
        }
        else if (expectedType.equals(Integer.class)) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            return Integer.valueOf(value.toString());
        }
        else if (expectedType.equals(String.class)) {
            return value.toString();
        }

        return value;
    }

    public void loadGameRule(IPluginArena arena) {
        Location startLocation = arena.getStartLocation();
        World world = startLocation.getWorld();
        if (world != null) {
            for (Map.Entry<GameRule<?>, Object> entry : gameRules.entrySet()) {
                GameRule<?> gameRule = entry.getKey();
                Object value = convertValue(gameRule, entry.getValue());

                try {
                    setGameRuleSafely(world, gameRule, value);
                    plugin.getLogger().info("设置世界规则: " + gameRule.getName() + " 值为: " + value);
                } catch (Exception e) {
                    plugin.getLogger().warning("设置世界规则失败: " + gameRule.getName() + ", 错误: " + e.getMessage());
                }
            }
        }else {
            plugin.getLogger().warning("无法加载世界规则，世界为空");
        }
    }


    @SuppressWarnings("unchecked")
    private <T> void setGameRuleSafely(World world, GameRule<T> gameRule, Object value) {
        try {
            T convertedValue = (T) value;
            world.setGameRule(gameRule, convertedValue);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("无法将值转换为正确的类型: " + gameRule.getName(), e);
        }
    }

}
