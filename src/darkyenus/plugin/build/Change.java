package darkyenus.plugin.build;

import java.util.*;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.bukkit.*;
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
    private final List<ChunkSnapshot> beforeChunks = new ArrayList<>();

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
        final World world = this.world;
        final int totalChanges = getSize();

        originalBiomes.forEachEntry((key, biome) -> {
            world.setBiome(biomeLocationKeyX(key), biomeLocationKeyZ(key), biome);
            return true;
        });

        for(BlockSnapshot state:before) {
            state.revert();
        }

        for (ChunkSnapshot snapshot : beforeChunks) {
            final int chunkX = snapshot.getX() << 4;
            final int chunkZ = snapshot.getZ() << 4;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    world.setBiome(chunkX + x, chunkZ + z, snapshot.getBiome(x,z));
                    for (int y = 0; y < world.getMaxHeight(); y++) {
                        world.getBlockAt(chunkX + x, y, chunkZ + z).setTypeIdAndData(snapshot.getBlockTypeId(x,y,z), (byte)snapshot.getBlockData(x,y,z), false);
                    }
                }
            }
        }

        return totalChanges;
    }
    
    public int getSize(){
        return before.size() + originalBiomes.size() + 16*16*world.getMaxHeight()*beforeChunks.size();
    }
    
    public void snapshotBlock(Block block){
        if(!changedBlockLocations.contains(block.getLocation())){
            before.add(new BlockSnapshot(block));
            changedBlockLocations.add(block.getLocation());
        }
    }

    public void snapshotChunk(Chunk chunk) {
        for (ChunkSnapshot snapshot : beforeChunks) {
            if(snapshot.getX() == chunk.getX() && snapshot.getZ() == chunk.getZ()) return;
        }
        beforeChunks.add(chunk.getChunkSnapshot(false, true, false));
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
