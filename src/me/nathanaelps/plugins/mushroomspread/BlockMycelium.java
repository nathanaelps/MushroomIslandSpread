package me.nathanaelps.plugins.mushroomspread;

import java.util.Random;

import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import net.minecraft.server.Block;
import net.minecraft.server.BlockMycel;
import net.minecraft.server.World;

public class BlockMycelium extends BlockMycel{
	//Mmm. Fun stuff here, we're going to extend an NMS class. This stuff is "deeper" than Bukkit, so it gets hairy sometimes.
	
	protected BlockMycelium(int i) {
		//First, we make a constructor, then tell it to deal with its super.
		//This basically says that we're not changing the basic structure of Mycelium, just some details,
		//since we can still use the former constructor.
		super(i);
	}

	@Override
    public void b(World world, int x, int y, int z, Random random) {
		//Mmm. b(World, int, int, int, Random) is the "how do I spread?" check.
		//I bet it's actually the block tick function, I'll dig into that later.
		
		//I don't know why we're checking isStatic. This is in the original class and I decided not to mess with it until I understood it.
		if (!world.isStatic) {
			
            if (
            		world.getLightLevel(x, y + 1, z) < 2
//            		&& Block.lightBlock[world.getTypeId(nearX, nearY + 1, nearZ)] > 2
            	) {
                // CraftBukkit start
                org.bukkit.World bworld = world.getWorld();
                BlockState blockState = bworld.getBlockAt(x, y, z).getState();
                blockState.setTypeId(Block.DIRT.id);

                BlockFadeEvent event = new BlockFadeEvent(blockState.getBlock(), blockState);
                world.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    blockState.update(true);
                }
                // CraftBukkit end
                return;
                
            }			
			
			//Now, create a for loop, since we want to try spreading in more directions than 1.
			for (int i = 0; i < 4; ++i) {
				
				//Which block do we want to infect?
				int nearX = x + random.nextInt(5) - 2;
				int nearY = y + random.nextInt(5) - 3;
				int nearZ = z + random.nextInt(5) - 2;
				
				//? IDK. It's in NMS.
//				int aboveNearBlock = world.getTypeId(nearX, nearY + 1, nearZ);
				
				//This is a shorthand, used in the if-statement.
				int nearBlockTypeId = world.getTypeId(nearX, nearY, nearZ);
				if (
						//Now, if the nearby block is dirt or grass,
						(
							nearBlockTypeId == Block.GRASS.id ||
							nearBlockTypeId == Block.SAND.id  ||
							nearBlockTypeId == Block.DIRT.id
						)
						//and it's bright enough
						&& world.getLightLevel(nearX, nearY + 1, nearZ) >= 4  
						//and it's not covered in water,
//						&& Block.lightBlock[aboveNearBlock] <= 2
					) {
					
					//Then we trigger the Bukkit BlockSpreadEvent that we catch in the main plugin.
					//This is the Bukkit team's work here, you can pretty well read it, I think.
					// CraftBukkit start
					org.bukkit.World bworld = world.getWorld();
					BlockState blockState = bworld.getBlockAt(nearX, nearY, nearZ).getState();
					blockState.setTypeId(this.id);

					BlockSpreadEvent event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(x, y, z), blockState);
					world.getServer().getPluginManager().callEvent(event);

					if (!event.isCancelled()) {
						blockState.update(true);
					}
					// CraftBukkit end
				}
			}
		}
    }
	
    protected net.minecraft.server.Block setDurability(float durability) {
        this.strength = durability;
        if (this.durability < durability * 5.0F) {
            this.durability = durability * 5.0F;
        }

        return this;
    }
}
