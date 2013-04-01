package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

	public static String SignMessage;

	public static DonationPoints plugin;

	public PlayerListener(DonationPoints instance) {
		plugin = instance;
	}

	public static HashMap<String, String> purchases = new HashMap();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (block.getState() instanceof Sign) {
			Sign s = (Sign) block.getState();
			String signline1 = s.getLine(0);
			if (signline1.equalsIgnoreCase("[" + SignMessage + "]")
					&& event.getAction().equals(Action.LEFT_CLICK_BLOCK)
					&& block.getType() == Material.WALL_SIGN) {
				if (!player.hasPermission("donationpoints.sign.use")) {
					player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
				}
				if (player.hasPermission("donationpoints.sign.use")) {
					String purchasedPack = s.getLine(1);
					Double price = plugin.getConfig().getDouble("packages." + purchasedPack + ".price");
					player.sendMessage(Commands.Prefix + "§cRight Clicking this sign will allow you to purchase §3" + purchasedPack + "§c for §3" + price + "§c.");
				}
			}
			if (signline1.equalsIgnoreCase("[" + SignMessage + "]")
					&& event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
					&& block.getType() == Material.WALL_SIGN) {
				if (!player.hasPermission("donationpoints.sign.use")) {
					player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
				}
				if (player.hasPermission("donationpoints.sign.use")) {
					String purchasedPack = s.getLine(1);
					Double price = plugin.getConfig().getDouble("packages." + purchasedPack + ".price");
					String username = player.getName().toLowerCase();
					Double balance = Methods.getBalance(username);
					if (player.hasPermission("donationpoints.free")) {
						purchases.put(username, purchasedPack);
						if (purchases.containsKey(username)) {
							String price2 = price.toString();
							player.sendMessage(Commands.Prefix + "§cUse §3/dp confirm §cto confirm.");
						}
					} else {
						if (!(balance >= price)) {
							player.sendMessage(Commands.Prefix + Commands.NotEnoughPoints);
						} else if (balance >= price) {
							purchases.put(username, purchasedPack);
							if (purchases.containsKey(username)) {
								String price2 = price.toString();
								player.sendMessage(Commands.Prefix + Commands.DPConfirm.replace("%pack", purchasedPack).replace("%amount", price2));
							}
						}
					}
					event.setUseItemInHand(Result.DENY);
					event.setUseInteractedBlock(Result.DENY);
				}
			}
		}
	}

	@EventHandler
	public void PlayerJoinEvent(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		String user = p.getName().toLowerCase();
		if (plugin.getConfig().getBoolean("General.AutoCreateAccounts", true)) {
			if (!Methods.hasAccount(user)) {
				Methods.createAccount(user);
				plugin.log.info("Created an account for " + user);
			}
		}
		ResultSet rs2 = DBConnection.sql.readQuery("SELECT * FROM dp_transactions WHERE player = '" + user + "' AND expiredate = '" + Methods.getCurrentDate() + "';");
		try {
			if (rs2.next()) {
				String pack2 = rs2.getString("package");

				List<String> commands = plugin.getConfig().getStringList("packages." + pack2 + ".expirecommands");
				for (String cmd : commands) {
					plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("%player", user));
				}
				DBConnection.sql.modifyQuery("UPDATE dp_transactions SET expired = 'true' WHERE player = '" + user + "' AND expiredate = '" + Methods.getCurrentDate() + "' AND package = '" + pack2 + "';");
			} else if (!rs2.next()) {
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
