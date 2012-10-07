package me.nathanaelps.plugins.mushroomislandspread;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.TreeType;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MushroomIslandSpread extends JavaPlugin implements Listener {

	public static String pluginName;
	public static String pluginVersion;
	public static Server server;
	public static MushroomIslandSpread plugin;

	public void onDisable() {
		this.saveConfig();

		log("Disabled");
	}

	public void onEnable() {
		pluginName = this.getDescription().getName();
		pluginVersion = this.getDescription().getVersion();
		server = this.getServer();
		plugin = this;

		getServer().getPluginManager().registerEvents(this, this);

		log("Enabled.");
	}

	public void log(String in) {
		System.out.println("[" + pluginName + "] " + in);
	}
	public void log(int in) {
		log(String.valueOf(in));
	}
	public void log(double in) {
		log(Double.toString(in));
	}
	
	@EventHandler public void onBlockSpreadEvent(BlockSpreadEvent event) {
		if(event.getSource().getType()!=Material.MYCEL) {return;}
		
		rotBlock(event.getBlock(), false);
	}
	
	@EventHandler public void onBlockPlace(BlockPlaceEvent event) {
		if(event.getBlockPlaced().getType()!=Material.MYCEL) {return;}
		
		rotBlock(event.getBlock(), false);
	}
	
	public void rotBlock(Block block, boolean onLog) {
		List<Integer> browns = this.getConfig().getIntegerList("brownableBlocks");
		List<Integer> reds = this.getConfig().getIntegerList("redableBlocks");
		List<Integer> rots = this.getConfig().getIntegerList("rottableBlocks");
		List<Integer> removes = this.getConfig().getIntegerList("removeBlocks");
		List<Integer> logs = this.getConfig().getIntegerList("logBlocks");
		int brown = 39;
		int red = 40;
		int rot = 3;
		int remove = 0;
		
		for(int i=-2; i<3; i++){
			for(int j=-3; j<2; j++){
				for(int k=-2; k<3; k++){
						Block nearBlock = block.getRelative(i,j,k);
						if((nearBlock.getTypeId()==brown) && (Math.random() < .02)) {
							nearBlock.setTypeId(0);
							block.getWorld().generateTree(nearBlock.getLocation(), TreeType.BROWN_MUSHROOM);
						}
						if((nearBlock.getTypeId()==red) && (Math.random() < .02)) {
							nearBlock.setTypeId(0);
							block.getWorld().generateTree(nearBlock.getLocation(), TreeType.RED_MUSHROOM);
						}
						if(browns.contains(nearBlock.getTypeId())) { nearBlock.setTypeId(brown); }
						if(reds.contains(nearBlock.getTypeId())) { nearBlock.setTypeId(red); }
						if(nearBlock.getRelative(0,1,0).isEmpty() && rots.contains(nearBlock.getTypeId())) {
							if(Math.random()<.1) { nearBlock.setType(Material.MYCEL); }
							else { nearBlock.setTypeId(rot); }
						}
						if(removes.contains(nearBlock.getTypeId())) { nearBlock.setTypeId(remove); }
						if((!onLog) && logs.contains(nearBlock.getTypeId())) { scheduleRot(nearBlock); }
						//also turn surface stone/cobble to dirt. Or sand?
						nearBlock.setBiome(Biome.MUSHROOM_ISLAND);
				}
			}
		}
		int x = block.getChunk().getX();
		int z = block.getChunk().getZ();
		block.getWorld().refreshChunk(x, z);
	}
	
	public void rotLog(Block block) {
		if(block.getTypeId() != 17) { return; }
		List<Integer> log = this.getConfig().getIntegerList("logBlocks");
		block.setTypeId(99);
		boolean lastLeaf = true;
		for(int i=-1; i<=1; i++){
			for(int j=-1; j<=1; j++){
				for(int k=-1; k<=1; k++){
					Block nearBlock = block.getRelative(i,j,k);
					if(log.contains(nearBlock.getTypeId())) { scheduleRot(nearBlock); lastLeaf = false;}
				}
			}
		}
		if(lastLeaf) { block.setData((byte) 14); }
		else { block.setData((byte) 15); }
		rotBlock(block,true);
	}

	private void scheduleRot(final Block block) {
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				rotLog(block);
			}
		}, (long) (Math.random()*200));
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("mushload")){
			this.reloadConfig();
			log("Config reloaded.");
			return true;
		}
		return false; 
	}

}
