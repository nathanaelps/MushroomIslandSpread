package me.nathanaelps.plugins.mushroomspread;

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

public class MushroomSpread extends JavaPlugin implements Listener {

	//These are standard lines that I put in all my plugins. I got them from somewhere, and they're so handy!
	public static String pluginName;
	public static String pluginVersion;
	public static Server server;
	public static MushroomSpread plugin;
	
	public void onDisable() {
		
		//When we're closing down the plugin, save the config... Isn't working. Why not?
		this.saveConfig();
		
		//and log "disabled" to the server log. Note the 'log' function. Pretty handy, if you're lazy like me.
		log("Disabled");
	}

	public void onEnable() {
		
		//Sets up the standard variables.
		pluginName = this.getDescription().getName();
		pluginVersion = this.getDescription().getVersion();
		server = this.getServer();
		plugin = this;

		//registers this plugin
		getServer().getPluginManager().registerEvents(this, this);
		
		//Maybe this will fix my config.yml-is-not-saving issues? I don't know how, or why...
		this.getConfig();
		
		//logs.... You know, you can figure this line out by yourself. Go for it.
		log("Enabled.");
	}

	//Boring utility functions. I don't like having to write out the whole "System.out.println, etc" stuff each time. BORING.
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
		//Whenever a block spreads (i.e., grass or mycelium),
		
		//check to see if it's mycelium. If it's not, just leave this function.
		if(event.getSource().getType()!=Material.MYCEL) {return;}
		
		//if it is, then rot this block. We'll look at the _false_ when we get to the rotLog function.
		rotBlock(event.getBlock(), false);
	}
	
	@EventHandler public void onBlockPlace(BlockPlaceEvent event) {
		//similarly, whenever a block is placed,
		
		//check to see if it's mycelium. If it's not, just leave this function.
		if(event.getBlockPlaced().getType()!=Material.MYCEL) {return;}
		
		//if it is, then rot this block.
		rotBlock(event.getBlock(), false);
	}
	
	public void rotBlock(Block block, boolean onLog) {
		//Ah, the actual rot! We accept a Block argument, and a boolean argument that we'll deal with later.
		
		//First, we'll load all of our lists of blocks.
		//These lists come from the config.yml, and they tell us:
		
		//Which blocks should we turn into small brown mushrooms?
		List<Integer> browns = this.getConfig().getIntegerList("mush.brownableBlocks");

		//Which blocks should we turn into small red mushrooms?
		List<Integer> reds = this.getConfig().getIntegerList("mush.redableBlocks");

		//Which blocks "rot" (into dirt) immediately?
		List<Integer> rots = this.getConfig().getIntegerList("mush.rottableBlocks");
		
		//Which blocks should just disappear (This is handy for vines!)
		List<Integer> removes = this.getConfig().getIntegerList("mush.removeBlocks");
		
		//And which blocks should we treat as smaller pieces of a larger fungus.
		List<Integer> logs = this.getConfig().getIntegerList("mush.logBlocks");

		//And let's simplify our later code.
		int brown = 39; //small brown mushroom.
		int red = 40; //small red mushroom.
		int rot = 3; //dirt!
		int remove = 0; //Um... air. If you don't know that 0 is air, then... Ouch.
		
		//Now, the FOR blocks that will let us look at each block around the rotting block.
		//i will be used in the x direction, j in the y, and k in the z.
		//Note that i and k (x and z) form a square with a diameter of 5, while
		//j (y) starts 3 below the block, and goes to 1 above the block.
		for(int i=-2; i<3; i++){
			for(int k=-2; k<3; k++){
				for(int j=1; j>-4; j--){
					//We'll name the nearby block that we want to examine... nearBlock! How's that for creative naming?
					Block nearBlock = block.getRelative(i,j,k);

					//Fun little side thing that allows occasional small mushrooms to grow into BIG mushrooms! YAY!
					if((nearBlock.getTypeId()==brown) && (Math.random() < .02)) {
						nearBlock.setTypeId(0);
						block.getWorld().generateTree(nearBlock.getLocation(), TreeType.BROWN_MUSHROOM);
					}
					if((nearBlock.getTypeId()==red) && (Math.random() < .02)) {
						nearBlock.setTypeId(0);
						block.getWorld().generateTree(nearBlock.getLocation(), TreeType.RED_MUSHROOM);
					}

					//Now, actually rotting things: Turn blocks from the brown mushroom list (that we got at the beginning of the function)
					//into brown mushrooms. Pretty simple.
					if(browns.contains(nearBlock.getTypeId())) { nearBlock.setTypeId(brown); }
					//And ditto for red.
					if(reds.contains(nearBlock.getTypeId())) { nearBlock.setTypeId(red); }

					//If the block is in the rottable list,
					if(rots.contains(nearBlock.getTypeId())) {
						//then we randomly make it mycelium. This makes the spread much faster. Delicious.
						if(Math.random()<.1) { nearBlock.setType(Material.MYCEL); }
						//or we just turn it into dirt. Dirt that mycelium can spread onto and start the cycle again.
						else { nearBlock.setTypeId(rot); }
					}
					//If it's a vine, or any other removable block, we just set it to air.
					if(removes.contains(nearBlock.getTypeId())) { nearBlock.setTypeId(remove); }

					//Second bit o' fun. If the block is in our "I'm a log!" list, we schedule it to become a shroomstem later.
					if((!onLog) && logs.contains(nearBlock.getTypeId())) { scheduleRot(nearBlock); }
				}
				//And set the biome to be Mushroom Island. We only do this once per x,z place, to save time.
				block.getRelative(i,0,k).setBiome(Biome.MUSHROOM_ISLAND);
			}
		}
		//Finally, after we've made all our changes, we tell the server to update the biome on everybody's clients.
		//Kinda a cheat, since it only refreshes the chunk that the block started in.
		//But that doesn't too much matter, since we'll be updating the nearby chunks pretty soon...
		//BWAHAHAAH!
		int x = block.getChunk().getX();
		int z = block.getChunk().getZ();
		block.getWorld().refreshChunk(x, z);
		//Figure it out for yourself, or just accept it as magic. Either way.
	}
	
	private void scheduleRot(Block block) {
		scheduleRot(block, (int) (99+Math.round(Math.random())));
		
	}

	private void scheduleRot(final Block block, final int capColor) {
		//Remember how we checked to see if the nearBlock was a log? If it was, we send it down to here.
		//Here we schedule it to actually rot sometime in the next 10 seconds.
		//When it does rot, it'll use a slightly different function than the normal rot function.
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				rotLog(block, capColor);
			}
		}, (long) (Math.random()*200));
	}

	public void rotLog(Block block, int capColor) {
		//And this is the slightly different function that we use to rot logs.
		//First, we'll pull the "I'm a Log!" list from the config again.
		//I should probably streamline this so it doesn't have to check the config each time... Maybe?
		List<Integer> log = this.getConfig().getIntegerList("mush.logBlocks");
		
		//And if the block is still in the list (i.e., nobody harvested it/pistoned it away/whatever)
		if(!log.contains(block.getTypeId())) { return; }
		
		//Then we set it to Brown Mushroom Stem (block type id 99, block data 0x0)
		block.setTypeId(capColor);
		
		//Of course, if this block will spread its rot to another block, we won't consider it a cap.
		boolean lastLeaf = true;
		
		//same i,j,k = x,y,z as before, but this time we're only looking at the blocks immediately touching this one.
		for(int i=-1; i<=1; i++){
			for(int j=-1; j<=1; j++){
				for(int k=-1; k<=1; k++){
					//Again, we'll name the nearby block to nearBlock
					Block nearBlock = block.getRelative(i,j,k);
					
					//And we'll check to see if we can rot any farther.
					//If we can, we schedule it (remember the previous function) and we'll remark that this isn't in fact the last leaf.
					if(log.contains(nearBlock.getTypeId())) { scheduleRot(nearBlock, capColor); lastLeaf = false;}
				}
			}
		}
		
		//If it *is* the end of the line, we'll make it a mushroom cap.
		if(lastLeaf) { block.setData((byte) 14); cap(block, capColor); }
		
		//otherwise, it's just a stem.
		else { block.setData((byte) 15); }
		
		//And we'll see if there's any more rottable blocks nearby.
		//Note that this time the boolean is TRUE, that's because we don't want the rotBlock function to deal with nearby logs,
		//since we've already dealt with them in this function.
		rotBlock(block,true);
	}
	
	private void cap(Block block, int capColor) {
		//cap will make a mushroom cap.	

		List<Integer> offGround = this.getConfig().getIntegerList("mush.skyBlocks");
		
		if(!offGround.contains(block.getRelative(0, -3, 0).getTypeId())) { return; }

		//The maximum size of the cap.
		double radius = 2.5;
		
		if(capColor == 99) {
			//Same i and k as before. Brown caps are flat, so we don't have to think about j (or, y)
			for(int i=-2; i<=2; i++){
				for(int k=-2; k<=2; k++){
					//This gives the cap a roundish shape.
					if(Math.sqrt((i*i)+(k*k))>radius) { continue; }

					//Again, we'll name the nearby block to nearBlock
					Block nearBlock = block.getRelative(i,0,k);
					//make it a mushroom block
					nearBlock.setTypeId(capColor);
					//that's a cap-style block.
					nearBlock.setData((byte) 14);
				}
			}
		} else if (capColor==100) {
			//Same i, j, and k as before.
			for(int i=-1; i<=1; i++){
				for(int j=-1; j<=1; j++){
					for(int k=-1; k<=1; k++){
						if(j==1) {
							if(Math.sqrt((i*i)+(k*k))>1) { continue; }
							
							//Again, we'll name the nearby block to nearBlock
							Block nearBlock = block.getRelative(i,j,k);
							//make it a mushroom block
							nearBlock.setTypeId(capColor);
							//that's a cap-style block.
							nearBlock.setData((byte) 14);
						} else {
							if(Math.sqrt((i*i)+(k*k))<1){ continue; }
							
							//Again, we'll name the nearby block to nearBlock
							Block nearBlock = block.getRelative(i,j,k);
							//make it a mushroom block
							nearBlock.setTypeId(capColor);
							//that's a cap-style block.
							nearBlock.setData((byte) 14);
						}
					}
				}
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		//Finally, a pretty simple reload command. I don't think it works right now... I'll find out why!
		if(cmd.getName().equalsIgnoreCase("mushload")){
			this.reloadConfig();
			log("Config reloaded.");
			return true;
		}
		return false; 
	}

}
