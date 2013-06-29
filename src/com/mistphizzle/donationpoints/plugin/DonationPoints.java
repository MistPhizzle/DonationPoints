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

import net.milkbowl.vault.permission.Permission;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class DonationPoints extends JavaPlugin {

	protected static Logger log;

	public static DonationPoints instance;

	public static Permission permission = null;

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}
	// Configs
	File configFile;
	FileConfiguration config;

	// Commands
	Commands cmd;

	// Events
	private final SignListener signListener = new SignListener(this);
	private final PlayerListener playerListener = new PlayerListener(this);

	@SuppressWarnings("static-access")
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

		setupPermissions();

		// Declare FileConfigurations, load.
		config = new YamlConfiguration();
		loadYamls();

		configCheck();

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
		DBConnection.playerTable = config.getString("MySQL.PlayerTable", "dp_players");
		DBConnection.transactionTable = config.getString("MySQL.TransactionTable", "dp_transactions");
		DBConnection.frameTable = config.getString("MySQL.ItemFrameTable", "dp_frames");

		// Other Variables.
		PlayerListener.SignMessage = config.getString("General.SignMessage");
		SignListener.SignMessage = config.getString("General.SignMessage");

		//String Variables
		Commands.Server = getConfig().getString("General.ServerName");
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
		Commands.DPPrerequisite = Methods.colorize(getConfig().getString("messages.DPPrerequisite"));

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
		
		if (getConfig().getBoolean("General.PurgeEmptyAccountsOnStartup")) {
			Methods.purgeEmptyAccounts();
			this.log.info("Purged Empty Accounts.");
		}
		
		if (getConfig().getBoolean("General.ExpireOnStartup")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.transactionTable + " WHERE expired = 'false' AND expiredate = '" + Methods.getCurrentDate() + "';");
			try {
				if (rs2.next()) {
					String pack2 = rs2.getString("package");
					String user = rs2.getString("player");

					List<String> commands = getConfig().getStringList("packages." + pack2 + ".expirecommands");
					for (String cmd : commands) {
						getServer().dispatchCommand(getServer().getConsoleSender(), cmd.replace("%player", user));
					}
					DBConnection.sql.modifyQuery("UPDATE " + DBConnection.transactionTable + " SET expired = 'true' WHERE player = '" + user + "' AND expiredate = '" + Methods.getCurrentDate() + "' AND package = '" + pack2 + "';");
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
			log.info("Main Config not found. Generating.");
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

	@SuppressWarnings("static-access")
	public void configCheck() {
		// Normal Config
		int ConfigVersion = getConfig().getInt("General.ConfigVersion");
		if (ConfigVersion != 181) {
			this.log.info("Config is not up to date! Updating.");
			// General
			if (!getConfig().contains("General.AutoCreateAccounts")) {
				getConfig().set("General.AutoCreateAccounts", true);
			}
			if (!getConfig().contains("General.SignMessage")) {
				getConfig().set("General.SignMessage", "Premium");
			}
			if (!getConfig().contains("General.Transferrable")) {
				getConfig().set("General.Transferrable", true);
			}
			if (!getConfig().contains("General.ExpireOnStartup")) {
				getConfig().set("General.ExpireOnStartup", true);
			}
			if (!getConfig().contains("General.AutoFillSigns")) {
				getConfig().set("General.AutoFillSigns", true);
			}
			if (!getConfig().contains("General.SpecificPermissions")) {
				getConfig().set("General.SpecificPermissions", false);
			}
			if (!getConfig().contains("General.PurgeEmptyAccountsOnStartup")) {
				getConfig().set("General.PurgeEmptyAccountsOnStartup", false);
			}
			// MySQL Stuff
			if (!getConfig().contains("MySQL.engine")) {
				getConfig().set("MySQL.engine", "sqlite");
			}
			if (!getConfig().contains("MySQL.SQLiteDB")) {
				getConfig().set("MySQL.MySQLiteDB", "donationpoints.db");
			}
			if (!getConfig().contains("MySQL.host")) {
				getConfig().set("MySQL.host", "localhost");
			}
			if (!getConfig().contains("MySQL.port")) {
				getConfig().set("MySQL.port", 3306);
			}
			if (!getConfig().contains("MySQL.username")) {
				getConfig().set("MySQL.username", "root");
			}
			if (!getConfig().contains("MySQL.password")) {
				getConfig().set("MySQL.password", "");
			}
			if (!getConfig().contains("MySQL.database")) {
				getConfig().set("MySQL.database", "minecraft");
			}
			// Messages
			if (!getConfig().contains("messages.Prefix")) {
				getConfig().set("messages.Prefix", "&7[&aDonationPoints&7] ");
			}
			if (!getConfig().contains("messages.NoPermission")) {
				getConfig().set("messages.NoPermission", "&cYou dont have permission to do that.");
				}
			if (!getConfig().contains("messages.InvalidArguments")) {
				getConfig().set("messages.InvalidArguments", "&cYou have specified an incorrect number of arguments.");
			}
			if (!getConfig().contains("messages.PlayerOnly")) {
				getConfig().set("messages.PlayerOnly", "&cThis command is only available to players.");
			}
			if (!getConfig().contains("messages.ReloadSuccessful")) {
				getConfig().set("messages.ReloadSuccessful", "&cConfig file has been successfully reloaded.");
			}
			if (!getConfig().contains("messages.NoCommandExists")) {
				getConfig().set("messages.NoCommandExists", "&cThat is not a valid DonationPoints command.");
			}
			if (!getConfig().contains("messages.DPConfirm")) {
				getConfig().set("messages.DPConfirm", "&cType &3/dp confirm &cto confirm your purchase of &3%pack &cfor &3%amount points&c.");
			}
			if (!getConfig().contains("messages.DPActivate")) {
				getConfig().set("messages.DPActivate", "&cType &3/dp activate %pack &cto activate your purchase.");
			}
			if (!getConfig().contains("messages.DPSuccessfulActivation")) {
				getConfig().set("messages.DPSuccessfulActivation", "&cYou have activated your &3%pack &cpackage.");
			}
			if (!getConfig().contains("messages.DPFailedActivation")) {
				getConfig().set("messages.DPFailedActivation", "&cYou do not have a &3%pack &cto activate.");
			}
			if (!getConfig().contains("messages.ExpireDate")) {
				getConfig().set("messages.ExpireDate", "&cYour &3%pack &cis set to expire on &3%expiredate&c.");
			}
			if (!getConfig().contains("messages.DPGive")) {
				getConfig().set("messages.DPGive", "&3%amount &chas been added to &3%player &cbalance");
			}
			if (!getConfig().contains("messages.DPTake")) {
				getConfig().set("messages.DPTake", "&3%amount &chas been removed from &3player &c balance.");
			}
			if (!getConfig().contains("messages.DPSet")) {
				getConfig().set("messages.DPSet", "&3%player &caccount has been set to &3%amount points&c.");
			}
			if (!getConfig().contains("messages.NoAccount")) {
				getConfig().set("messages.NoAccount", "&cNo account found for &3%player&c.");
			}
			if (!getConfig().contains("messages.AccountCreated")) {
				getConfig().set("messages.AccountCreated", "&cAn account has been created for &3%player&c.");
			}
			if (!getConfig().contains("messages.AccountAlreadyExists")) {
				getConfig().set("messages.AccountAlreadyExists", "&cAn account already exists for &3player&c.");
			}
			if (!getConfig().contains("messages.NoTransfer")) {
				getConfig().set("messages.NoTransfer", "&cUnable to make the transfer.");
			}
			if (!getConfig().contains("messages.TransferOff")) {
				getConfig().set("messages.TransferOff", "&cThis server does not have transfers enabled.");
			}
			if (!getConfig().contains("messages.TransferSent")) {
				getConfig().set("messages.TransferSent", "&cYou have sent &3%amount &cto &3%player&c.");
			}
			if (!getConfig().contains("messages.TransferReceive")) {
				getConfig().set("messages.TransferReceive", "&cYou have received &3%amount &cfrom &3%player.");
			}
			if (!getConfig().contains("messages.PlayerBalance")) {
				getConfig().set("messages.PlayerBalance", "&cYou have a balance of &3%amount&c.");
			}
			if (!getConfig().contains("messages.OtherBalance")) {
				getConfig().set("messages.OtherBalance", "&3%player &chas a balance of &3%amount&c.");
			}
			if (!getConfig().contains("messages.NoPurchaseStarted")) {
				getConfig().set("messages.NoPurchaseStarted", "&cYou have not yet started a purchase.");
			}
			if (!getConfig().contains("messages.PurchaseSuccessful")) {
				getConfig().set("messages.PurchaseSuccessful", "&cYou have purchased &3%pack &cfor &3%amount points&c.");
			}
			if (!getConfig().contains("messages.NeedActivation")) {
				getConfig().set("messages.NeedActivation", "&cYou have already purchased &3%pack &cbut it is not active.");
			}
			if (!getConfig().contains("messages.LimitReached")) {
				getConfig().set("messages.LimitReached", "&cYou have already reached the limit of &3%limit &cpurchases of &3%pack&c.");
			}
			if (!getConfig().contains("messages.PackageActivated")) {
				getConfig().set("messages.PackageActivated", "&cYou have successfully activated your &3%pack &cpackage");
			}
			if (!getConfig().contains("messages.InvalidPackage")) {
				getConfig().set("messages.InvalidPackage", "&cUnable to find that package.");
			}
			if (!getConfig().contains("messages.NotEnoughPoints")) {
				getConfig().set("messages.NotEnoughPoints", "&cYou don't have enough points to make that purchase.");
			}
			if (!getConfig().contains("messages.DPPrerequisite")) {
				getConfig().set("messages.DPPrerequisite", "&cYou must purchase &3%pack &cbefore you can purchase this one.");
			}
			if (!getConfig().contains("packages.ExamplePackage.price")) {
				getConfig().set("packages.ExamplePackage.price", 100);
			}
			if (!getConfig().contains("packages.ExamplePackage.description")) {
				getConfig().set("packages.ExamplePackage.description", "This is an example package.");
			}
			if (!getConfig().contains("packages.ExamplePackage.haslimit")) {
				getConfig().set("packages.ExamplePackage.haslimit", false);
			}
			if (!getConfig().contains("packages.ExamplePackage.limit")) {
				getConfig().set("packages.ExamplePackage.limit", 3);
			}
			if (!getConfig().contains("packages.ExamplePackage.activateimmediately")) {
				getConfig().set("packages.ExamplePackage.activateimmediately", true);
			}
			if (!getConfig().contains("packages.ExamplePackage.expires")) {
				getConfig().set("packages.ExamplePackage.expires", false);
			}
			if (!getConfig().contains("packages.ExamplePackage.expiretime")) {
				getConfig().set("packages.ExamplePackage.expiretime", 3);
			}
			if (!getConfig().contains("packages.ExamplePackage.commands")) {
				getConfig().createSection("packages.ExamplePackage.commands");
				List<String> examplecommands = getConfig().getStringList("packages.ExamplePackage.commands");;
				examplecommands.add("say %player has purchased the example package.");
				getConfig().set("packages.ExamplePackage.commands", examplecommands);
			}
			if (!getConfig().contains("packages.ExamplePackage.expirecommands")) {
				getConfig().createSection("packages.ExamplePackage.expirecommands");
				List<String> exampleexpirecommands = getConfig().getStringList("packages.ExamplePackage.expirecommands");;
				exampleexpirecommands.add("say %player's example package has expired.");
				getConfig().set("packages.ExamplePackage.expirecommands", exampleexpirecommands);
			}
			if (!getConfig().contains("packages.ExamplePackage.requireprerequisite")) {
				getConfig().set("packages.ExamplePackage.requireprerequisite", false);
			}
			if (!getConfig().contains("packages.ExamplePackage.prerequisite")) {
				getConfig().set("packages.ExamplePackage.prerequisite", "");
			}
			if (!getConfig().contains("MySQL.PlayerTable")) {
				getConfig().set("MySQL.PlayerTransaction", "dp_players");
			}
			if (!getConfig().contains("MySQL.TransactionTable")) {
				getConfig().set("MySQL.TransactionTable", "dp_transactions");
			}
			if (!getConfig().contains("MySQL.ItemFrameTable")) {
				getConfig().set("MySQL.ItemFrameTable", "dp_frames");
			}
			if (!getConfig().contains("General.ServerName")) {
				getConfig().set("General.ServerName", "MinecraftServer");
			}
			getConfig().set("General.ConfigVersion", 181);
			saveConfig();
		}
	}
}
