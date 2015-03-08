package com.github.Kraken3.AFKPGC;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

class EventHandlers implements Listener {		
	
	@EventHandler
	public void PlayerKickEvent(PlayerQuitEvent event) {	
		AFKPGC.removerPlayer(event.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent event) {	
		AFKPGC.removerPlayer(event.getPlayer().getUniqueId());
	}	
		
	
	public void registerActivity(Player p){
		AFKPGC.addPlayer(p);
	}
	
	
	//EVENTS THAT REGISTER PLAYER ACTIVITY
	
	@EventHandler
	public void PlayerJoinEvent(PlayerLoginEvent event) {
		registerActivity(event.getPlayer());
	}

	//seemingly duplicate events are here for resiliency/defensive programming
	//as the plugin used to crash for some unobvious reason. I hate it too.
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		registerActivity(event.getPlayer());
	}

	@EventHandler
	public void onPlayerMoveEvent(PlayerMoveEvent event) {			
		registerActivity(event.getPlayer());
	}	
	@EventHandler
	public void onPlayerChatEvent(PlayerChatEvent event) {
		registerActivity(event.getPlayer());
	}
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		registerActivity(event.getPlayer());
	}
	@EventHandler
	public void onPlayerDropItemEvent(PlayerDropItemEvent event) {	
		registerActivity(event.getPlayer());
	}
	@EventHandler
	public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
		registerActivity(event.getPlayer());
	}
	@EventHandler
	public void onPlayerItemHeldEvent(PlayerItemHeldEvent event) {
		registerActivity(event.getPlayer());
	}
	@EventHandler
	public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event) {	
		registerActivity(event.getPlayer());
	}
	@EventHandler
	public void onEnchantItemEvent(EnchantItemEvent event) {		
		registerActivity(event.getEnchanter().getPlayer());
	}
	@EventHandler
	public void onPrepareItemEnchantEvent(PrepareItemEnchantEvent event) {	
		registerActivity(event.getEnchanter().getPlayer());
	}
	@EventHandler
	public void onInventoryClickEvent(InventoryClickEvent event) {	
		registerActivity(Bukkit.getPlayer(event.getWhoClicked().getName()));
	}

}
