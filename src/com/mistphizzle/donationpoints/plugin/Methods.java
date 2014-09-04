package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.mistphizzle.donationpoints.plugin.Objects.Transaction;

public class Methods {

	static DonationPoints plugin;

	public Methods(DonationPoints instance) {
		plugin = instance;
	}

	public static HashMap<UUID, Double> accounts = new HashMap<UUID, Double>();

	public static void loadAccounts() {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.playerTable);
		try {
			/*
			 * We'll first make sure UUIDs exist for all players in the thing.
			 */
			if (!rs2.next()) {
				return;
			}
			do {
				if (rs2.getString("uuid") == null || rs2.getString("uuid").equalsIgnoreCase("")) {
					// There is no UUID in the database.
					String playername = rs2.getString("player");
					UUID uuid = Bukkit.getOfflinePlayer(playername).getUniqueId();
					if (uuid != null) {
						// They have played on the server before.
						DBConnection.sql.modifyQuery("UPDATE " + DBConnection.playerTable + " SET uuid = '" + uuid + "' WHERE player = '" + playername + "'"); // Updates the table to include the player's UUID.
						accounts.put(uuid, rs2.getDouble("balance"));
					}
				} else {
					accounts.put(UUID.fromString(rs2.getString("uuid")), rs2.getDouble("balance"));
				}
			} while (rs2.next());
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Double getBalance(UUID uuid) {
		return accounts.get(uuid);
	}

	public static boolean hasAccount(UUID uuid) {
		return accounts.containsKey(uuid);
	}

	public static void createAccount(UUID uuid) {
		DBConnection.sql.modifyQuery("INSERT INTO " + DBConnection.playerTable + "(uuid, player, balance) VALUES ('" + uuid.toString() + "', '" + Bukkit.getPlayer(uuid).getName() + "', 0)");
		accounts.put(uuid, 0.0);
	}

	public static void addPoints (Double amount, UUID uuid) {
		Double balance = accounts.get(uuid);
		DBConnection.sql.modifyQuery("UPDATE " + DBConnection.playerTable + " SET balance = balance + " + amount + " WHERE uuid = '" + uuid.toString() + "';");
		accounts.put(uuid, balance + amount);
	}

	public static void removePoints (Double amount, UUID uuid) {
		Double balance = accounts.get(uuid);
		DBConnection.sql.modifyQuery("UPDATE " + DBConnection.playerTable + " SET balance = balance - " + amount + " WHERE uuid = '" + uuid.toString() + "';");
		accounts.put(uuid, balance - amount);
	}
	public static void setPoints (Double amount, UUID uuid) {
		DBConnection.sql.modifyQuery("UPDATE " + DBConnection.playerTable + " SET balance = " + amount + " WHERE uuid = '" + uuid.toString() + "';");
		accounts.put(uuid, amount);
	}

	public static void logTransaction(Transaction tran) {
		DBConnection.sql.modifyQuery("INSERT INTO " + DBConnection.transactionTable + "(player, package, price, date, activated, expires, expiredate, expired, server) VALUES ('" + Transaction.getPlayer() + "', '" + Transaction.getPackageName() + "', " + Transaction.getPrice() + ", '" + Transaction.getDate() + "', '" + Transaction.getActivated() + "', '" + Transaction.getExpires() + "', '" + Transaction.getExpireDate() + "', '" + Transaction.hasExpired()+ "', '" + Transaction.getServer() + "');");
	}

	public static void logTransaction(String player, Double price, String packageName, String date, String activated, String expires, String expiredate, String expired, String server) {
		DBConnection.sql.modifyQuery("INSERT INTO " + DBConnection.transactionTable + "(player, package, price, date, activated, expires, expiredate, expired, server) VALUES ('" + player + "', '" + packageName + "', " + price + ", '" + date + "', '" + activated + "', '" + expires + "', '" + expiredate + "', '" + expired + "', '" + server + "');");
	}

	public static int getTotalPackagePurchased(String player, String pack) {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.transactionTable + " WHERE player = '" + player.toLowerCase() + "' AND package = '" + pack + "'");
		int total = 0;
		try {
			while (rs2.next()) {
				total++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return total;
	}

	public static boolean doesPackageExpire(String pack) {
		if (plugin.getConfig().getInt("packages." + pack + ".ExpireTime") != 0) return true;
		return false;
	}

	public static Date getCurrentDateAsDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String date2 = dateFormat.format(date);
		try {
			return dateFormat.parse(date2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	public static void checkForExpiredPackages() {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.transactionTable + " WHERE expired = 'false';");
		try {
			if (!rs2.next()) return;
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			do {
				String expireDate = rs2.getString("expiredate").replaceAll("-", "/");
				if ("expiredate" != null) { // The package will expire.
					Date currentDate = Methods.getCurrentDateAsDate();
					Date expireDate2 = null;
					try {
						expireDate2 = dateFormat.parse(expireDate);
					} catch (ParseException e) {
						DonationPoints.log.info("Unable to parse date: " + expireDate + ". Skipping Entry.");
					}
					if (expireDate2 == null) continue;
					long timeUntilExpiration = (expireDate2.getTime() - currentDate.getTime()); // Returns time
					if (timeUntilExpiration <= 0) {
						String user = rs2.getString("player");
						String pack = rs2.getString("package");
						List<String> commands = plugin.getConfig().getStringList("packages." + pack + ".expirecommands");
						for (String cmd: commands) {	
							plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("%player", user));
						}
						DBConnection.sql.modifyQuery("UPDATE " + DBConnection.transactionTable + " SET expired = 'true' WHERE player = '" + user + "' AND package = '" + pack + "';");
						DonationPoints.log.info("Expired " + user + "'s " + pack + " package.");
					}
				}
			} while (rs2.next());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean NeedActive(String player, String packageName) {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.transactionTable + " WHERE player LIKE '" + player + "' AND package = '" + packageName + "' AND activated = 'false';");
		try {
			if (rs2.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static String getCurrentDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String colorize(String message) {
		return message.replaceAll("(?i)&([a-fk-or0-9])", "\u00A7$1");
	}

	public static boolean hasPurchased(String player, String packName, String server) {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.transactionTable + " WHERE player = '" + player + "' AND package = '" + packName + "' AND SERVER = '" + server + "';");
		try {
			if (rs2.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static double roundTwoDecimals(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
	}

	public static void purgeEmptyAccounts() {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.playerTable + " WHERE balance = 0");
		try {
			if (rs2.next()) {
				DBConnection.sql.modifyQuery("DELETE FROM " + DBConnection.playerTable + " WHERE balance = 0");
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static boolean getPackageExists(String pack) {
		if (DonationPoints.instance.getConfig().getDouble("packages." + pack + ".price") == 0) {
			return false;
		}
		return true;
	}

	public static void linkFrame(String pack, Double x, Double y, Double z, String world, String server) {
		DBConnection.sql.modifyQuery("INSERT INTO " + DBConnection.frameTable + " (x,y,z,world,package,server) VALUES ('" + x + "','" + y + "','" + z + "','" + world + "','" + pack + "', '" + server + "');");
	}

	public static boolean isFrameLinked(Double x, Double y, Double z, String world, String server) {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.frameTable + " WHERE x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "' AND world = '" + world + "' AND server = '" + server + "';");
		try {
			if (rs2.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static String getLinkedPackage(Double x, Double y, Double z, String world, String server) {
		String linkedPack = null;
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT package FROM " + DBConnection.frameTable + " WHERE x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "' AND world = '" + world + "' AND server = '" + server + "';");
		try {
			if (rs2.next()) {
				linkedPack = rs2.getString("package");
				return linkedPack;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return linkedPack;
	}

	public static void deleteAccount(UUID uuid) {
		DBConnection.sql.modifyQuery("DELETE FROM " + DBConnection.playerTable + " WHERE uuid = '" + uuid.toString() + "';");
		accounts.remove(uuid);
	}

	public static void unlinkFrame(Double x, Double y, Double z, String world, String server) {
		DBConnection.sql.modifyQuery("DELETE FROM " + DBConnection.frameTable + " WHERE x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "' AND world = '" + world + "' AND server = '"+ server + "';");
	}

	public static boolean hasInventorySpace(Player player, int requiredSlots) {
		int emptySlots = 0;
		for (ItemStack i : player.getInventory().getContents()) {
			if (i == null) {
				emptySlots++;
			}
		}
		if (emptySlots >= requiredSlots) {
			return true;
		}
		return false;
	}

	public static Economy econ;
	public static boolean setupEconomy() {
		if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) return false;

		econ = rsp.getProvider();
		return econ != null;
	}

	public static boolean hasPermission(CommandSender player, String permission) {
		return DonationPoints.permission.has(player, permission);
	}

	public static void createExamplePackage() {
		FileConfiguration config = plugin.getConfig();

		config.set("packages.ExamplePackage.UseVaultEconomy", false);
		config.set("packages.ExamplePackage.price", 100);
		config.set("packages.ExamplePackage.description", "This is an example package.");
		config.set("packages.ExamplePackage.limit", 0);
		config.set("packages.ExamplePackage.ActivateImmediately", true);
		config.set("packages.ExamplePackage.ExpireTime", 0);

		List<String> restrictToWorlds = new ArrayList<String>();
		restrictToWorlds.add("world");
		config.set("packages.ExamplePackage.RestrictToWorlds", restrictToWorlds);

		List<String> activationCommands = new ArrayList<String>();
		activationCommands.add("say %player has purchased the Example package.");
		activationCommands.add("p:me purchased the Example Package.");
		config.set("packages.ExamplePackage.ActivationCommands", activationCommands);

		List<String> expirationCommands = new ArrayList<String>();
		expirationCommands.add("say %player's Example Package has expired.");
		config.set("packages.ExamplePackage.ExpirationCommands", expirationCommands);

		List<String> preRequisites = new ArrayList<String>();
		config.set("packages.ExamplePackage.PreRequisites", preRequisites);
		config.set("packages.ExamplePackage.RequiredInventorySpace", 0);
		plugin.saveConfig();
	}

	public static boolean isPackMisconfigured(String packageName) {
		FileConfiguration config = plugin.getConfig();

		if (config.get("packages." + packageName + ".UseVaultEconomy") == null) return true;
		if (config.get("packages." + packageName + ".price") == null) return true;
		if (config.getDouble("packages." + packageName + ".price") < 0) return true;
		if (config.getString("packages." + packageName + ".description") == null) return true;
		if (config.get("packages." + packageName + ".limit") == null) return true;
		if (config.getInt("packages." + packageName + ".limit") < 0) return true;
		if (config.get("packages." + packageName + ".ActivateImmediately") == null) return true;
		if (config.get("packages." + packageName + ".ExpireTime") == null) return true;
		if (config.getInt("packages." + packageName + ".ExpireTime") < 0) return true;
		if (config.get("packages." + packageName + ".ActivationCommands") == null) return true;
		return false;
	}
}
