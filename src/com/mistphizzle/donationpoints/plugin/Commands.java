package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;

public class Commands {

	DonationPoints plugin;

	public Commands(DonationPoints instance) {
		this.plugin = instance;
		init();
	}

	private void init() {
		PluginCommand donationpoints = plugin.getCommand("donationpoints");
		CommandExecutor exe;

		exe = new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
				if (args.length < 1) {
					s.sendMessage("§eDonationPoints Commands");
					if (s.hasPermission("donationpoints.create")) {
						s.sendMessage("§3/dp create §f - Creates a points account for you.");
					} if (s.hasPermission("donationpoints.balance")) {
						s.sendMessage("§3/dp balance§f - Check your balance.");
					} if (s.hasPermission("donationpoints.give")) {
						s.sendMessage("§3/dp give <player> <amount>§f - Gives a player points.");
					} if (s.hasPermission("donationpoints.take")) {
						s.sendMessage("§3/dp take <player> <amount>§f - Takes a player's points.");
					} if (s.hasPermission("donationpoints.set")) {
						s.sendMessage("§3/dp set <player> <amount>§f - Sets a player's points.");
					} if (s.hasPermission("donationpoints.update")) {
						s.sendMessage("§3/dp update§f - Checks if an update for DonationPoints is available.");
					} if (s.hasPermission("donationpoints.reload")) {
						s.sendMessage("§3/dp reload §f- Reloads Configuration / Packages.");
					}
					return true;
				} else if (args[0].equalsIgnoreCase("reload") && s.hasPermission("donationpoints.reload")) {
					plugin.reloadConfig();
					s.sendMessage("§aConfig / Packages reloaded.");
				} else if (args[0].equalsIgnoreCase("balance") && s.hasPermission("donationpoints.balance")) {
					if (args.length == 1) {
						ResultSet rs2 = DBConnection.query("SELECT balance FROM points_players WHERE player = '" + s.getName() + "';", false);
						try {
							if (rs2.next()) {
								do {
									s.sendMessage("§aYou currently have §3" + rs2.getDouble("balance") + "§a points.");
								} while (rs2.next());
							} else if (!rs2.next()) {
								s.sendMessage("§cYour balance can't be found!");
								s.sendMessage("§cCreate an account using: §3/dp create");
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					} else if (args.length == 2 && s.hasPermission("donationpoints.balance.others")) {
						ResultSet rs2 = DBConnection.query("SELECT balance FROM points_players WHERE player = '" + args[1] + "';", false);
						try {
							if (rs2.next()) {
								do {
									s.sendMessage("§a" + args[1] + " has §3 " + rs2.getDouble("balance") + "§a points.");
								} while (rs2.next());
							} else if (!rs2.next()) {
								s.sendMessage("§cWas unable to find a balance for §a " + args[1]);	
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				} else if (args[0].equalsIgnoreCase("create") && s.hasPermission("donationpoints.create")) {
					if (args.length == 1) {
						ResultSet rs2 = DBConnection.query("SELECT player FROM points_players WHERE player = '" + s.getName() + "';", false);
						try {
							if (rs2.next()) {
								do {
									s.sendMessage("§cA balance was found for you already. We will not create a new one.");
								} while (rs2.next());
							} else if (!rs2.next()) {
								DBConnection.query("INSERT INTO points_players(player, balance) VALUES ('" + s.getName() + "', 0)", true);
								s.sendMessage("§aYour account has been created.");
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					} if (args.length == 2 && s.hasPermission("donationpoints.create.others")) {
						ResultSet rs2 = DBConnection.query("SELECT player FROM points_players WHERE player = '" + args[1] + "';", false);
						try {
							if (rs2.next()) {
								do {
									s.sendMessage("§3" + args[1] + " §calready has a balance.");
								} while (rs2.next());
							} else if (!rs2.next()) {
								DBConnection.query("INSERT INTO points_players(player, balance) VALUES ('" + args[1] + "', 0)", true);
								s.sendMessage("§aCreated an account for §3" + args[1]);
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				} else if (args[0].equalsIgnoreCase("give") && args.length == 3 && s.hasPermission("donationpoints.give")) {
					ResultSet rs2 = DBConnection.query("UPDATE points_players SET balance = balance + " + args[2] + " WHERE player = '" + args[1] + "';", true);
					s.sendMessage("§aYou have given §3" + args[1] + " §aa total of §3" + args[2] + " §apoints.");
				} else if (args[0].equalsIgnoreCase("take") && args.length ==3 && s.hasPermission("donationpoints.take")) {
					ResultSet rs2 = DBConnection.query("UPDATE points_players SET balance = balance - " + args[2] + " WHERE player = '" + args[1] + "';", true);
					s.sendMessage("§aYou have taken §3" + args[2] + "§a points from §3" + args[1]);
				} else if (args[0].equalsIgnoreCase("confirm") && s.hasPermission("donationpoints.confirm")) {
					if (PlayerListener.purchases.containsKey(s.getName())) {
						String pack2 = PlayerListener.purchases.get(s.getName());
						Double price2 = plugin.getConfig().getDouble("packages." + pack2 + ".price");
						DBConnection.query("UPDATE points_players SET balance = balance - " + price2 + " WHERE player = '" + s.getName() + "';", true);
						List<String> commands = plugin.getConfig().getStringList("packages." + pack2 + ".commands");
						for (String cmd : commands) {
							plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("%player", s.getName()));
						}
						s.sendMessage("§aYou have just purchased §3" + pack2 + "§a for §3" + price2 + "§a points.");
						s.sendMessage("§aYour balance has been updated.");
						s.sendMessage("§aTransaction Complete.");
						PlayerListener.purchases.remove(s.getName());
						if (plugin.getConfig().getBoolean("General.LogTransactions", true)) {
							DBConnection.query("INSERT INTO points_transactions(player, package, price) VALUES ('" + s.getName() + "', '" + pack2 + "', " + price2 + ")", true);
							plugin.log.info("[DonationPoints] " + s.getName() + " has made a purchase. It has been logged to points_transactions.");
						} else {
							plugin.log.info("[DonationPoints] " + s.getName() + " has made a purchase. Not logged to points_transactions.");
						}
					} else {
						s.sendMessage("§cDoesn't look like you have started a transaction.");
					}
				} else if (args[0].equalsIgnoreCase("set") && s.hasPermission("donationpoints.set")) {
					ResultSet rs2 = DBConnection.query("UPDATE points_players SET balance = " + args[2] + " WHERE player = '" + args[1] + "';", true);
					s.sendMessage("§aYou have set §3" + args[1] + "'s §abalance to §3" + args[2]);
				} else if (args[0].equalsIgnoreCase("update")) {
					if (s.hasPermission("donationpoints.update") && UpdateChecker.updateNeeded()) {
						s.sendMessage("§eYour server is not running the same version of DonationPoints as the latest file on Bukkit!");
						s.sendMessage("§ePerhaps it's time to upgrade?");
					} else if (s.hasPermission("donationpoints.update") && !UpdateChecker.updateNeeded()) {
						s.sendMessage("§eYou are running the same DonationPoints version as the one on Bukkit!");
						s.sendMessage("§eNo need for an update at this time. :)");
					} else {
						s.sendMessage("§cYou don't have permission for that.");
					}
				} else {
					s.sendMessage("Not a valid DonationPoints command / Not Enough Permissions.");
				} return true;
			}
		}; donationpoints.setExecutor(exe);
	}

}
