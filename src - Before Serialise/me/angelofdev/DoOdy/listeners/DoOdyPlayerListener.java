/*
 *  DoOdy v1: Separates Admin/Mod duties so everyone can enjoy the game.
 *  Copyright (C) 2013  M.Y.Azad
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */

package me.angelofdev.DoOdy.listeners;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import me.angelofdev.DoOdy.command.DoOdyCommandExecutor;
import me.angelofdev.DoOdy.config.Configuration;
import me.angelofdev.DoOdy.util.Debug;
import me.angelofdev.DoOdy.util.HashMaps;
import me.angelofdev.DoOdy.util.MessageSender;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class DoOdyPlayerListener implements Listener {
	private MessageSender m = new MessageSender();
	
	public DoOdyPlayerListener() {
	}
	List<String> deniedCommands = Configuration.config.getStringList("Denied.commands");
	List<Integer> configDropList = Configuration.config.getIntegerList("Duty Deny Drops.whitelist");
	List<Integer> configStorageDenied = Configuration.config.getIntegerList("Deny Storage.storage");

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		String message = event.getMessage().toLowerCase();
		if (DoOdyCommandExecutor.myArr.contains(playerName)) {
			if (deniedCommands.contains(message)) {
				event.setCancelled(true);
				m.player(player, "&6[DoOdy] &cYou're not allowed to use this command on duty!");
				Debug.check("<onPlayerCommandPreprocess> " + playerName + " tried executing command in Denied Commands");
			}
		}
	}

	@EventHandler(ignoreCancelled=true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) {
			String playerName = player.getName();
			if (player.isOp() || HashMaps.duty.containsKey(playerName) || player.hasPermission("doody.failsafe.bypass")){
				return;
			}
			player.setGameMode(GameMode.SURVIVAL);
			player.getInventory().clear();
			if (DoOdyCommandExecutor.myArr.contains(playerName)) {
				DoOdyCommandExecutor.myArr.removeAll(Arrays.asList(playerName));
			}
			if (HashMaps.expOrb.containsKey(playerName)) {
				HashMaps.expOrb.remove(playerName);
			}
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (HashMaps.duty.containsKey(playerName)) {
			try {
				setOriginal(player);
				HashMaps.removeMaps(playerName);
			} catch (Exception e) {
				DoOdyCommandExecutor.myArr.removeAll(Arrays.asList(playerName));
				player.setGameMode(GameMode.SURVIVAL);
				player.getInventory().clear();
			}
		}
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (HashMaps.duty.containsKey(playerName)) {
			try {
				setOriginal(player);
				HashMaps.removeMaps(playerName);
			} catch (Exception e) {
				DoOdyCommandExecutor.myArr.removeAll(Arrays.asList(playerName));
				player.setGameMode(GameMode.SURVIVAL);
				player.getInventory().clear();
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (HashMaps.duty.containsKey(playerName)) {
			String worldName = player.getWorld().getName();
			if (!player.hasPermission("doody.worlds." + worldName)) {
				try {
					setOriginal(player);
					HashMaps.removeMaps(playerName);
				} catch (Exception e) {
					DoOdyCommandExecutor.myArr.removeAll(Arrays.asList(playerName));
					player.setGameMode(GameMode.SURVIVAL);
					player.getInventory().clear();
				}
				Debug.check("<onPlayerWorldChange> " + playerName + " Does not have the permission 'doody.worlds." + worldName + "'");
			} else {
				if (player.isOp()) {
					Debug.normal("<onPlayerWorldChange> " + playerName + " is OP.");
				} else {
					Debug.check("<onPlayerWorldChange> " + playerName + " Player has the permission 'doody.worlds." + worldName + "'");
				}
			}
		}
	}
		
	@EventHandler(ignoreCancelled=true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		String playerName = player.getName();
		
		if(HashMaps.duty.containsKey(playerName)) {
			event.getDrops().clear();
			event.setDroppedExp(0);
		}
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		
		if(HashMaps.duty.containsKey(playerName) && HashMaps.armour.containsKey(playerName)) {
			player.getInventory().setArmorContents(HashMaps.armour.get(playerName));
		}
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		
		if (HashMaps.duty.containsKey(playerName) && Configuration.config.getBoolean("Duty Deny Drops.enabled")) {
			if (!(player.isOp() || player.hasPermission("doody.dropitems"))) {
				Item item = event.getItemDrop();
				int itemID = item.getItemStack().getTypeId();
				if (!(configDropList.contains(itemID))) {
					String message = item.getItemStack().getType().name();
					String itemname = message.toLowerCase();
			
					event.getItemDrop().remove();
			
					if (Configuration.config.getBoolean("Duty Deny Drops.messages")) {
						player.sendMessage(ChatColor.GOLD + "[DoOdy] " + ChatColor.RED + "There's no need to drop " + ChatColor.YELLOW + itemname + ChatColor.RED + " while on Duty.");
					}
					Debug.check("<onPlayerDropItem> " + playerName + " got denied item drop. <Item not in whitelist(" + itemname + ")>");
				}
			} else {
				if (Configuration.config.getBoolean("Debug.enabled")) {
					Item item = event.getItemDrop();
					int itemID = item.getItemStack().getTypeId();
					String message = item.getItemStack().getType().name();
					String itemname = message.toLowerCase();
					if (configDropList.contains(itemID)) {
						Debug.normal("<onPlayerDropItem> Warning! " + itemname + " is whitelisted in config.");
						Debug.normal("<onPlayerDropItem> Warning! " + "Allowing " + playerName + " to drop " + itemname);
					} else {
						if (player.isOp()) {
							Debug.normal("<onPlayerDropItem> Warning! " + playerName + " is OP -Allowing item drop, " + itemname);
						} else if (player.hasPermission("doody.dropitems")) {
							Debug.normal("<onPlayerDropItem> Warning! " + playerName + " has doody.dropitems -Allowing item drop, " + itemname);
						} else {
							//It should not have reached here
							Debug.severe("<onPlayerDropItem> Another plugin may be causing a conflict. DoOdy Debug cannot make sense.");
						}
					}
				}
				return;
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			String playerName = player.getName();
			
			if (HashMaps.duty.containsKey(playerName) && Configuration.config.getBoolean("Deny Storage.enabled")) {
				Block block = event.getClickedBlock();
				int blockID = block.getType().getId();
				
				Debug.check("<onPlayerInteract>" + playerName + " Right Clicked on " + blockID);
				if (configStorageDenied.contains(blockID)) {
					if (!(player.isOp() || player.hasPermission("doody.storage"))) {
						event.setCancelled(true);
						if (Configuration.config.getBoolean("Deny Storage.messages")) {
							player.sendMessage(ChatColor.RED + "There's no need to store things while on duty.");
						}
						Debug.check("<onPlayerInteract> " + playerName + " got denied storage interact. <Block :" + blockID + " is in Deny Storage list>");
					} else {
						if (Configuration.config.getBoolean("Debug.enabled")) {
							if (player.isOp()) {
								Debug.normal("<onPlayerInteract> Warning! " + playerName + " is OP -Allowing storage interact");
							} else if (player.hasPermission("doody.storage")) {
								Debug.normal("<onPlayerInteract> Warning! " + playerName + " has doody.storage -Allowing storage interact");
							} else if (!(configStorageDenied.contains(blockID))) {
								Debug.normal("<onPlayerInteract> Warning! " + block.getType().name().toLowerCase() + " is not in 'Deny Storage.storage' list -Allowing storage interact");
							} else {
								//It should not have reached here
								Debug.severe("<onPlayerInteract> Another plugin may be causing a conflict. DoOdy Debug cannot make sense.");
							}
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof StorageMinecart) {
			Player player = event.getPlayer();
			String playerName = player.getName();
			
			if (HashMaps.duty.containsKey(playerName) && (Configuration.config.getBoolean("Deny Storage.enabled"))) {
				if (!(player.isOp() || player.hasPermission("doody.storage"))) {
					event.setCancelled(true);
					if (Configuration.config.getBoolean("Deny Storage.messages")) {
						player.sendMessage(ChatColor.RED + "There's no need to store things while on Duty.");
					}
					Debug.check("<onEntityInteract> Success! " + playerName + " got denied storage interact.");
				} else {
					if (Configuration.config.getBoolean("Debug.enabled")) {
						if (player.isOp()) {
							Debug.normal("<onEntityInteract> Warning! " + playerName + " is OP -Allowing storage interact");
						} else if (player.hasPermission("doody.storage")) {
							Debug.normal("<onEntityInteract> Warning! " + playerName + " has doody.storage -Allowing storage interact");
						} else {
							//It should not have reached here
							Debug.severe("<onEntityInteract> Another plugin may be causing a conflict. DoOdy Debug cannot make sense.");
						}
					}
				}
			}
		}
	}
	
	public void setOriginal(Player player) {
		String playerName = player.getName();

		if (HashMaps.inventory.containsKey(playerName)) {
			player.getInventory().setContents(HashMaps.inventory.get(playerName));
		} else {
			player.getInventory().clear();
			try {
				Integer size = player.getInventory().getSize();
				Integer i = 0;
				for(i=0; i < size; i++) {
					ItemStack item = new ItemStack(0, 0);
					if(Configuration.inventory.getInt(playerName + "." + i.toString() + ".amount", 0) !=0) {
						Integer amount = Configuration.inventory.getInt(playerName + "." + i.toString() + ".amount", 0);
						Integer durability = Configuration.inventory.getInt(playerName + "." + i.toString() + ".durability", 0);
						Integer type = Configuration.inventory.getInt(playerName + "." + i.toString() + ".type", 0);
						item.setAmount(amount);
						item.setTypeId(type);
						item.setDurability(Short.parseShort(durability.toString()));
						player.getInventory().setItem(i, item);
					}
				}
			} catch(Exception e) {
			}
		}
		Configuration.inventory.set(playerName, null);
		try {
			Configuration.inventory.save();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		player.getInventory().setArmorContents(HashMaps.armour.get(playerName));
		World world = Bukkit.getServer().getWorld(Configuration.location.getString(playerName + ".world"));
		double x = Configuration.location.getDouble(playerName + ".x");
		double y = Configuration.location.getDouble(playerName + ".y");
		double z = Configuration.location.getDouble(playerName + ".z");
		double pit = Configuration.location.getDouble(playerName + ".pitch");
		double ya = Configuration.location.getDouble(playerName + ".yaw");
		float pitch = (float) pit;
		float yaw = (float) ya;

		Location local = new Location(world, x, y, z, yaw, pitch);
		player.teleport(local);

		Configuration.location.set(playerName, null);
		try {
			Configuration.location.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		player.setGameMode(GameMode.SURVIVAL);
		player.setLevel(HashMaps.expOrb.get(playerName));
		DoOdyCommandExecutor.myArr.removeAll(Arrays.asList(playerName));
	}

	
	/** SLAPI = Saving/Loading API
	 * API for Saving and Loading Objects.
	 * @author Tomsik68
	 */
	public static class SLAPI {
		public static void save(Object obj,String path) throws Exception {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(obj);
			oos.flush();
			oos.close();
		}
		public static Object load(String path) throws Exception	{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			Object result = ois.readObject();
			ois.close();
			return result;
		}
	}
}
