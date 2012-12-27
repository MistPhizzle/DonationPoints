package com.mistphizzle.donationpoints.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DonationPoints extends JavaPlugin {

	protected static Logger log;
	protected UpdateChecker updateChecker;

	public static DonationPoints instance;

	// Configs
	File configFile;
	FileConfiguration config;
	File cumulativeFile;
	FileConfiguration cumulative;

	// Commands
	Commands cmd;

	// Events
	private final SignListener signListener = new SignListener(this);
	private final PlayerListener playerListener = new PlayerListener(this);

	@Override
	public void onEnable() {

		instance = this;

		// Logger
		this.log = this.getLogger();

		// Initialize Config
		configFile = new File(getDataFolder(), "config.yml");
		cumulativeFile = new File(getDataFolder(), "cumulativepackages.yml");

		// Use firstRun() method
		try {
			firstRun();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Declare FileConfigurations, load.
		config = new YamlConfiguration();
		cumulative = new YamlConfiguration();
		loadYamls();

		// Events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(signListener, this);
		pm.registerEvents(playerListener, this);

		// Database Variables
		DBConnection.host = config.getString("MySQL.host", "localhost");
		DBConnection.db = config.getString("MySQL.database", "minecraft");
		DBConnection.user = config.getString("MySQL.username", "root");
		DBConnection.pass = config.getString("MySQL.password", "");
		DBConnection.port = config.getInt("MySQL.port", 3306);
		DBConnection.engine = config.getString("MySQL.engine", "sqlite");
		DBConnection.sqlite_db = config.getString("MySQL.SQLiteDB", "donationpoints.db");

		DBConnection.init();

		// Register Commands

		cmd = new Commands(this);

		// Metrics
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit stats.
		}

		// Run Update Checker, log it to console.
		if (getConfig().getBoolean("General.AutoCheckForUpdates", true)) {

			this.updateChecker = new UpdateChecker(this, "http://dev.bukkit.org/server-mods/donationpoints/files.rss");
			if (UpdateChecker.updateNeeded()) {
				this.log.info("[DonationPoints] A new version is available: " + this.updateChecker.getVersion());
				this.log.info("[DonationPoints] Get it from: " + this.updateChecker.getLink());
			}
		}

	}

	@Override
	public void onDisable() {
		DBConnection.sql.close();
	}

	// Methods
	public void firstRun() throws Exception {
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
			log.info("Config not found. Generaing.");
		}
		if (!cumulativeFile.exists()) {
			cumulativeFile.getParentFile().mkdirs();
			copy(getResource("cumulativepackages.yml"), cumulativeFile);
			log.info("Creating Cumulative Packages File.");
		}
	}

	private void loadYamls() {
		try {
			config.load(configFile);
			config.load(cumulativeFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf))>0) {
				out.write(buf,0,len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveYamls() {
		try {
			config.save(configFile);
			config.save(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Get Cumulative Points Configs
	public FileConfiguration getWitchConfig() {
		if (cumulative == null) {
			reloadCumulativeConfig();
		}
		return cumulative;
	}

	public void reloadCumulativeConfig() {
		if (cumulativeFile == null) {
			cumulativeFile = new File(getDataFolder(), "cumulativepackages.yml");
		}
		cumulative = YamlConfiguration.loadConfiguration(cumulativeFile);

		InputStream defConfigStream = getResource("cumulativepackages.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			cumulative.setDefaults(defConfig);
		}
	}

	public void saveWitchConfig() {
		if (cumulative == null || cumulativeFile == null) {
			return;
		}
		try {
			cumulative.save(cumulativeFile);
		} catch (IOException ex) {
			this.log.info("Could not save config to " + cumulativeFile);
		}
	}

	public static DonationPoints getInstance() {
		return instance;
	}

}
