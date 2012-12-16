package com.mistphizzle.donationpoints.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DonationPoints extends JavaPlugin {
	
	protected static Logger log;
	
	// Configs
	File configFile;
	FileConfiguration config;
	
	// Commands
	Commands cmd;
	
	// Events
	private final SignListener signListener = new SignListener(this);
	private final PlayerListener playerListener = new PlayerListener(this);
	
	@Override
	public void onEnable() {
		
		// Logger
		this.log = this.getLogger();
		
		// Initialize Config
		configFile = new File(getDataFolder(), "config.yml");
		
		// Use firstRun() method
		try {
			firstRun();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Declare FileConfigurations, load.
		config = new YamlConfiguration();
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
	}
	
	@Override
	public void onDisable() {
		DBConnection.disable();
	}
	
	// Methods
	private void firstRun() throws Exception {
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
		}
	}
	
	private void loadYamls() {
		try {
			config.load(configFile);
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
