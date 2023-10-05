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

package plugily.projects.minigamesbox.classic.utils.services.exception;

import plugily.projects.minigamesbox.classic.PluginMain;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Reporter service for reporting exceptions directly to website reporter panel
 */
public class ReporterService {

  private final PluginMain plugin;
  private final String pluginName;
  private final String pluginVersion;
  private final String serverVersion;
  private final String error;

  //don't create it outside core
  ReporterService(PluginMain plugin, String pluginName, String pluginVersion, String serverVersion, String error) {
    this.plugin = plugin;
    this.pluginName = pluginName;
    this.pluginVersion = pluginVersion;
    this.serverVersion = serverVersion;
    this.error = error;
  }

  public void reportException() {
  }

}
