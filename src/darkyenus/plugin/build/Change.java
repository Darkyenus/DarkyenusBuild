package darkyenus.plugin.build;

import java.util.*;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 *
 * @author Darkyen
 */
public class Change {

    private final Set<Location> changedBlockLocations = new HashSet<>();
    private final List<BlockSnapshot> before = new ArrayList<>();

    private final World world;
    private final TLongObjectMap<Biome> originalBiomes = new TLongObjectHashMap<>();

    public Change(World world) {
        this.world = world;
    }

    private static long biomeLocationKey(int x, int z){
        return ((long)x << 32) | (z & 0xFFFF_FFFFL);
    }

    private static int biomeLocationKeyX(long key){
        return (int) (key >> 32);
    }

    private static int biomeLocationKeyZ(long key){
        return (int)(key & 0xFFFF_FFFFL);
    }

    public void changeBiome(int x, int z, Biome biome) {
        final Biome currentBiome = world.getBiome(x, z);
        if(currentBiome != biome){
            final long key = biomeLocationKey(x, z);
            if(!originalBiomes.containsKey(key)){
                originalBiomes.put(key, currentBiome);
            }
            world.setBiome(x, z, biome);
        }
    }

    public int revert(){
        final int totalChanges = originalBiomes.size() + before.size();

        originalBiomes.forEachEntry((key, biome) -> {
            world.setBiome(biomeLocationKeyX(key), biomeLocationKeyZ(key), biome);
            return true;
        });
        originalBiomes.clear();

        for(BlockSnapshot state:before) {
            state.revert();
        }

        return totalChanges;
    }
    
    public int getSize(){
        return before.size();
    }
    
    public void snapshotBlock(Block block){
        if(!changedBlockLocations.contains(block.getLocation())){
            before.add(new BlockSnapshot(block));
            changedBlockLocations.add(block.getLocation());
        }
    }
    
    private class BlockSnapshot{
        private final Location location;
        private final Material material;
        private final byte data;
        private final BlockState state;//This may be null
        
        public BlockSnapshot(Block block){
            location = block.getLocation();
            material = block.getType();
            data = block.getData();
            state = block.getState();
        }
        
        public void revert(){
            final Block block = location.getBlock();
            block.setType(material, false);
            block.setData(data, false);
            if(state != null) {
                state.update(true, false);
            }
        }
    }
}
