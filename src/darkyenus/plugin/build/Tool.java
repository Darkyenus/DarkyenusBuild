package darkyenus.plugin.build;

import darkyenus.plugin.build.ParsingUtils.SyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static darkyenus.plugin.build.ParsingUtils.StringContainer;
import static darkyenus.plugin.build.ParsingUtils.startsWith;

/**
 *
 * @author Darkyen
 */
public class Tool {

    private final ArrayList<ActionPack> actionPacks = new ArrayList<>();
    private int id;

    public Tool(Tokenizer from) {
        if (!from.hasNext()) {
            throw new IllegalArgumentException("Specify arguments!");
        }

        ActionPack activeActionPack = new ActionPack();

        while (from.hasNext()) {
            final String tokenRaw = from.next();
            final String token = tokenRaw.toLowerCase();

            switch (token) {
                case "in":
                {
                    activeActionPack.add(Selector.createSelector(from));
                }
                break;
                case "where":
                case "vvhere":
                case "vhere":
                {
                    activeActionPack.add(Filter.createFilterExpression(from));
                }
                break;
                case "set":
                case "to":
                {
                    activeActionPack.add(Worker.createSimpleWorker(from));
                }
                break;
                case "do":
                {
                    //TODO Custom workers
                    //noinspection ConstantIfStatement,ConstantConditions
                    if(true) throw new SyntaxException("Invalid keyword: do");
                }
                break;
                case ";":
                case ",":
                {
                    if(activeActionPack.complete()){
                        actionPacks.add(activeActionPack);
                        activeActionPack = new ActionPack();
                    } else {
                        throw new SyntaxException("Action pack "+(actionPacks.size()+1)+" is incomplete");
                    }
                }
                break;
                default:
                {// Flags
                    //Custom flag
                    boolean flagEnabled = true;
                    String flag = token;
                    final StringContainer restOfFlag = new StringContainer();
                    if(startsWith(flag, restOfFlag, "-", "no", "!")){
                        flagEnabled = false;
                        flag = restOfFlag.string;
                    }

                    if (flag.startsWith("phys")) {
                        activeActionPack.setPhysicsFlag(flagEnabled);
                    } else {
                        throw new SyntaxException("Building block \"" + token + "\" not recognized.\n" +
                                "Expected: IN, WHERE, SET, DO, AND or a flag");
                    }
                }
            }
        }


        if(activeActionPack.complete()){
            actionPacks.add(activeActionPack);
        }

        if(actionPacks.size() == 0){
            throw new SyntaxException("Missing any actions");
        }
        //Done!
    }

    public Change processClick(Player player, BlockFace blockFace, Location at) {
        Change change = new Change(player.getWorld());
        for (ActionPack actionPack : actionPacks) {
            actionPack.processClick(player, blockFace, change, at);
        }
        return change;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    public void sendInfo(CommandSender sender) {
        for (int i = 0; i < actionPacks.size(); i++) {
            sender.sendMessage(ChatColor.BLUE + " ActionPack " + ChatColor.WHITE + i);
            actionPacks.get(i).sendInfo(sender);
        }
    }

    private static final class ActionPack {

        public void sendInfo(CommandSender sender) {
            sender.sendMessage(ChatColor.BLUE + "  In");
            for (Selector selector : selectors) {
                selector.sendInfo(sender);
            }
            if(!filters.isEmpty()){
                sender.sendMessage(ChatColor.BLUE + "  Where");
                for (Filter filter : filters) {
                    sender.sendMessage("   " + filter.getInfo());
                }
            }
            sender.sendMessage(ChatColor.BLUE + "  Do");
            for (Worker worker : workers) {
                worker.sendInfo(sender);
            }
            sender.sendMessage(ChatColor.BLUE + "  Physics: " + ChatColor.WHITE + physicsFlag);
        }

        private final List<Selector> selectors = new ArrayList<>();
        private final List<Filter> filters = new ArrayList<>();
        private final List<Worker> workers = new ArrayList<>();
        private boolean physicsFlag = true;

        public void add(Selector selector) {
            selectors.add(selector);
        }

        public void add(Filter filter) {
            filters.add(filter);
        }

        public void add(Worker worker) {
            workers.add(worker);
        }

        public void setPhysicsFlag(boolean physics){
            this.physicsFlag = physics;
        }

        /**
         * Try to complete this ActionPack with default values and check
         * integrity.
         *
         * @return if completion was successful and is ready to be used
         */
        public boolean complete() {
            if (workers.isEmpty()) {
                return false;
            }
            if (selectors.isEmpty()) {
                selectors.add(Selector.createDefaultSelector());
            }
            return true;
        }

        public void processClick(final Player player, final BlockFace blockFace, Change change, final Location location) {
            final boolean physicsFlag = this.physicsFlag;
            final BlockAggregate aggregate = new BlockAggregate(player.getWorld(), filters);

            //Gather filtered blocks
            for (Selector selector : selectors) {
                selector.getBlocks(aggregate, player, location, blockFace);
            }

            //Perform actions
            for (Worker worker : workers) {
                worker.processBlocks(change, aggregate, physicsFlag);
            }
        }
    }

    public static final class BlockAggregate {
        private final World world;
        private final List<Filter> filters;
        private final List<Block> blocks = new ArrayList<>();

        public BlockAggregate(World world, List<Filter> filters) {
            this.world = world;
            this.filters = filters;
        }

        public void add(int x, int y, int z){
            add(world.getBlockAt(x, y, z));
        }

        public void add(Block block){
            if(block == null) return;
            for (Filter filter : filters) {
                if(!Filter.passesFilter(filter, block)){
                    return;
                }
            }
            blocks.add(block);
        }

        Iterable<Block> blocks() {
            return blocks;
        }
    }
}
