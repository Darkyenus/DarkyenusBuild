package darkyenusbuild;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Darkyen
 */
public abstract class Worker {

    public final void processBlocks(Change change, Tool.BlockAggregate blocks, boolean applyPhysics) {
        processBlocks(new WorkerDelegate(change, blocks, applyPhysics));
    }

    abstract void processBlocks(WorkerDelegate delegate);

    public abstract void sendInfo(CommandSender sender);

    public static Worker createSimpleWorker(Tokenizer tokenizer) throws ParsingUtils.SyntaxException {
        final AttributeMatcher.Result result = AttributeMatcher.matchAttribute(tokenizer);
        switch (result.type) {
            case MATERIAL:
                if(result.data == MatchUtils.MaterialSpec.DATA_INHERIT) {
                    return createBlockWorkerWithForceInheritedData(result.material);
                } else {
                    return createBlockWorker(result.material);
                }
            case MATERIAL_WITH_DATA:
                return createBlockWorker(result.material, result.data);
            case BIOME:
                return createBiomeWorker(result.biome);
            case ERROR:
                throw new ParsingUtils.SyntaxException(result.error);
            default:
                throw new ParsingUtils.SyntaxException("Something went wrong, matcher returned "+result);
        }
    }

    private static Worker createBlockWorker(Material material) {
        return new Worker() {
            @Override
            public void processBlocks(WorkerDelegate delegate) {
                while (delegate.hasNext()) {
                    final Block next = delegate.next();
                    if (next.getType().getData().equals(material.getData())) {
                        delegate.changeMaterial(next, material, next.getState().getRawData());
                    } else {
                        delegate.changeMaterial(next, material);
                    }
                }
            }

            @Override
            public void sendInfo(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "    Set material to: " + ChatColor.WHITE + material);
                sender.sendMessage(ChatColor.BLUE + "    Set data to: " + ChatColor.WHITE + "default");
            }
        };
    }

    private static Worker createBlockWorker(Material material, int data) {
        return new Worker() {
            @Override
            public void processBlocks(WorkerDelegate delegate) {
                while (delegate.hasNext()) {
                    final Block next = delegate.next();
                    delegate.changeMaterial(next, material, data);
                }
            }

            @Override
            public void sendInfo(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "    Set material to: " + ChatColor.WHITE + material);
                sender.sendMessage(ChatColor.BLUE + "    Set data to: " + ChatColor.WHITE + data);
            }
        };
    }

    private static Worker createBlockWorkerWithForceInheritedData(Material material) {
        return new Worker() {
            @Override
            public void processBlocks(WorkerDelegate delegate) {
                while (delegate.hasNext()) {
                    final Block next = delegate.next();
                    delegate.changeMaterial(next, material, next.getData());
                }
            }

            @Override
            public void sendInfo(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "    Set material to: " + ChatColor.WHITE + material);
                sender.sendMessage(ChatColor.BLUE + "    Don't change data");
            }
        };
    }

    private static Worker createBiomeWorker(Biome biome) {
        return new Worker() {
            @Override
            public void processBlocks(WorkerDelegate delegate) {
                while (delegate.hasNext()) {
                    delegate.changeBiome(delegate.next(), biome);
                }
            }

            @Override
            public void sendInfo(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "    Set biome to: " + ChatColor.WHITE + biome);
            }
        };
    }

    public static Worker createSpecialWorker(Tokenizer tokenizer) throws ParsingUtils.SyntaxException {
        if(!tokenizer.hasNext()) throw new ParsingUtils.SyntaxException("Worker specifier missing after DO");
        final String workerSpec = tokenizer.next();
        switch (workerSpec.toLowerCase()){
            case "regenerate":
                return new Worker() {
                    @Override
                    void processBlocks(WorkerDelegate delegate) {
                        final Set<Chunk> chunks = new HashSet<>();
                        while(delegate.hasNext()){
                            chunks.add(delegate.next().getChunk());
                        }
                        for(Chunk chunk:chunks){
                            delegate.change.snapshotChunk(chunk);
                            //Chunk regeneration may affect neighboring chunks as well, so we need to snapshot them
                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX()+1, chunk.getZ()+1));
                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX()+1, chunk.getZ()));
                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX()+1, chunk.getZ()-1));

                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ()+1));
                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ()));
                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ()-1));

                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX()-1, chunk.getZ()+1));
                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX()-1, chunk.getZ()));
                            delegate.change.snapshotChunk(chunk.getWorld().getChunkAt(chunk.getX()-1, chunk.getZ()-1));
                        }
                        for (Chunk chunk : chunks) {
                            chunk.getWorld().regenerateChunk(chunk.getX(), chunk.getZ());
                        }
                    }

                    @Override
                    public void sendInfo(CommandSender sender) {
                        sender.sendMessage(ChatColor.BLUE + "    Regenerate whole chunk");
                    }
                };
            case "erode":
                throw new ParsingUtils.SyntaxException("Erode not yet implemented");
            default:
                throw new ParsingUtils.SyntaxException("Unrecognized worker "+workerSpec);
        }
    }

    private static final class WorkerDelegate {

        private final Change change;
        private final Iterator<Block> blocks;
        private final boolean applyPhysics;

        WorkerDelegate(Change change, Tool.BlockAggregate blocks, boolean applyPhysics) {
            this.change = change;
            this.blocks = blocks.blocks().iterator();
            this.applyPhysics = applyPhysics;
        }

        boolean hasNext() {
            return blocks.hasNext();
        }

        Block next() {
            return blocks.next();
        }

        void changeMaterial(Block block, Material material) {
            final Material existingMaterial = block.getType();
            if(existingMaterial == material){
                return;
            }

            change.snapshotBlock(block);
            block.setType(material, applyPhysics);
        }

        @SuppressWarnings("deprecation")
        void changeMaterial(Block block, Material material, int data) {
            final Material existingMaterial = block.getType();
            final byte existingData = block.getData();
            if (existingMaterial == material && existingData == data) {
                return;
            }

            change.snapshotBlock(block);
            block.setType(material, applyPhysics);
            block.setData((byte) data, applyPhysics);
        }

        void changeBiome(Block block, Biome biome) {
            change.changeBiome(block.getX(), block.getZ(), biome);
        }
    }
}
