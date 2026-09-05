/*
 *  MiniGamesBox - Library box with massive content that could be seen as minigames core.
 *  Copyright (C) 2023 Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugily.projects.minigamesbox.classic.kits;

import com.cryptomorin.xseries.XItemStack;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import plugily.projects.minigamesbox.api.kit.HandleItem;
import plugily.projects.minigamesbox.api.kit.IKit;
import plugily.projects.minigamesbox.api.kit.IKitRegistry;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.kits.basekits.FreeKit;
import plugily.projects.minigamesbox.classic.kits.basekits.Kit;
import plugily.projects.minigamesbox.classic.kits.basekits.LevelKit;
import plugily.projects.minigamesbox.classic.kits.basekits.PremiumKit;
import plugily.projects.minigamesbox.classic.kits.free.EmptyKit;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 21.09.2021
 */
public class KitRegistry implements IKitRegistry {

  private static HandleItem handleItem;
  public final List<IKit> kits = new java.util.ArrayList<>();
  public final PluginMain plugin;
  private final NamespacedKey kitItemTagKey;
  private IKit defaultKit;

  public KitRegistry(PluginMain plugin) {
    this.plugin = plugin;
    this.kitItemTagKey = new NamespacedKey(plugin, "kit_item_tag");
  }

  @Override
  public HandleItem getHandleItem() {
    return handleItem;
  }

  @Override
  public void setHandleItem(HandleItem handleItem) {
    KitRegistry.handleItem = handleItem;
  }

  @Override
  public void registerKit(IKit kit) {
    if (!plugin.getConfigPreferences().getOption("KITS")) {
      plugin.getDebugger().performance("Kit", "Kits are disabled, thus registerKit method will not be ran.");
      plugin.getDebugger().debug("Kit " + kit.getKitFileName() + " can't be added as kits are disabled");
      return;
    }
    if (kits.contains(kit)) {
      plugin.getDebugger().debug("Kit " + kit.getKitFileName() + " can't be added as its already registered");
      return;
    }

    ConfigurationSection configurationSection = kit.getKitConfigSection();
    if (configurationSection != null && !configurationSection.getBoolean("enabled", false)) {
      plugin.getDebugger().debug("Kit " + kit.getKitFileName() + " is disabled by kit file");
      return;
    }

    plugin.getDebugger().debug("Registered {0} kit", kit.getKitFileName());
    kits.add(kit);
  }

  @Override
  public void registerKits(List<String> optionalConfigurations) {
    if (!plugin.getConfigPreferences().getOption("KITS")) {
      plugin.getDebugger().performance("Kit", "Kits are disabled, thus registerKits method will not be ran.");
      return;
    }
    try {
      if (!new File(plugin.getDataFolder() + File.separator + "kits").exists()) {
        new File(plugin.getDataFolder() + "/kits").mkdir();
      }
      File[] kitsFiles = new File(plugin.getDataFolder() + File.separator + "kits").listFiles();
      if (kitsFiles == null || kitsFiles.length == 0) {
        plugin.getDebugger().debug(Level.SEVERE, "No kits found in kits folder, but kits are enabled in config.yml.");
        plugin.getDebugger().debug(Level.SEVERE, "Please add kits to kits folder and restart the server.");
        plugin.onDisable();
        return;
      }

      Pattern numberPrefix = Pattern.compile("^[0-9]+");
      Arrays.sort(kitsFiles, Comparator
              .<File, BigInteger>comparing(
                      file -> {
                        Matcher matcher = numberPrefix.matcher(file.getName());
                        return matcher.find()
                                ? new BigInteger(matcher.group())
                                : null;
                      },
                      Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(File::getName));

      for (File file : kitsFiles) {
        String kitFileName = ConfigUtils.removeExtension(file.getName());
        plugin.getDebugger().debug(Level.INFO, "Trying to load " + kitFileName);
        FileConfiguration kitsConfig = ConfigUtils.getConfig(plugin, "/kits/" + kitFileName);
        loadKitConfig(kitFileName, kitsConfig, optionalConfigurations);
      }
    } catch (Exception exception) {
      plugin.getDebugger().debug(Level.WARNING, "ERROR IN LOADING KITS");
    }
  }

  /**
   * Loads the configuration for a kit.
   *
   * @param kitsConfig the yml of the kit to load the configuration for
   */
  public void loadKitConfig(String kitFileName, FileConfiguration kitsConfig, List<String> optionalConfigurations) {
    plugin.getDebugger().debug(Level.INFO, "Loading Kit " + kitFileName + " ...");

    if (!kitsConfig.getBoolean("enabled", false)) {
      plugin.getDebugger().debug("Kit " + kitFileName + " is disabled by kit file");
      return;
    }

    String kit_name = kitsConfig.getString("name", kitFileName);
    List<String> kit_description = kitsConfig.getStringList("description");

    ItemStack itemStack = XMaterial.BEDROCK.parseItem();
    if (kitsConfig.getConfigurationSection("display_item") != null) {
      itemStack = XItemStack.deserialize(kitsConfig.getConfigurationSection("display_item"));
    }

    String kitType = kitsConfig.getString("kit_type");

    if (kitType == null) {
      plugin.getDebugger().debug(Level.SEVERE, "Kit " + kitFileName + " kit_type is null.");
      plugin.getDebugger().debug(Level.SEVERE, "Kit " + kitFileName + " will not be loaded.");
      return;
    }

    Kit kit;

    switch (kitType) {
      case "free": {
        kit = new FreeKit(kitFileName, kit_name, kit_description, itemStack);
        break;
      }
      case "level": {
        kit = new LevelKit(kitFileName, kit_name, kit_description, itemStack);
        ((LevelKit) kit).setLevel(kitsConfig.getInt("required_level"));
        break;
      }
      case "premium": {
        kit = new PremiumKit(kitFileName, kit_name, kit_description, itemStack);
        ((PremiumKit)kit).setPermissionKey(kitsConfig.getString("permission_key"));
        break;
      }
      default: {
        plugin.getDebugger().debug(Level.SEVERE, "Kit " + kitFileName + " kit_type is not recognised.");
        plugin.getDebugger().debug(Level.SEVERE, "Kit " + kitFileName + " will not be loaded.");
        return;
      }
    }

    if (kitsConfig.getString("unlockedOnDefault") == null) {
      plugin.getDebugger().debug(Level.SEVERE, "Kit " + kitFileName + " does not have an unlockedOnDefault configuration.");
      plugin.getDebugger().debug(Level.SEVERE, "Kit " + kitFileName + " will not be loaded.");
      return;
    }
    kit.setUnlockedOnDefault(kitsConfig.getBoolean("unlockedOnDefault"));

    HashMap<ItemStack, Integer> kitItems = new HashMap<>();

    ConfigurationSection inventoryConfigurationSection = kitsConfig.getConfigurationSection("inventory");
    if (inventoryConfigurationSection != null) {
      inventoryConfigurationSection.getKeys(false).forEach((k) -> {

        ConfigurationSection itemConfigurationSection = inventoryConfigurationSection.getConfigurationSection(k);
        assert itemConfigurationSection != null;

        ConfigurationSection itemStackConfigurationSection = itemConfigurationSection.getConfigurationSection("item");
        assert itemStackConfigurationSection != null;
        ItemStack item = deserializeTaggedItem(itemStackConfigurationSection);
        Integer slot = itemConfigurationSection.getInt("slot");

        kitItems.put(item, slot);
      });
      kit.setKitItems(kitItems);
    } else {
      plugin.getDebugger().debug(Level.SEVERE, "The kit " + kit.getKitFileName() + " does not have an inventory configuration section.");
      plugin.getDebugger().debug(Level.SEVERE, "The kit " + kit.getKitFileName() + " will not give any inventory items.");
    }


    ConfigurationSection armourConfigurationSection = kitsConfig.getConfigurationSection("armour");
    if (armourConfigurationSection != null) {

      ConfigurationSection helmetConfigurationSection = armourConfigurationSection.getConfigurationSection("helmet");
      if (helmetConfigurationSection != null) {
        ItemStack item = deserializeTaggedItem(helmetConfigurationSection);
        kit.setKitHelmet(item);
      }

      ConfigurationSection chestplateConfigurationSection = armourConfigurationSection.getConfigurationSection("chestplate");
      if (chestplateConfigurationSection != null) {
        ItemStack item = deserializeTaggedItem(chestplateConfigurationSection);
        kit.setKitChestplate(item);
      }

      ConfigurationSection leggingsConfigurationSection = armourConfigurationSection.getConfigurationSection("leggings");
      if (leggingsConfigurationSection != null) {
        ItemStack item = deserializeTaggedItem(leggingsConfigurationSection);
        kit.setKitLeggings(item);
      }

      ConfigurationSection bootsConfigurationSection = armourConfigurationSection.getConfigurationSection("boots");
      if (bootsConfigurationSection != null) {
        ItemStack item = deserializeTaggedItem(bootsConfigurationSection);
        kit.setKitBoots(item);
      }
    } else {
      plugin.getDebugger().debug(Level.SEVERE, "The kit " + kit.getKitFileName() + " does not have an armour configuration section.");
      plugin.getDebugger().debug(Level.SEVERE, "The kit " + kit.getKitFileName() + " will not give any armour items.");
    }

    List<String> kit_actions = kitsConfig.getStringList("abilities");
    kit.setAbilities(kit_actions);

    if (optionalConfigurations != null) {
      optionalConfigurations.forEach((configuration) -> {
        if (kitsConfig.contains(configuration)) {
          kit.addOptionalConfiguration(configuration, kitsConfig.get(configuration));
          plugin.getDebugger().debug("Kit " + kit.getKitFileName() + " has optional configuration " + configuration);
        }
      });
    }

    plugin.getDebugger().debug("Kit " + kit.getKitFileName() + " loaded.");
    kits.add(kit);
  }

  private ItemStack deserializeTaggedItem(ConfigurationSection itemConfigurationSection) {
    ItemStack item = XItemStack.deserialize(itemConfigurationSection);
    if (item == null) {
      return null;
    }
    List<String> tags;
    if (itemConfigurationSection.isList("tag")) {
      tags = itemConfigurationSection.getStringList("tag").stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
    } else {
      String tag = itemConfigurationSection.getString("tag", "").trim();
      tags = tag.isEmpty() ? List.of() : List.of(tag);
    }

    if (!tags.isEmpty()) {
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        meta.getPersistentDataContainer().set(kitItemTagKey, PersistentDataType.LIST.strings(), tags);
        item.setItemMeta(meta);
      }
    }

    return item;
  }

  @Override
  public boolean hasItemTag(ItemStack itemStack, String tag) {
    if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta() || tag == null || tag.isBlank()) {
      return false;
    }

    List<String> tags = itemStack.getItemMeta().getPersistentDataContainer().get(kitItemTagKey, PersistentDataType.LIST.strings());
    return tags != null && tags.contains(tag);
  }

  @Override
  public IKit getDefaultKit() {
    if (defaultKit == null) {
      setDefaultKit(new EmptyKit("default", "default"));
    }
    plugin.getDebugger().debug("getDefaultKit is {0}", defaultKit.getName());
    return defaultKit;
  }

  @Override
  public void setDefaultKit(IKit defaultKit) {
    plugin.getDebugger().debug("DefaultKit set to {0}", defaultKit.getName());
    this.defaultKit = defaultKit;
  }

  @Override
  public void setDefaultKit(String defaultKitName) {
    String defaultKitKey = plugin.getConfig().getString("Kit.Default", defaultKitName);
    AtomicReference<IKit> defaultKit = new AtomicReference<>(getKitByKey(defaultKitKey));
    if (defaultKit.get() == null) {
      if (getKits().isEmpty()) {
        plugin.getDebugger().debug(Level.SEVERE, "Default kit set is not found and there are no available kits.");
        plugin.getDebugger().debug(Level.SEVERE, "Please add kits to the 'kits' folder and restart the server");
        plugin.onDisable();
        return;
      }
      getKits().stream().filter(IKit::isUnlockedOnDefault).findFirst().ifPresent(defaultKit::set);
      if (defaultKit.get() == null) {
        plugin.getDebugger().debug(Level.SEVERE, "Default kit set is not found and there are no available free kits.");
        plugin.getDebugger().debug(Level.SEVERE, "Please add free kits to the 'kits' folder and restart the server");
        plugin.onDisable();
        return;
      }
      plugin.getDebugger().debug("Default kit {0} not found, using {1}", defaultKitKey, defaultKit.get().getKitFileName());
    }
    this.plugin.getDebugger().debug("DefaultKit set to {0}", defaultKit.get().getName());
    this.defaultKit = defaultKit.get();
  }

  @Override
  public NamespacedKey getKitItemTagKey() {
    return kitItemTagKey;
  }

  @Override
  public List<IKit> getKits() {
    return kits;
  }

  @Override
  public IKit getKitByKey(String key) {
    for (IKit kit : kits) {
      if (kit.getKitFileName().equalsIgnoreCase(key)) {
        return kit;
      }
    }
    return null;
  }


  @Override
  public IKit getKitByName(String key) {
    for (IKit kit : kits) {
      if (kit.getName().equalsIgnoreCase(key)) {
        return kit;
      }
    }
    return null;
  }

}
