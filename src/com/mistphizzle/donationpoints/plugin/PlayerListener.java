package com.mistphizzle.donationpoints.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

	public static String SignMessage;
	public static String Points;

	public static DonationPoints plugin;

	public static int confirmTask;

	public PlayerListener(DonationPoints instance) {
		plugin = instance;
	}

	public static HashMap<String, String> purchases = new HashMap<String, String>();
	public static HashMap<String, String> links = new HashMap<String, String>();

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent e) {
		if (purchases.containsKey(e.getPlayer().getName().toLowerCase())) {
			purchases.remove(e.getPlayer().getName().toLowerCase());
			e.getPlayer().sendMessage(Commands.Prefix + Commands.TooLongOnConfirm);
		}
	}

	@EventHandler 
	public void playerEntityInteract(PlayerInteractEntityEvent event) {
		final Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		if (entity instanceof ItemFrame) {
			Double x = entity.getLocation().getX();
			Double y = entity.getLocation().getY();
			Double z = entity.getLocation().getZ();
			String world = entity.getWorld().getName();
			if (Methods.isFrameLinked(x, y, z, world, Commands.Server)) {
				((ItemFrame) entity).setRotation(Rotation.NONE);
				if (!DonationPoints.permission.has(player, "donationpoints.sign.use")) {
					player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
					event.setCancelled(true);
					return;
				}
				String packName = Methods.getLinkedPackage(x, y, z, world, Commands.Server);
				if (Methods.isPackMisconfigured(packName)) {
					player.sendMessage(Commands.Prefix + ChatColor.RED + "This package is misconfigured. Consult an administrator.");
					return;
				}

				String purchasedPack = packName;
				boolean usesVault;
				boolean hasPreRequisites;
				Set<String> preRequisites = new HashSet<String>();

				if (plugin.getConfig().getStringList("packages." + purchasedPack + ".PreRequisites") == null
						|| plugin.getConfig().getStringList("packages." + purchasedPack + ".PreRequisites").isEmpty()) {
					hasPreRequisites = false;
				} else {
					hasPreRequisites = true;
				}

				if (hasPreRequisites) {
					preRequisites.addAll(plugin.getConfig().getStringList("packages." + purchasedPack + ".PreRequisites"));
				}

				for (String preRequisite: preRequisites) {
					if (!Methods.hasPurchased(player.getName(), preRequisite, Commands.Server)) {
						player.sendMessage(Commands.Prefix + Commands.DPPrerequisite.replace("%pack", preRequisite));
						return;
					}
				}

				if (plugin.getConfig().getBoolean("General.SpecificPermissions", true)) {
					if (!Methods.hasPermission(player, "donationpoitns.sign.use." + purchasedPack)) {
						player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
						return;
					}
				}

				if (plugin.getConfig().getBoolean("packages." + purchasedPack + ".UseVaultEconomy")) {
					usesVault = true;
				} else {
					usesVault = false;
				}

				Double price;
				boolean worldRestricted;

				if (plugin.getConfig().getStringList("packages." + purchasedPack + ".RestrictToWorlds") != null
						&& !plugin.getConfig().getStringList("packages." + purchasedPack + ".RestrictToWorlds").isEmpty()) {
					worldRestricted = true;
				} else {
					worldRestricted = false;
				}
				if (worldRestricted) {
					List<String> applicableWorlds = plugin.getConfig().getStringList("packages." + purchasedPack + ".RestrictToWorlds");
					if (!applicableWorlds.contains(player.getWorld().getName().toLowerCase())) {
						player.sendMessage(Commands.RestrictedWorldMessage.replace("%worlds", applicableWorlds.toString()));
						return;
					}
				}
				
				if (Methods.hasPermission(player, "donationpoints.free")) {
					price = 0.0;
				} else {
					price = plugin.getConfig().getDouble("packages." + purchasedPack + ".price");
				}
				
				if (usesVault) {
					if (!Methods.econ.has(player.getName(), price)) {
						player.sendMessage(Commands.Prefix + Commands.NotEnoughPoints);
						return;
					}
				} else {
					if (Methods.getBalance(player.getUniqueId()) < price) {
						player.sendMessage(Commands.Prefix + Commands.NotEnoughPoints);
						return;
					}
				}
				
				purchases.put(player.getName().toLowerCase(), purchasedPack);
				if (purchases.containsKey(player.getName().toLowerCase())) {
					player.sendMessage(Commands.Prefix + Commands.DPConfirm.replace("%pack", purchasedPack).replace("%price", price.toString()));
					confirmTask = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
						public void run() {
							if (purchases.containsKey(player.getName().toLowerCase())) {
								purchases.remove(player.getName().toLowerCase());
								player.sendMessage(Commands.Prefix + Commands.TooLongOnConfirm);
							}
						}
					}, 300L);
				}
			}
		}
	}


	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (block.getState() instanceof Sign) {
			Sign s = (Sign) block.getState();
			String signline1 = s.getLine(0);
			if (signline1.equalsIgnoreCase("[" + SignMessage + "]")
					&& event.getAction().equals(Action.LEFT_CLICK_BLOCK)
					&& block.getType() == Material.WALL_SIGN) {
				if (!DonationPoints.permission.has(player, "donationpoints.sign.use")) {
					player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
				}
				if (DonationPoints.permission.has(player, "donationpoints.sign.use")) {
					String purchasedPack = s.getLine(1);
					if (Methods.isPackMisconfigured(purchasedPack)) {
						player.sendMessage(Commands.Prefix + ChatColor.RED + "This package is misconfigured. Consult an administrator.");
						return;
					}
					Double price = plugin.getConfig().getDouble("packages." + purchasedPack + ".price");
					String packDesc = plugin.getConfig().getString("packages." + purchasedPack + ".description");
					player.sendMessage(Commands.Prefix + Commands.SignLeftClick.replace("%pack", purchasedPack).replace("%price", price.toString()));
					player.sendMessage(Commands.Prefix + Commands.SignLeftClickDescription.replace("%desc", packDesc));
				}
			}
			if (signline1.equalsIgnoreCase("[" + SignMessage + "]")
					&& event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
					&& block.getType() == Material.WALL_SIGN) { // They clicked a sign and are going to purchase a package.
				if (Methods.hasPermission(player, "donationpoints.sign.use")) {
					player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
					return;
				}
				String purchasedPack = s.getLine(1);
				boolean usesVault;
				boolean hasPreRequisites;
				Set<String> preRequisites = new HashSet<String>();

				if (Methods.isPackMisconfigured(purchasedPack)) {
					player.sendMessage(Commands.Prefix + ChatColor.RED + "This package is misconfigured. Consult an administrator.");
					return;
				}
				if (plugin.getConfig().getStringList("packages." + purchasedPack + ".PreRequisites") == null
						|| plugin.getConfig().getStringList("packages." + purchasedPack + ".PreRequisites").isEmpty()) {
					hasPreRequisites = false;
				} else {
					hasPreRequisites = true;
				}
				if (hasPreRequisites) {
					preRequisites.addAll(plugin.getConfig().getStringList("packages." + purchasedPack + "PreRequisites"));
				}
				for (String preRequisite: preRequisites) {
					if (!Methods.hasPurchased(player.getName(), preRequisite, Commands.Server)) {
						player.sendMessage(Commands.Prefix + Commands.DPPrerequisite.replace("%pack", preRequisite));
						return; // We don't want to let the player purchase the package if they are missing even one of the PreRequisites.
					}
				}

				/*
				 * By this point we have made sure that the player has purchased all of the required PreRequisites and can continue to purchase to the package.
				 */

				if (plugin.getConfig().getBoolean("General.SpecificPermissions", true)) {
					if (!Methods.hasPermission(player, "donationpoints.sign.use." + purchasedPack)) {
						player.sendMessage(Commands.Prefix + Commands.noPermissionMessage);
						return;
					}
				}

				event.setUseItemInHand(Result.DENY);
				event.setUseInteractedBlock(Result.DENY);

				/*
				 * Now we have checked to make sure the player has the permission to buy the package,
				 * if the SpecificPermission node is true. If the SpecificPermission node is false, we
				 * checked for Permission earlier, we only care if they can click the sign.
				 * 
				 * We can start getting the package information now.
				 */

				if (plugin.getConfig().getBoolean("packages." + purchasedPack + ".UseVaultEconomy")) {
					usesVault = true;
				} else {
					usesVault = false;
				}

				Double price;
				boolean worldRestricted;

				if (plugin.getConfig().getStringList("packages." + purchasedPack + ".RestrictToWorlds") != null
						&& plugin.getConfig().getStringList("packages." + purchasedPack + ".RestrictToWorlds").isEmpty()) {
					worldRestricted = true;
				} else {
					worldRestricted = false;
				}

				if (worldRestricted) {
					List<String> applicableWorlds = plugin.getConfig().getStringList("packages." + purchasedPack + ".RestrictToWorlds");
					if (!applicableWorlds.contains(player.getWorld().getName().toLowerCase())) {
						player.sendMessage(Commands.RestrictedWorldMessage.replace("%worlds", applicableWorlds.toString()));
						return;
					}
				}

				/*
				 * Now we've made sure that the player is in a world where they are allowed to purchase the package.
				 */

				if (Methods.hasPermission(player, "donationpoints.free")) {
					price = 0.0;
				} else {
					price = plugin.getConfig().getDouble("packages." + purchasedPack + ".price");
				}

				if (usesVault) {
					if (!Methods.econ.has(player.getName(), price)) {
						player.sendMessage(Commands.Prefix + Commands.NotEnoughPoints);
						return;
					}
				} else {
					if (Methods.getBalance(player.getUniqueId()) < price) {
						player.sendMessage(Commands.Prefix + Commands.NotEnoughPoints);
						return;
					}
				}

				/*
				 * Now we've checked to make sure that the player has the money / points to make the purchase.
				 * If the player has the permission node donationpoints.free, the price is 0.
				 */

				purchases.put(player.getName().toLowerCase(), purchasedPack);
				if (purchases.containsKey(player.getName().toLowerCase())) {
					player.sendMessage(Commands.Prefix + Commands.DPConfirm.replace("%pack", purchasedPack).replace("%price", price.toString()));
					confirmTask = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
						public void run() {
							if (purchases.containsKey(player.getName().toLowerCase())) {
								purchases.remove(player.getName().toLowerCase());
								player.sendMessage(Commands.Prefix + Commands.TooLongOnConfirm);
							}
						}
					}, 300L);
				}

			}
		}
	}

	@EventHandler
	public void PlayerJoinEvent(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		if (plugin.getConfig().getBoolean("General.AutoCreateAccounts", true)) {
			if (!Methods.hasAccount(p.getUniqueId())) {
				Methods.createAccount(p.getUniqueId());
				DonationPoints.log.info("Created an account for " + p.getName());
			}
		}
	}


}
