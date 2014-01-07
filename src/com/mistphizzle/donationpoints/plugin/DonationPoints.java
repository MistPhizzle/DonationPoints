package com.mistphizzle.donationpoints.plugin;

import java.io.IOException;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

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

	// Commands
	Commands cmd;

	// Events
	private final SignListener signListener = new SignListener(this);
	private final PlayerListener playerListener = new PlayerListener(this);

	@Override
	public void onEnable() {
		instance = this;
		DonationPoints.log = this.getLogger();
		
		setupPermissions();

		configCheck();

		// Events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(signListener, this);
		pm.registerEvents(playerListener, this);

		// Database Variables
		DBConnection.host = getConfig().getString("MySQL.host", "localhost");
		DBConnection.db = getConfig().getString("MySQL.database", "minecraft");
		DBConnection.user = getConfig().getString("MySQL.username", "root");
		DBConnection.pass = getConfig().getString("MySQL.password", "");
		DBConnection.port = getConfig().getInt("MySQL.port", 3306);
		DBConnection.engine = getConfig().getString("MySQL.engine", "sqlite");
		DBConnection.sqlite_db = getConfig().getString("MySQL.SQLiteDB", "donationpoints.db");
		DBConnection.playerTable = getConfig().getString("MySQL.PlayerTable", "dp_players");
		DBConnection.transactionTable = getConfig().getString("MySQL.TransactionTable", "dp_transactions");
		DBConnection.frameTable = getConfig().getString("MySQL.ItemFrameTable", "dp_frames");

		// Other Variables.
		PlayerListener.SignMessage = getConfig().getString("General.SignMessage");
		PlayerListener.Points = getConfig().getString("General.Points");
		SignListener.Points = getConfig().getString("General.Points");
		SignListener.SignMessage = getConfig().getString("General.SignMessage");

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
		Commands.TooLongOnConfirm = Methods.colorize(getConfig().getString("messages.TooLongOnConfirm"));
		Commands.SignLeftClick = Methods.colorize(getConfig().getString("messages.SignLeftClick"));
		Commands.SignLeftClickDescription = Methods.colorize(getConfig().getString("messages.SignLeftClickDescription"));
		Commands.RequiredInventorySpace = Methods.colorize(getConfig().getString("messages.RequiredInventorySpace"));

		DBConnection.init();
		DBConnection.sql.modifyQuery("UPDATE dp_players SET player = lower(player)");

		// Register Commands

		cmd = new Commands(this);
		new Methods(this);

		// Metrics
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit stats.
		}

		if (getConfig().getBoolean("General.PurgeEmptyAccountsOnStartup")) {
			Methods.purgeEmptyAccounts();
			DonationPoints.log.info("Purged Empty Accounts.");
		}
		
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this,  new Runnable() {
			public void run() {
				Methods.checkForExpiredPackages();
			}
		}, 0, getConfig().getInt("General.ExpirationCheckTimer") * 20);

		if (getConfig().getBoolean("General.ExpireOnStartup")) {
			Methods.checkForExpiredPackages();
		}
		
		if (getConfig().get("packages") == null) {
			Methods.createExamplePackage();
		}
		
	}

	@Override
	public void onDisable() {
		DBConnection.sql.close();
	}

	public static DonationPoints getInstance() {
		return instance;
	}

	public void configReload() {
		reloadConfig();
	}

	public void configCheck() {
		// General
		getConfig().addDefault("General.AutoCreateAccounts", true);
		getConfig().addDefault("General.SignMessage", "Premium");
		getConfig().addDefault("General.Points", "Points");
		getConfig().addDefault("General.Transferrable", true);
		getConfig().addDefault("General.ExpireOnStartup", true);
		getConfig().addDefault("General.AutoFillSigns", true);
		getConfig().addDefault("General.SpecificPermissions", false);
		getConfig().addDefault("General.PurgeEmptyAccountsOnStartup", true);
		getConfig().addDefault("General.ServerName", "Minecraft Server");
		getConfig().addDefault("General.ExpirationCheckTimer", 300);
		// MySQL
		getConfig().addDefault("MySQL.engine", "sqlite");
		getConfig().addDefault("MySQL.SQLiteDB", "donationpoints.db");
		getConfig().addDefault("MySQL.host", "localhost");
		getConfig().addDefault("MySQL.port", 3306);
		getConfig().addDefault("MySQL.username", "root");
		getConfig().addDefault("MySQL.password", "");
		getConfig().addDefault("MySQL.database", "minecraft");
		getConfig().addDefault("MySQL.TransactionTable", "dp_transactions");
		getConfig().addDefault("MySQL.PlayerTable", "dp_players");
		getConfig().addDefault("MySQL.ItemFrameTable", "dp_frames");
		// Messages
		getConfig().addDefault("messages.Prefix", "&7[&aDonationPoints&7]");
		getConfig().addDefault("messages.NoPermissions", "&cYou dont have permission to do that.");
		getConfig().addDefault("messages.InvalidArguments", "&cYou have specified an incorrect number of arguments.");
		getConfig().addDefault("messages.PlayerOnly", "&cThis command is only available to players.");
		getConfig().addDefault("messages.ReloadSuccessful", "&cConfig file has been successfully reloaded.");
		getConfig().addDefault("messages.NoCommandExists", "&cThat is not a valid DonationPoints command.");
		getConfig().addDefault("messages.DPConfirm", "&cType &3/dp confirm &cto confirm your purchase of &3%pack &cfor &3%amount points&c.");
		getConfig().addDefault("messages.DPActivate", "&cType &3/dp activate %pack &cto activate your purchase.");
		getConfig().addDefault("messages.DPSuccessfulActivation", "&cYou have activated your &3%pack &cpackage.");
		getConfig().addDefault("messages.DPFailedActivation", "&cYou do not have a &3%pack &cto activate.");
		getConfig().addDefault("messages.ExpireDate", "&cYour &3%pack &cis set to expire on &3%expiredate&c.");
		getConfig().addDefault("messages.DPGive", "&3%amount &chas been added to &3%player &cbalance");
		getConfig().addDefault("messages.DPTake", "&3%amount &chas been removed from &%3players &c balance.");
		getConfig().addDefault("messages.DPSet", "&3%player &caccount has been set to &3%amount points&c.");
		getConfig().addDefault("messages.NoAccount", "&cNo account found for &3%player&c.");
		getConfig().addDefault("messages.AccountCreated", "&cAn account has been created for &3%player&c.");
		getConfig().addDefault("messages.AccountAlreadyExists", "&cAn account already exists for &3player&c.");
		getConfig().addDefault("messages.noTransfer", "&cUnable to make the transfer.");
		getConfig().addDefault("messages.TransferOff", "&cThis server does not have transfers enabled.");
		getConfig().addDefault("messages.TransferSent", "&cYou have sent &3%amount &cto &3%player&c.");
		getConfig().addDefault("messages.TransferReceive", "&cYou have received &3%amount &cfrom &3%player.");
		getConfig().addDefault("messages.PlayerBalance", "&cYou have a balance of &3%amount&c.");
		getConfig().addDefault("messages.OtherBalance", "&3%player &chas a balance of &3%amount&c.");
		getConfig().addDefault("messages.NoPurchaseStarted", "&cYou have not yet started a purchase.");
		getConfig().addDefault("messages.PurchaseSuccessful", "&cYou have purchased &3%pack &cfor &3%amount points&c.");
		getConfig().addDefault("messages.NeedActivation", "&cYou have purchased &3%pack &cfor &3%amount points&c.");
		getConfig().addDefault("messages.TooLongOnConfirm", "&cYou waited too long to confirm your purchase. Purchase canceled.");
		getConfig().addDefault("messages.LimitReached", "&cYou have already reached the limit of &3%limit &cpurchases of &3%pack&c.");
		getConfig().addDefault("messages.PackageActivated", "&cYou have successfully activated your &3%pack &cpackage");
		getConfig().addDefault("messages.InvalidPackage", "&cUnable to find that package.");
		getConfig().addDefault("messages.NotEnoughPoints", "&cYou dont have enough points to make that purchase.");
		getConfig().addDefault("messages.DPPrerequisite", "&cYou must purchase &3%pack &cbefore you can purchase this one.");
		getConfig().addDefault("messages.SignLeftClick", "&cRight click to purchase &3%pack &cfor &3%price points&c.");
		getConfig().addDefault("messages.SignLeftClickDescription", "&7[&cDescription&7] &a%desc");
		getConfig().addDefault("messages.RequiredInventorySpace", "&cYou need at least &3%slot &cinventory slots to purchase this package.");

		// Update Config, Save
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
}
