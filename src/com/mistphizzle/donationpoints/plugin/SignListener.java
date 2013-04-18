package com.mistphizzle.donationpoints.plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.block.Sign;

public class SignListener implements Listener {

	public static String SignMessage;
	public static DonationPoints plugin;

	public SignListener(DonationPoints instance) {
		plugin = instance;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		Player player = e.getPlayer();
		Block block = e.getBlock();
		
		if (block.getState() instanceof Sign) {
			Sign s = (Sign) block.getState();
			String signline1 = s.getLine(0);
			if (signline1.equalsIgnoreCase("[" + SignMessage + "]") && !DonationPoints.permission.has(player, "donationpoints.sign.break")) {
				if (!DonationPoints.permission.has(player, "donationpoints.sign.break")) {
					player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
				}
				e.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void onSignChance(SignChangeEvent e) {
		if (e.isCancelled()) return;
		if (e.getPlayer() == null) return;
		Player p = e.getPlayer();
		String line1 = e.getLine(0);
		Block block = e.getBlock();
		String pack = e.getLine(1);

		// Permissions
		if (line1.equalsIgnoreCase("[" + SignMessage + "]") && !DonationPoints.permission.has(p, "donationpoints.sign.create")) {
			e.setCancelled(true);
			block.breakNaturally();
			p.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
		} else if (DonationPoints.permission.has(p, "donationpoints.sign.create") && line1.equalsIgnoreCase("[" + SignMessage + "]")) {
			if (block.getType() == Material.SIGN_POST) {
				p.sendMessage(Commands.Prefix + "§cDonationPoints signs must be placed on a wall.");
				block.breakNaturally();
				e.setCancelled(true);
			} if (plugin.getConfig().getString("packages." + pack) == null) {
				e.setCancelled(true);
				p.sendMessage(Commands.Prefix + Commands.InvalidPackage);
				block.breakNaturally();
			} if (e.getLine(1).isEmpty()) {
				e.setCancelled(true);
				p.sendMessage(Commands.Prefix + Commands.InvalidPackage);
				block.breakNaturally();
			} else {
				if (plugin.getConfig().getBoolean("General.AutoFillSigns", true)) {
					Double price = plugin.getConfig().getDouble("packages." + pack + ".price");
					e.setLine(2, (price + " Points"));
				}
				p.sendMessage(Commands.Prefix + "§cYou have created a DonationPoints sign.");
			}
		} 
	}
}
