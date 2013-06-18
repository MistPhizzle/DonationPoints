package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Methods {

	DonationPoints plugin;

	public Methods(DonationPoints instance) {
		plugin = instance;
	}

	public static Double getBalance(String string) {
		if (DBConnection.engine.equalsIgnoreCase("mysql") | DBConnection.engine.equalsIgnoreCase("sqlite")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT balance FROM " + DBConnection.playerTable + " WHERE player LIKE '" + string.toLowerCase() + "';");
			try {
				if (rs2.next()) {
					Double balance = rs2.getDouble("balance");
					Double balance2 = Methods.roundTwoDecimals(balance);
					return balance2;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static boolean hasAccount(String string) {
		if (DBConnection.engine.equalsIgnoreCase("mysql") | DBConnection.engine.equalsIgnoreCase("sqlite")) {
			ResultSet rs2 = DBConnection.sql.readQuery("SELECT player FROM " + DBConnection.playerTable + " WHERE player = '" + string.toLowerCase() + "';");
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
		DBConnection.sql.modifyQuery("INSERT INTO " + DBConnection.playerTable + "(player, balance) VALUES ('" + string.toLowerCase() + "', 0)");
	}

	public static void addPoints (Double amount, String string) {
		DBConnection.sql.modifyQuery("UPDATE " + DBConnection.playerTable + " SET balance = balance + " + amount + " WHERE player = '" + string.toLowerCase() + "';");
	}

	public static void removePoints (Double amount, String string) {
		DBConnection.sql.modifyQuery("UPDATE " + DBConnection.playerTable + " SET balance = balance - " + amount + " WHERE player = '" + string.toLowerCase() + "';");
	}
	public static void setPoints (Double amount, String string) {
		DBConnection.sql.modifyQuery("UPDATE " + DBConnection.playerTable + " SET balance = " + amount + " WHERE player = '" + string.toLowerCase() + "';");
	}

	public static void logTransaction(String player, Double price, String packageName, String date, String activated, String expires, String expiredate, String expired) {
		DBConnection.sql.modifyQuery("INSERT INTO " + DBConnection.transactionTable + "(player, package, price, date, activated, expires, expiredate, expired) VALUES ('" + player + "', '" + packageName + "', " + price + ", '" + date + "', '" + activated + "', '" + expires + "', '" + expiredate + "', '" + expired + "')");
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
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String colorize(String message) {
		return message.replaceAll("(?i)&([a-fk-or0-9])", "\u00A7$1");
	}

	public static boolean hasPurchased(String player, String packName) {
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM " + DBConnection.transactionTable + " WHERE player LIKE '" + player + "' AND package = '" + packName + "';");
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
}
