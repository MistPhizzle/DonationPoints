package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;

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
					} if (s.hasPermission("donationpoints.reload")) {
						s.sendMessage("§3/dp reload §f- Reloads Configuration / Packages.");
					} if (s.hasPermission("donationpoints.packages")) {
						s.sendMessage("§3/dp packs§f - Show the package sub commands.");
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
									s.sendMessage("§aYou currently have" + rs2.getDouble("balance") + "§3 points.");
								} while (rs2.next());
							} else if (!rs2.next()) {
								s.sendMessage("§cYour balance can't be found!");
								s.sendMessage("§cCreate an account using: §3/dp create");
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					} else if (args.length == 2 && s.hasPermission("darkpoints.balance.others")) {
						ResultSet rs2 = DBConnection.query("SELECT balance FROM points_players WHERE player = '" + args[1] + "';", false);
						try {
							if (rs2.next()) {
								do {
									s.sendMessage("§a" + args[1] + " has §3 " + rs2.getDouble("balance") + "§3 points.");
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
					s.sendMessage("§eYou have given " + args[1] + " §a" + args[2] + "§epoints.");
				} else if (args[0].equalsIgnoreCase("take") && args.length ==3 && s.hasPermission("donationpoints.take")) {
					ResultSet rs2 = DBConnection.query("UPDATE points_players SET balance = balance - " + args[2] + " WHERE player = '" + args[1] + "';", true);
					s.sendMessage("§eYou have taken §a" + args[2] + "§e from " + args[1]);
				} else {
					s.sendMessage("Not a valid DonationPoints command / Not Enough Permissions.");
				} return true;
			}
		}; donationpoints.setExecutor(exe);
	}

}
