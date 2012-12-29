package com.mistphizzle.donationpoints.plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.block.Sign;

public class SignListener implements Listener {

	public static String SignMessage;
	public static DonationPoints plugin;

	public SignListener(DonationPoints instance) {
		plugin = instance;
	}

	@EventHandler
	public void onSignChance(SignChangeEvent e) {
		if (e.isCancelled()) return;
		if (e.getPlayer() == null) return;
		Player p = e.getPlayer();
		String line1 = e.getLine(0);
		Block block = e.getBlock();
		Sign s = (Sign) block.getState();
		String pack = e.getLine(2);

		// Permissions
		if (line1.equalsIgnoreCase("[" + SignMessage + "]") && !p.hasPermission("donationpoints.sign.create")) {
			e.setCancelled(true);
			block.breakNaturally();
			p.sendMessage("§cYou don't have permission to create DonationPoints signs.");
		} else if (p.hasPermission("donationpoints.sign.create") && line1.equalsIgnoreCase("[" + SignMessage + "]")) {
			if (block.getType() == Material.SIGN_POST) {
				p.sendMessage("§cDonationPoints signs must be placed on a wall.");
				block.breakNaturally();
				e.setCancelled(true);
			} else if (plugin.getConfig().getString("packages." + pack) == null) {
				p.sendMessage("§cThat package does not exist.");
				e.setCancelled(true);
				block.breakNaturally();
			} else {
				p.sendMessage("§aYou have created a DonationPoints sign.");
			}
		} 
	}
}
