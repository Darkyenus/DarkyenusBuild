package darkyenus.plugin.build;

import darkyenus.plugin.build.ParsingUtils.SyntaxException;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static darkyenus.plugin.build.ParsingUtils.StringContainer;
import static darkyenus.plugin.build.ParsingUtils.startsWith;

/**
 *
 * @author Darkyen
 */
public final class Tool {

    private final ArrayList<ActionPack> actionPacks = new ArrayList<>();
    private final String source;
    private final int id;

    private long lastClick = 0;

    private Tool(int id, CommandSender creator, Tokenizer from) {
        this.id = id;
        if (!from.hasNext()) {
            throw new IllegalArgumentException("Specify arguments!");
        }
        source = from.serialize();

        ActionPack activeActionPack = new ActionPack();

        while (from.hasNext()) {
            final String tokenRaw = from.next();
            final String token = tokenRaw.toLowerCase();

            switch (token) {
                case "in":
                {
                    activeActionPack.add(Selector.createSelector(creator, from));
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

    public static final Material BUILDING_ITEM_MATERIAL = Material.GOLD_PICKAXE;
    public static final String BUILDING_ITEM_NAME = ChatColor.GOLD+"Building Tool";

    private static final Random random = new Random();
    private static int nextToolID(){
        int id;
        do {
            id = random.nextInt(0x10000000);
        } while (loadedTools.containsKey(id));
        return id;
    }
    private static final TIntObjectMap<Tool> loadedTools = new TIntObjectHashMap<>();

    public static Tool createNewTool(CommandSender creator, Tokenizer from) throws SyntaxException {
        final int id = nextToolID();
        final Tool tool = new Tool(id, creator, from);
        loadedTools.put(id, tool);
        return tool;
    }

    /** Retrieves created or dematerializes a tool associated with this item stack. */
    public static Tool getTool(CommandSender creator, ItemStack item){
        if(item == null)return null;
        if (item.getType() != BUILDING_ITEM_MATERIAL) return null;//Not a building tool, invalid material
        final ItemMeta itemMeta = item.getItemMeta();
        if(!Objects.equals(itemMeta.getDisplayName(), BUILDING_ITEM_NAME)) return null;//Not a building tool, invalid name
        if(!itemMeta.hasLore())return null;//Not a building tool, missing lore
        final List<String> lore = itemMeta.getLore();
        if(lore.size() < 2) return null;//Not a building tool, invalid lore size
        int id;
        try {
            id = Integer.parseInt(lore.get(0), 16);
        } catch (NumberFormatException e) {
            return null;//Not a building tool, missing ID
        }
        final String source = lore.get(1);

        final Tool existingTool = loadedTools.get(id);
        if(existingTool != null) {
            if(source.equals(existingTool.source)){
                //Tool exists
                return existingTool;
            } else {
                //This tool was created before and has the same ID as already created tool!
                id = nextToolID();
                lore.set(0, Integer.toString(id, 16));
                itemMeta.setLore(lore);
                item.setItemMeta(itemMeta);
            }
        }
        //This tool does not exist in this session yet, create it
        final Tool newTool;
        try {
            newTool = new Tool(id, creator, new Tokenizer(source.split(" ")));
        } catch (SyntaxException e) {
            lore.add(e.getMessage());
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);
            return null;
        } catch (Exception e) {
            DarkyenusBuild.LOG.warning("Serialized build tool with source \""+source+"\" threw exception on deserialization");
            DarkyenusBuild.LOG.throwing("Tool","getTool", e);
            return null;
        }
        loadedTools.put(id, newTool);
        return newTool;
    }

    public ItemStack createItem(){
        final ItemStack stack = new ItemStack(BUILDING_ITEM_MATERIAL, 1);
        final ItemMeta itemMeta = stack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS);
        itemMeta.setDisplayName(BUILDING_ITEM_NAME);
        itemMeta.setLore(Arrays.asList(Integer.toString(id, 16), source));
        stack.setItemMeta(itemMeta);
        return stack;
    }

    public boolean canProcessClickNow(long delay){
        if(lastClick + delay < System.currentTimeMillis()){
            lastClick = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }

    public Change processClick(Player player, BlockFace blockFace, Location at) {
        Change change = new Change(player.getWorld());
        for (ActionPack actionPack : actionPacks) {
            actionPack.processClick(player, blockFace, change, at);
        }
        return change;
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
