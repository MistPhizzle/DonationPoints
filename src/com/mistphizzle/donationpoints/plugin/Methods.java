package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;

public class Methods {
	
	public static DonationPoints plugin;
	
	public Methods(DonationPoints instance) {
		this.plugin = instance;
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
	
	public static Double getPackagePrice(String packageName, Double price) {
		plugin.getConfig().getDouble("packages." + packageName + "price");
		return price;
		
	}

}
