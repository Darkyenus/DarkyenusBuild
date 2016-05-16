/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package darkyenus.plugin.build;

import java.util.ArrayList;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 *
 * @author Darkyen
 */
public class Change {
    private HashSet<Location> snapshottedBlocks = new HashSet<Location>();
    private ArrayList<BlockSnapshot> before = new ArrayList<BlockSnapshot>();
    
    public int changeBack(){
        for(BlockSnapshot state:before){
            state.revert();
        }
        return before.size();
    }
    
    public int getSize(){
        return before.size();
    }
    
    public void snapshotBlock(Block block){
        if(!snapshottedBlocks.contains(block.getLocation())){
            before.add(new BlockSnapshot(block));
            snapshottedBlocks.add(block.getLocation());
        }
    }
    
    private class BlockSnapshot{
        Location location;
        Material material;
        byte data;
        Biome biome;
        BlockState state;//This may be null
        
        public BlockSnapshot(Block block){
            location = block.getLocation();
            material = block.getType();
            data = block.getData();
            biome = block.getBiome();
            state = block.getState();
        }
        
        public void revert(){
            final Block block = location.getBlock();
            block.setType(material, false);
            block.setData(data, false);
            block.setBiome(biome);
            if(state != null) {
                state.update(true);
            }
        }
    }
}
