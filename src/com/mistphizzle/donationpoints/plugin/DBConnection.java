package com.mistphizzle.donationpoints.plugin;

import com.mistphizzle.donationpoints.sql.Database;
import com.mistphizzle.donationpoints.sql.MySQLConnection;
import com.mistphizzle.donationpoints.sql.SQLite;

public final class DBConnection {

	public static Database sql;

	public static String engine;
	public static String sqlite_db;
	public static String host;
	public static int port;
	public static String db;
	public static String user;
	public static String pass;
	public static String playerTable;
	public static String transactionTable;
	public static String cumulativeTable;

	public static void init() {
		if (engine.equalsIgnoreCase("mysql")) {
			sql = new MySQLConnection(DonationPoints.log, "[DonationPoints] Establishing Database Connection...", host, port, user, pass, db);
			((MySQLConnection) sql).open();
			DonationPoints.log.info("[DonationPoints] Etablishing Database Connection...");

			if (sql.tableExists("points_players")) {
				DonationPoints.log.info("Renaming points_players to " + playerTable + ".");
				sql.modifyQuery("RENAME TABLE points_players TO " + playerTable);
			}
			
			DonationPoints.log.info("Making sure all data is correct.");
			sql.modifyQuery("UPDATE " + playerTable + " SET player = lower(player)");
			
			if (!sql.tableExists(playerTable)) {
				DonationPoints.log.info("Creating " + playerTable + " table.");
				String query = "CREATE TABLE IF NOT EXISTS `" + playerTable + "` ("
						+ "`id` int(32) NOT NULL AUTO_INCREMENT,"
						+ "`player` TEXT(32),"
						+ "`balance` double,"
						+ " PRIMARY KEY (id));";
				sql.modifyQuery(query);
			}

			if (!sql.tableExists(transactionTable)) {
				DonationPoints.log.info("Creating " + transactionTable + " table");
				String query = "CREATE TABLE `" + transactionTable + "` ("
						+ "`id` int(32) NOT NULL AUTO_INCREMENT,"
						+ "`player` TEXT(32),"
						+ "`package` TEXT(255),"
						+ "`price` double,"
						+ "`date` VARCHAR(32),"
						+ "`activated` VARCHAR(32),"
						+ "`expires` VARCHAR(32),"
						+ "`expiredate` VARCHAR(32),"
						+ "`expired` VARCHAR(32),"
						+ " PRIMARY KEY (id));";
				sql.modifyQuery(query);
			}
			
			if (!sql.tableExists(cumulativeTable)) {
				DonationPoints.log.info("Creating " + cumulativeTable + " table");
				String query = "CREATE TABLE `" + cumulativeTable + "` ("
						+ "`id` int(32) NOT NULL AUTO_INCREMENT,"
						+ "`player` TEXT(32),"
						+ "`package` TEXT(255),"
						+ "`points` double,"
						+ " PRIMARY KEY (id));";
				sql.modifyQuery(query);
			}
			/*
			 * Everything below this line is for the sqlite connections.
			 * Will only work if the player has "sqlite" for the engine in their config. 
			 * sqlite can be in any case.
			 * Generates the tables points_players, points_transactions, and points_cumulative and logs them into the console.
			 */
		} else if (engine.equalsIgnoreCase("sqlite")) {
			sql = new SQLite(DonationPoints.log, "[DonationPoints] Establishing SQLite Connection.", sqlite_db, DonationPoints.getInstance().getDataFolder().getAbsolutePath());
			((SQLite) sql).open();
			
			if (sql.tableExists("points_players")) {
				DonationPoints.log.info("Renaming points_players to " + playerTable);
				sql.modifyQuery("RENAME TABLE points_players TO " + playerTable);
			}

			if (!sql.tableExists(playerTable)) {
				DonationPoints.log.info("Creating " + playerTable + " table.");
				String query = "CREATE TABLE `" + playerTable + "` ("
						+ "`player` TEXT(32),"
						+ "`balance` DOUBLE(255));";
				sql.modifyQuery(query);
			}

			if (!sql.tableExists(transactionTable)) {
				DonationPoints.log.info("Creating " + transactionTable + " table.");
				String query = "CREATE TABLE `" + transactionTable + "` ("
						+ "`player` TEXT(32),"
						+ "`package` TEXT(255),"
						+ "`price` DOUBLE(255),"
						+ "`date` STRING(32),"
						+ "`activated` STRING(32),"
						+ "`expires` STRING(32),"
						+ "`expiredate` STRING(32),"
						+ "`expired` STRING(32));";
				
				sql.modifyQuery(query);
			}
			if (!sql.tableExists(cumulativeTable)) {
				DonationPoints.log.info("Creating " + cumulativeTable + " table.");
				String query = "CREATE TABLE `" + cumulativeTable + "` ("
						+ "`player` TEXT(32),"
						+ "`package` TEXT(255),"
						+ "`points` double(255));";
				sql.modifyQuery(query);
			}
		} else {
			DonationPoints.log.info("[DonationPoints] Unknown value for SQL Engine, expected sqlite or mysql.");
		}
	}

}