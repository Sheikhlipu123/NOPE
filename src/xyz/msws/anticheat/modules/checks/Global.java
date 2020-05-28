package xyz.msws.anticheat.modules.checks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import xyz.msws.anticheat.NOPE;
import xyz.msws.anticheat.modules.AbstractModule;
import xyz.msws.anticheat.modules.data.CPlayer;
import xyz.msws.anticheat.utils.MSG;

public class Global extends AbstractModule implements Listener {
	public Global(NOPE plugin) {
		super(plugin);
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		CPlayer cp = plugin.getCPlayer(player);

		long time = System.currentTimeMillis();
		boolean onGround = player.isOnGround(), weirdBlock = cp.isInWeirdBlock(), climbing = cp.isInClimbingBlock();

		cp.setTempData(Stat.MOVE, time);

		Location from = event.getFrom(), to = event.getTo();

		if (plugin.debugMode()) {
			MSG.tell(player, " ");
			MSG.tell(player, "&9From &e" + String.format("%.3f, %.3f, %.3f", from.getX(), from.getY(), from.getZ()));
			MSG.tell(player, "&9To &6" + String.format("%.3f, %.3f, %.3f", to.getX(), to.getY(), to.getZ()));
			MSG.tell(player, "&9Diff &b" + String.format("%.3f, %.3f, %.3f", to.getX() - from.getX(),
					to.getY() - from.getY(), to.getZ() - from.getZ()));
			MSG.tell(player,
					String.format("&9In: &e%s &7(Solid: %s&7, Liquid: %s&7)",
							MSG.camelCase(to.getBlock().getType().toString()),
							MSG.TorF(to.getBlock().getType().isSolid()), MSG.TorF(to.getBlock().isLiquid())));
			MSG.tell(player, String.format("&9OnGround&7: %s", MSG.TorF(player.isOnGround())));
		}

		if (to.getBlock().isLiquid() || from.getBlock().isLiquid())
			cp.setTempData(Stat.IN_LIQUID, time);

		if (from.getY() != to.getY())
			cp.setTempData(Stat.VERTICAL_CHANGE, time);

		if (onGround) {
			cp.setTempData(Stat.ON_GROUND, time);
			if (!weirdBlock && player.getLocation().subtract(0, .1, 0).getBlock().getType().isSolid()) {
				cp.setLastSafeLocation(player.getLocation());
			}
		} else {
			cp.setTempData(Stat.IN_AIR, time);
		}

		if (cp.isBlockAbove()) {
			if (player.getLocation().clone().add(0, -.5, 0).getBlock().getType().toString().contains("ICE") || player
					.getLocation().clone().subtract(0, .05, 0).getBlock().getType().toString().contains("TRAP")) {
				cp.setTempData(Stat.ICE_TRAPDOOR, time);

			}
		}

		if (player.getLocation().clone().subtract(0, 1, 0).getBlock().getType().toString().contains("ICE"))
			cp.setTempData(Stat.ON_ICE, time);

		boolean isBlockNearby = false;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				if (player.getLocation().clone().add(x, -.1, z).getBlock().getType().isSolid()) {
					isBlockNearby = true;
					break;
				}
				if (player.getLocation().clone().add(x, -1.5, z).getBlock().getType().isSolid()) {
					isBlockNearby = true;
					break;
				}
			}
		}

		if (isBlockNearby)
			cp.setTempData(Stat.FLIGHT_GROUNDED, time);

		if (climbing)
			cp.setTempData(Stat.CLIMBING, time);

		if (weirdBlock)
			cp.setTempData(Stat.IN_WEIRD_BLOCK, time);

		if (player.isInsideVehicle())
			cp.setTempData(Stat.IN_VEHICLE, time);

		if (player.isFlying() || cp.usingElytra())
			cp.setTempData(Stat.FLYING, time);

		if (player.isSprinting())
			cp.setTempData(Stat.SPRINTING, time);

		if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ())
			cp.setTempData(Stat.HORIZONTAL_BLOCKCHANGE, time);

		Location vertLine = player.getLocation().clone();
		while (!vertLine.getBlock().getType().isSolid() && vertLine.getY() > 0)
			vertLine.subtract(0, 1, 0);

		Block lowestBlock = vertLine.getBlock();

		if (lowestBlock.getType() == Material.SLIME_BLOCK || lowestBlock.getType() == Material.HONEY_BLOCK)
			cp.setTempData(Stat.ON_SLIMEBLOCK, time);

		if (cp.isRedstoneNearby())
			cp.setTempData(Stat.NEAR_REDSTONE, time);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		CPlayer cp = plugin.getCPlayer(player);
		cp.setTempData(Stat.BLOCK_PLACE, System.currentTimeMillis());
	}

	@EventHandler(ignoreCancelled = true)
	public void onToggleFlight(PlayerToggleFlightEvent event) {
		Player player = event.getPlayer();
		CPlayer cp = plugin.getCPlayer(player);

		cp.setTempData(Stat.TOGGLE_FLIGHT, System.currentTimeMillis());

		if (player.isFlying()) {
			cp.setTempData(Stat.DISABLE_FLIGHT, System.currentTimeMillis());
		} else {
			cp.setTempData(Stat.ENABLE_FLIGHT, System.currentTimeMillis());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onToggleGlide(EntityToggleGlideEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		Player player = ((Player) event.getEntity());
		CPlayer cp = plugin.getCPlayer(player);

		cp.setTempData(Stat.TOGGLE_GLIDE, System.currentTimeMillis());

		if (player.isGliding()) {
			cp.setTempData(Stat.DISABLE_GLIDE, System.currentTimeMillis());
		} else {
			cp.setTempData(Stat.ENABLE_GLIDE, System.currentTimeMillis());
		}
	}

	@EventHandler
	public void onVehicleLeave(VehicleExitEvent event) {
		if (!(event.getExited() instanceof Player))
			return;
		Player player = ((Player) event.getExited());
		CPlayer cp = plugin.getCPlayer(player);

		cp.setTempData(Stat.LEAVE_VEHICLE, System.currentTimeMillis());

	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		CPlayer cp = plugin.getCPlayer(player);

		cp.setTempData(Stat.TELEPORT, System.currentTimeMillis());
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		Entity ent = event.getEntity();
		if (!(ent instanceof Player))
			return;
		Player player = (Player) ent;
		CPlayer cp = plugin.getCPlayer(player);
		cp.setTempData(Stat.DAMAGE_TAKEN, System.currentTimeMillis());
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		CPlayer cp = plugin.getCPlayer(player);
		cp.setTempData(Stat.RESPAWN, System.currentTimeMillis());
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		CPlayer cp = plugin.getCPlayer(player);
		cp.setLastSafeLocation(player.getLocation());
		cp.setTempData(Stat.JOIN_TIME, System.currentTimeMillis());
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player))
			return;

		Player player = (Player) event.getWhoClicked();
		CPlayer cp = plugin.getCPlayer(player);

		cp.setTempData(Stat.INVENTORY_CLICK, System.currentTimeMillis());

	}

	public enum Stat {
		/**
		 * Last time damage was taken
		 */
		DAMAGE_TAKEN,
		/**
		 * Time since the player joined the server
		 */
		JOIN_TIME,
		/**
		 * Last time the player left their vehicle
		 */
		LEAVE_VEHICLE, TELEPORT, TOGGLE_GLIDE, DISABLE_GLIDE, ENABLE_GLIDE, BLOCK_PLACE, NEAR_REDSTONE, ON_SLIMEBLOCK,
		HORIZONTAL_BLOCKCHANGE, SPRINTING, FLYING, IN_VEHICLE, FLIGHT_GROUNDED, ON_ICE, ICE_TRAPDOOR, VERTICAL_CHANGE,
		IN_AIR, IN_LIQUID, ON_GROUND, MOVE, FLAGGED, CLIMBING, IN_WEIRD_BLOCK, TOGGLE_FLIGHT, DISABLE_FLIGHT,
		ENABLE_FLIGHT, INVENTORY_CLICK, OPEN_INVENTORY, RESPAWN;
	}

	@Override
	public void enable() {

		Bukkit.getPluginManager().registerEvents(this, plugin);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {

			for (Player p : Bukkit.getOnlinePlayers()) {
				CPlayer cp = plugin.getCPlayer(p);
				ConfigurationSection vlSection = cp.getDataFile().getConfigurationSection("vls");
				if (vlSection == null)
					continue;

				double lastFlag = cp.timeSince(Stat.FLAGGED);

				int diff = 1;
				if (cp.hasTempData(Stat.FLAGGED))
					if (lastFlag > 1.8e+6) {
						diff = 20;
					} else if (lastFlag > 600000) {
						diff = 10;
					} else if (lastFlag > 300000) {
						diff = 5;
					} else if (lastFlag > 50000) {
						diff = 3;
					} else if (lastFlag > 10000) {
						diff = 2;
					}

				for (String hack : vlSection.getKeys(false)) {
//					if (cp.getSaveInteger("vls." + hack) == 0)
					if (cp.getSaveData("vls." + hack, Integer.class) == 0)
						continue;
//					cp.setSaveData("vls." + hack, cp.getSaveInteger("vls." + hack) - diff);
					cp.setSaveData("vls." + hack, cp.getSaveData("vls." + hack, Integer.class) - diff);
//					if (cp.getSaveInteger("vls." + hack) < 0)
					if (cp.getSaveData("vls." + hack, Integer.class) < 0)
						cp.setSaveData("vls." + hack, 0);
					MSG.sendPluginMessage(null,
							"setvl:" + p.getName() + " " + hack + " " + cp.getSaveData("vls." + hack, Integer.class));
				}
			}
		}, 0, 40);
	}

	@Override
	public void disable() {

	}
}