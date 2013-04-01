package com.mistphizzle.donationpoints.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DonationPoints extends JavaPlugin {

	protected static Logger log;

	public static DonationPoints instance;

	// Configs
	File configFile;
	FileConfiguration config;
	File messageConfigFile;
	FileConfiguration messageConfig;

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
		messageConfigFile = new File(getDataFolder(), "messages.yml");

		// Use firstRun() method
		try {
			firstRun();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Declare FileConfigurations, load.
		config = new YamlConfiguration();
		messageConfig = new YamlConfiguration();
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
		Methods.engine = config.getString("MySQL.engine", "sqlite");

		// Other Variables.
		PlayerListener.SignMessage = config.getString("General.SignMessage");
		SignListener.SignMessage = config.getString("General.SignMessage");

		DBConnection.init();
		DBConnection.sql.modifyQuery("UPDATE dp_players SET player = lower(player)");

		// Register Commands

		cmd = new Commands(this);

		// Metrics
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit stats.
		}

		if (getConfig().getBoolean("General.ExpireOnStartup")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM dp_transactions WHERE expired = 'false' AND expiredate = '" + Methods.getCurrentDate() + "';");
			try {
				if (rs2.next()) {
					String pack2 = rs2.getString("package");
					String user = rs2.getString("player");

					List<String> commands = getConfig().getStringList("packages." + pack2 + ".expirecommands");
					for (String cmd : commands) {
						getServer().dispatchCommand(getServer().getConsoleSender(), cmd.replace("%player", user));
					}
					DBConnection.sql.modifyQuery("UPDATE dp_transactions SET expired = 'true' WHERE player = '" + user + "' AND expiredate = '" + Methods.getCurrentDate() + "' AND package = '" + pack2 + "';");
					log.info("Some packages were expired.");
				} else if (!rs2.next()) {
					log.info("There were no packages to expire.");

				}
			} catch (SQLException ex) {
				ex.printStackTrace();
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
			log.info("Config not found. Generating.");
		}
		if (!messageConfigFile.exists()) {
			messageConfigFile.getParentFile().mkdirs();
			copy(getResource("messages.yml"), messageConfigFile);
			log.info("messages.yml not found, generating...");
		}
	}

	private void loadYamls() {
		try {
			config.load(configFile);
			config.load(messageConfigFile);
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
			config.save(messageConfigFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static DonationPoints getInstance() {
		return instance;
	}

	public FileConfiguration getMessageConfig() {
		if (messageConfig == null) {
			reloadMessageConfig();
		}
		return messageConfig;
	}

	public void reloadMessageConfig() {
		if (messageConfigFile == null) {
			messageConfigFile = new File(getDataFolder(), "messages.yml");
		}
		messageConfig = YamlConfiguration.loadConfiguration(messageConfigFile);

		InputStream defConfigStream = getResource("messages.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			messageConfig.setDefaults(defConfig);
		}
	}

	public void saveMessageConfig() {
		if (messageConfig == null || messageConfigFile == null) {
			return;
		}
		try {
			messageConfig.save(messageConfigFile);
		} catch (IOException ex) {
			this.log.info("Could not save config to " + messageConfigFile);
		}
	}



}
