package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.mistphizzle.donationpoints.sql.MySQLConnection;

public final class DBConnection {

	public static MySQLConnection con;

	public static String host;
	public static int port;
	public static String db;
	public static String user;
	public static String pass;

	public static void init() {
		DonationPoints.log.info("[DonationPoints] Etablishing Database Connection...");

		try {
			con = new MySQLConnection(host, port, db, user, pass);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		if (con.connect(true)) {
			DonationPoints.log.info("[DonationPoints] Connection Established!");
			if (!con.tableExists(db, "points_players")) {
				query("CREATE TABLE points_players(id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), player TEXT(32), balance DOUBLE)", true);
				DonationPoints.log.info("[DonationPoints] Created points_players table.");
			}
		} else {
			DonationPoints.log.warning("[DonationPoints] MySQL Connection Failed!");
		}
		
//		if (!con.tableExists(db, "points_players")) {
//			String query = "CREATE TABLE IF NOT EXISTS `points_players` ("
//					+ "`id` int(32) NOT NULL AUTO INCREMENT,"
//					+ "`player` varchar(32) NOT NULL,"
//					+ "`balance` double(32) NOT NULL,"
//					+ "PRIMARY KEY (`id`));";
//		}
	}

	public static void disable() {
		con.disconnect();
	}

	public static ResultSet query (String query, boolean modifies) {
		try {
			return con.executeQuery(query, modifies);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

}