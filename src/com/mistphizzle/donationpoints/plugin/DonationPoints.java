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
		DBConnection.engine = config.getString("MySQL.engine", "sqlite");
		DBConnection.sqlite_db = config.getString("MySQL.SQLiteDB", "donationpoints.db");

		// Other Variables.
		PlayerListener.SignMessage = config.getString("General.SignMessage");
		SignListener.SignMessage = config.getString("General.SignMessage");
		
		//String Variables
		Commands.Prefix = Methods.colorize(getConfig().getString("messages.Prefix"));
		Commands.InvalidArguments = Methods.colorize(getConfig().getString("messages.InvalidArguments"));
		Commands.noPermissionMessage = Methods.colorize(getConfig().getString("messages.NoPermission"));
		Commands.NoCommandExists = Methods.colorize(getConfig().getString("messages.NoCommandExists"));
		Commands.ExpireDate = Methods.colorize(getConfig().getString("messages.ExpireDate"));
		Commands.DPTake = Methods.colorize(getConfig().getString("messages.DPTake"));
		Commands.DPGive = Methods.colorize(getConfig().getString("messages.DPGive"));
		Commands.DPConfirm = Methods.colorize(getConfig().getString("messages.DPConfirm"));
		Commands.DPActivate = Methods.colorize(getConfig().getString("messages.DPActivate"));
		Commands.DPSuccessfulActivation = Methods.colorize(getConfig().getString("messages.DPSuccessfulActivation"));
		Commands.DPFailedActivation = Methods.colorize(getConfig().getString("messages.DPFailedActivation"));
		Commands.NoAccount = Methods.colorize(getConfig().getString("messages.NoAccount"));
		Commands.AccountCreated = Methods.colorize(getConfig().getString("messages.AccountCreated"));
		Commands.NoTransfer = Methods.colorize(getConfig().getString("messages.NoTransfer"));
		Commands.TransferOff = Methods.colorize(getConfig().getString("messages.TransferOff"));
		Commands.TransferSent = Methods.colorize(getConfig().getString("messages.TransferSent"));
		Commands.TransferReceive = Methods.colorize(getConfig().getString("messages.TransferReceive"));
		Commands.PlayerOnly = Methods.colorize(getConfig().getString("messages.PlayerOnly"));
		Commands.PlayerBalance = Methods.colorize(getConfig().getString("messages.PlayerBalance"));
		Commands.OtherBalance = Methods.colorize(getConfig().getString("messages.OtherBalance"));
		Commands.NoPurchaseStarted = Methods.colorize(getConfig().getString("messages.NoPurchaseStarted"));
		Commands.ReloadSuccessful = Methods.colorize(getConfig().getString("messages.ReloadSuccessful"));
		Commands.AccountAlreadyExists = Methods.colorize(getConfig().getString("messages.AccountAlreadyExists"));
		Commands.PurchaseSuccessful = Methods.colorize(getConfig().getString("messages.PurchaseSuccessful"));
		Commands.PackageActivated = Methods.colorize(getConfig().getString("messages.PackageActivated"));
		Commands.NeedActivation = Methods.colorize(getConfig().getString("messages.NeedActivation"));
		Commands.LimitReached = Methods.colorize(getConfig().getString("messages.LimitReached"));
		Commands.DPSet = Methods.colorize(getConfig().getString("messages.DPSet"));
		Commands.InvalidPackage = Methods.colorize(getConfig().getString("messages.InvalidPackage"));
		Commands.NotEnoughPoints = Methods.colorize(getConfig().getString("messages.NotEnoughPoints"));

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

	public static DonationPoints getInstance() {
		return instance;
	}

	public void configReload() {
		reloadConfig();
	}



}
