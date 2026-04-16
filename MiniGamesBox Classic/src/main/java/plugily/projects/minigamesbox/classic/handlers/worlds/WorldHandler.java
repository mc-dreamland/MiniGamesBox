package plugily.projects.minigamesbox.classic.handlers.worlds;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import plugily.projects.minigamesbox.classic.PluginMain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class WorldHandler {

  public static final String BACKUP_WORLDS_SUFFIX = "_backupWorlds";

  /**
   * A method to clone a world given the original world to clone from and the name of the new world
   *
   * @param world The original world
   * @param name  The name of the new world
   * @return World The cloned world
   */
  public static World cloneWorld(World world, String name) {
    WorldCreator creator = new WorldCreator(name);
    creator.copy(world);

    return creator.createWorld();
  }

  /**
   * Deletes a specified world from the server.
   *
   * @param  world  the world to be deleted
   * @return        true if the world was successfully deleted, false otherwise
   */
  public static boolean deleteWorld(World world) {
    boolean success = true;
    if (world != null) {
      if (!Bukkit.unloadWorld(world, true)) {
        return false;
      }
      File worldFolder = world.getWorldFolder();
      if (worldFolder.exists()) {
        try {
          deleteFolder(worldFolder);
        }
        catch (WorldDeletionException e) {
          success = false;
        }
      }
    }
    return success;
  }

  /**
   * Copies a backup world folder into the server root world container.
   * This method only copies files and does not load or unload worlds.
   *
   * @param plugin the plugin used to resolve the backup directory name
   * @param worldName the backup world folder name inside <pluginName>_backupWorlds
   * @return true if the copy completed successfully, false otherwise
   */
  public static boolean copyWorldFile(PluginMain plugin, String worldName) {
    if(plugin == null || worldName == null ) {
      return false;
    }

    if(worldName.trim().isEmpty() ) {
      return false;
    }

    File worldContainer = Bukkit.getWorldContainer();
    plugin.getLogger().info("复制地图中:"+plugin.getDescription().getName() + BACKUP_WORLDS_SUFFIX+"\\"+worldName);
    File backupFolder = new File(worldContainer, plugin.getDescription().getName() + BACKUP_WORLDS_SUFFIX);
    File sourceFolder = new File(backupFolder, worldName);
    File targetFolder = new File(worldContainer, worldName);

    if(!sourceFolder.exists() || !sourceFolder.isDirectory()) {
      return false;
    }

    try {
      copyFolder(sourceFolder.toPath(), targetFolder.toPath());
      return true;
    } catch(IOException exception) {
      return false;
    }
  }

  /**
   * Deletes a folder and all its contents recursively.
   *
   * @param  folder  the folder to be deleted
   * @throws WorldDeletionException  if the folder deletion fails
   */
  private static void deleteFolder(File folder) throws WorldDeletionException {
    if (folder.isDirectory()) {
      File[] files = folder.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteFolder(file);
        }
      }
    }
    if (!folder.delete()) {
      throw new WorldDeletionException("Failed to delete folder: " + folder.getAbsolutePath());
    }
  }

  private static void copyFolder(Path source, Path target) throws IOException {
    if(Files.isDirectory(source)) {
      Files.createDirectories(target);
    }

    File[] files = source.toFile().listFiles();
    if(files == null) {
      return;
    }

    for(File file : files) {
      Path destination = target.resolve(file.getName());
      if(file.isDirectory()) {
        copyFolder(file.toPath(), destination);
        continue;
      }

      if("uid.dat".equalsIgnoreCase(file.getName()) || "session.lock".equalsIgnoreCase(file.getName())) {
        continue;
      }

      Files.createDirectories(destination.getParent());
      Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }
  }

  /**
   * Custom exception for world deletion errors.
   */
  public static class WorldDeletionException extends Exception {
    public WorldDeletionException(String message) {
      super(message);
    }
  }

}
