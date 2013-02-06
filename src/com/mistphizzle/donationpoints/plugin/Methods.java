package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

public class Methods {
	
	public static DonationPoints plugin;
	
	public Methods(DonationPoints instance) {
		Methods.plugin = instance;
	}
	
	public static String engine;
	
	public static Double getBalance(String string) {
		if (engine.equalsIgnoreCase("mysql") | engine.equalsIgnoreCase("sqlite")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT balance FROM points_players WHERE player = '" + string.toLowerCase() + "';");
			try {
				if (rs2.next()) {
					Double balance = rs2.getDouble("balance");
					return balance;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static boolean hasAccount(String string) {
		if (engine.equalsIgnoreCase("mysql") | engine.equalsIgnoreCase("sqlite")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT player FROM points_players WHERE player = '" + string.toLowerCase() + "';");
			try {
				if (rs2.next()) {
					return true;
				} else {
					return false;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	public static void createAccount(String string) {
			DBConnection.sql.modifyQuery("INSERT INTO points_players(player, balance) VALUES ('" + string.toLowerCase() + "', 0)");
	}
	
	public static void addPoints (Double amount, String string) {
		DBConnection.sql.modifyQuery("UPDATE points_players SET balance = balance + " + amount + " WHERE player = '" + string.toLowerCase() + "';");
	}
	
	public static void removePoints (Double amount, String string) {
		DBConnection.sql.modifyQuery("UPDATE points_players SET balance = balance - " + amount + " WHERE player = '" + string.toLowerCase() + "';");
	}
	public static void setPoints (Double amount, String string) {
		DBConnection.sql.modifyQuery("UPDATE points_players SET balance = " + amount + " WHERE player = '" + string.toLowerCase() + "';");
	}
	
	public static void logTransaction(String player, Double price, String packageName) {
		DBConnection.sql.modifyQuery("INSERT INTO points_transactions(player, package, price) VALUES ('" + player.toLowerCase() + "', '" + packageName + "', " + price + ")");
	}
	
	public static ResultSet getUnprocessedPurchases() {
		return DBConnection.sql.readQuery("SELECT * FROM dp_purchases WHERE processed='false'");
	}
	
	public static ResultSet getUnexpiredPurchases() {
		return DBConnection.sql.readQuery("SELECT * FROM dp_purchases WHERE expired = 'false'");
	}
	
	public static String getExpiresDate(String packagename) throws java.text.ParseException {
		Integer days = plugin.getPackagesConfig().getInt("packages." + packagename + ".expires");
		if (!(days == 0)) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar c = Calendar.getInstance();
			try {
				c.setTime(sdf.parse(getCurrentDate()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			c.add(Calendar.DATE, days);
			String exp = sdf.format(c.getTime());
			return exp;
		}
		return null;
	}
	
	public static String getCurrentDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	public static void noPermissionMessage(CommandSender s) {
		s.sendMessage("§7[§aDonationPoints§7] §cYou don't have permission to do that!");
	}
}
