package darkyenus.plugin.build;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.Iterator;

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
                return createBlockWorker(result.material);
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
                    delegate.changeMaterial(delegate.next(), material);
                }
            }

            @Override
            public void sendInfo(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "    Set material to: " + ChatColor.WHITE + material);
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
