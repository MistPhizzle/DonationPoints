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
	
	public static Double getBalance(Player p) {
		if (engine.equalsIgnoreCase("mysql") | engine.equalsIgnoreCase("sqlite")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT balance FROM points_players WHERE player = '" + p.getName().toLowerCase() + "';");
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
	
	public static boolean hasAccount(Player p) {
		if (engine.equalsIgnoreCase("mysql") | engine.equalsIgnoreCase("sqlite")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT player FROM points_player WHERE player = '" + p.getName().toLowerCase() + "';");
			try {
				if (rs2.next()) {
					String player = rs2.getString("player");
					return true;
				} else {
					return false;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	public static void createAccountForSelf(Player p) {
			DBConnection.sql.modifyQuery("INSERT INTO points_players(player, balance) VALUES ('" + p.getName().toLowerCase() + "', 0)");
	}

}
