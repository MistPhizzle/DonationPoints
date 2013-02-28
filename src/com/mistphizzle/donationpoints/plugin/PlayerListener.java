package com.mistphizzle.donationpoints.plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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
	public static HashMap<String, String> purchasedPack = new HashMap();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (block.getState() instanceof Sign) {
			Sign s = (Sign) block.getState();
			String signline1 = s.getLine(0);
			if (signline1.equalsIgnoreCase("[" + SignMessage + "]")
					&& event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
					&& block.getType() == Material.WALL_SIGN) {
				if (!player.hasPermission("donationpoints.sign.use")) {
					player.sendMessage("§cYou don't have permission to use the DonationPoints signs.");
				}
				if (player.hasPermission("donationpoints.sign.use")) {
					String purchasedPack = s.getLine(1);
					Double price = plugin.getConfig().getDouble("packages." + purchasedPack + ".price");
					String username = player.getName().toLowerCase();
					Double balance = Methods.getBalance(username);
					if (!(balance >= price)) {
						player.sendMessage("§cYou don't have enough points for this package.");	
					} else if (balance >= price) {
						purchases.put(username, purchasedPack);
						if (purchases.containsKey(username)) {
							player.sendMessage("§aType §3/dp confirm §ato purchase §3" + purchasedPack + "§a for §3" + price + " points§a.");
						}
					}
						event.setUseItemInHand(Result.DENY);
						event.setUseInteractedBlock(Result.DENY);
				}
			}
		}
	}

	@EventHandler
	public void AutoCreateAccount(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		String user = p.getName().toLowerCase();
		if (plugin.getConfig().getBoolean("General.AutoCreateAccounts", true)) {
			if (!Methods.hasAccount(user)) {
				Methods.createAccount(user);
				plugin.log.info("Created an account for " + user);
			}
		}
	}
}
