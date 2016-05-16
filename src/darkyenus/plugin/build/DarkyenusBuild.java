package darkyenus.plugin.build;

import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Darkyen
 */
public class DarkyenusBuild extends JavaPlugin implements Listener {

    public static final String BUILD_TOOL_NAME = ChatColor.GOLD + "Building Pickaxe";
    public static final String BUILD_TOOL_LORE_PREFIX = ChatColor.GRAY + "Tool #";

    //-----------------------Plugin stuff

    @Override
    public void onDisable() {
        getLogger().info("Disabled !");
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Enabled !");
    }
    //----------------------Fun stuff
    private HashMap<String, ArrayList<Change>> changes = new HashMap<>();
    private HashMap<Integer, Tool> tools = new HashMap<>();
    private int nextToolID = 0;
    private HashMap<String, PlayerSettings> settings = new HashMap<>();

    private PlayerSettings getSettings(Player player) {
        if (!settings.containsKey(player.getName())) {
            settings.put(player.getName(), new PlayerSettings());
        }

        return settings.get(player.getName());
    }

    private ArrayList<Change> getChanges(Player player) {
        if (!changes.containsKey(player.getName())) {
            changes.put(player.getName(), new ArrayList<>());
        }

        return changes.get(player.getName());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equals("build")) {
                if (args.length != 0) {
                    final Tool inHand = getToolInHandOfPlayer(player);

                    if (inHand != null) {
                        try {
                            Tool newTool = new Tool(new Tokenizer(args));
                            newTool.setId(inHand.getId());
                            tools.put(inHand.getId(), newTool);
                            player.sendMessage(ChatColor.GREEN + "Tool modified!");
                            newTool.sendInfo(player);
                        } catch (ParsingUtils.SyntaxException e) {
                            player.sendMessage(ChatColor.RED + "Syntax error: " + ChatColor.WHITE + ChatColor.ITALIC + e.getMessage());
                        }
                    } else {
                        if(player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                            try {
                                Tool newTool = new Tool(new Tokenizer(args));
                                newTool.setId(nextToolID);
                                tools.put(nextToolID, newTool);
                                ItemStack toolItemStack = new ItemStack(Material.GOLD_PICKAXE, 1);
                                ItemMeta meta = toolItemStack.getItemMeta();
                                meta.setDisplayName(BUILD_TOOL_NAME);
                                meta.setLore(Collections.singletonList(BUILD_TOOL_LORE_PREFIX + nextToolID));
                                toolItemStack.setItemMeta(meta);
                                nextToolID++;
                                player.getInventory().setItem(player.getInventory().getHeldItemSlot(), toolItemStack);
                                player.sendMessage(ChatColor.GREEN + "Tool created!");
                                newTool.sendInfo(player);
                            } catch (ParsingUtils.SyntaxException e) {
                                player.sendMessage(ChatColor.RED + "Syntax error: " + ChatColor.WHITE + ChatColor.ITALIC + e.getMessage());
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Empty your hands first.");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.BLUE+"Command for creating and modifying existing tools");
                    player.sendMessage("Use "+ChatColor.ITALIC+"/darkyenusbuild help"+ChatColor.RESET+" for more info");
                }
                return true;
            } else if (command.getName().equals("darkyenusbuild")) {
                if(args.length == 0){
                    sendHelpTopic(sender, "");
                } else {
                    sendHelpTopic(sender, args[0]);
                }
                return true;
            } else if (command.getName().equals("tool")) {
                Tool inHand = getToolInHandOfPlayer(player);
                if(inHand == null){
                    player.sendMessage(ChatColor.RED+"You are not holding any tool.");
                }else{
                    inHand.sendInfo(player);
                }
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "In game use only");
            return true;
        }
        return false;
    }

    private void sendHelpTopic(CommandSender sender, String topic){
        switch (topic.toLowerCase()) {
            case "about":
                sender.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "Darkyenus Build " + getDescription().getVersion());
                sender.sendMessage(ChatColor.BLUE + "Created by " + ChatColor.GOLD + ChatColor.BOLD + "Darkyen");
                sender.sendMessage(ChatColor.BLUE.toString() + ChatColor.ITALIC + "   (c) 2013 - 2016 darkyen@me.com");
                break;
            case "quickstart":
                sender.sendMessage(ChatColor.AQUA+"/build in box 3x3x3 to cobblestone");
                sender.sendMessage(ChatColor.BLUE+"- This command will create a tool that will build 3x3x3 cobblestone boxes where you tell it to. Left click to build, right click to undo.");
                sender.sendMessage(ChatColor.BLUE+"- For more shapes, see /dbld selectors");
                sender.sendMessage(ChatColor.BLUE+"- For more actions, see /dbld workers");
                sender.sendMessage(ChatColor.BLUE+"- For more advanced tools, see /dbld spec");
                break;
            case "selectors":
                sender.sendMessage(ChatColor.UNDERLINE+"cube"+ChatColor.RESET+" width"+ChatColor.ITALIC+"x"+ChatColor.RESET+"height"+ChatColor.ITALIC+"x"+ChatColor.RESET+"depth"+ChatColor.AQUA+" 3D cube centered on cursor");
                sender.sendMessage(ChatColor.UNDERLINE+"rectangle"+ChatColor.RESET+" width"+ChatColor.ITALIC+"x"+ChatColor.RESET+"height"+ChatColor.AQUA+" 2D cube centered on cursor, oriented on target face");
                sender.sendMessage(ChatColor.UNDERLINE+"square"+ChatColor.RESET+" width/height"+ChatColor.AQUA+" like rectangle, with equal side sizes");
                sender.sendMessage(ChatColor.UNDERLINE+"sphere"+ChatColor.RESET+" radius"+ChatColor.AQUA+" 3D sphere centered on cursor");
                sender.sendMessage(ChatColor.UNDERLINE+"disc"+ChatColor.RESET+" radius"+ChatColor.AQUA+" 2D disc centered on cursor, oriented on target face");
                sender.sendMessage(ChatColor.UNDERLINE+"column"+ChatColor.RESET+" length"+ChatColor.AQUA+" 1D column from cursor, oriented on target face (negative length supported)");
                sender.sendMessage(ChatColor.UNDERLINE+"floor"+ChatColor.RESET+" length"+ChatColor.AQUA+" 1D column from cursor, oriented on target face but always horizontal (negative length supported)");
                sender.sendMessage(ChatColor.UNDERLINE+"wall"+ChatColor.RESET+" length"+ChatColor.AQUA+" 1D column from cursor, always up (negative length supported)");
                sender.sendMessage(ChatColor.UNDERLINE+"chunk"+ChatColor.AQUA+" whole chunk, from bottom to top of the world");
                sender.sendMessage(ChatColor.UNDERLINE+"chunklayer"+ChatColor.RESET+" height"+ChatColor.AQUA+" slice of targeted chunk, from target up (negative length supported)");
                break;
            case "workers":
                sender.sendMessage(ChatColor.AQUA+"Basic worker: [block/biome] <material or biome name>");
                sender.sendMessage(ChatColor.BLUE+"Basic workers modify blocks to specified material or biome. Optional \"block\" or \"biome\" (space separated) may be specified before the name to resolve ambiguity. By default, only blocks are selected.");
                sender.sendMessage(ChatColor.BLUE+"Materials may have ':DATA' appended to also modify data. For example \"wool:11\" for blue wool.");
                sender.sendMessage(ChatColor.BLUE+"When modifying biomes, note that the change will be visible only after chunk reload.");
                break;
            case "specification":
            case "spec":
                sender.sendMessage(ChatColor.AQUA+"/build <action packs>");
                sender.sendMessage(ChatColor.AQUA+"<action pack> = IN [selector]+ WHERE [filter]+ SET [worker]+");
                sender.sendMessage(ChatColor.BLUE+"A tool may have one or more action packs, which is a union of all selectors, filtered by specified filters and acted on by workers. Action pack elements may be specified in any order. Action packs themselves are separated by a ; (which must have spaces on both sides). Each selector must be prefixed with IN, each filter with WHERE and each (basic) worker with SET or TO.");
                sender.sendMessage(ChatColor.BLUE+"See [commands]");
                break;
            case "commands":
                sender.sendMessage(ChatColor.AQUA+"/build ...");
                sender.sendMessage(ChatColor.BLUE+"Will create new tool or change the one already in your (main) hand. See [spec].");
                sender.sendMessage(ChatColor.AQUA+"/tool ...");
                sender.sendMessage(ChatColor.BLUE+"Will print info about the tool in your hand");
                break;
            case "filters":
                sender.sendMessage(ChatColor.AQUA+"Basic unit filter: [block/biome] <material or biome name>");
                sender.sendMessage(ChatColor.BLUE+"Filters are simple expressions build from blocks looking not unlike basic workers. Selected block only passes the test when all filters pass it through, which, in case of basic filter, means that the existing block is the same as specified. Data value is checked only when explicitly specified. Filters may be chained into expressions, see [filter-expressions].");
                break;
            case "filter-expressions":
                sender.sendMessage(ChatColor.AQUA+"[NOT] FILTER [[AND/OR/NOR/XOR] [NOT] FILTER]+");
                sender.sendMessage(ChatColor.BLUE+"Filter expressions are a powerful way to specify which blocks should be modified. For this, boolean operators are used. They are evaluated in line, with no order of preference, except for NOT, which always binds ONLY to the expression right after it.");
                sender.sendMessage(ChatColor.AQUA+"Ex: cobblestone or not biome ocean");
                sender.sendMessage(ChatColor.BLUE+"Will modify cobblestone blocks in ocean biome and all blocks elsewhere");
                break;
            default:
                sender.sendMessage(ChatColor.BLUE+"Available help topics:");
                sender.sendMessage(ChatColor.ITALIC+"quickstart, commands, selectors, workers, specification, filters, filter-expressions, about");
        }
    }

    @EventHandler
    public void onAction(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL && event.getPlayer().hasPermission("darkyenusbuild")) {
            if (!event.isBlockInHand() && (!event.getPlayer().isSneaking() || event.getPlayer().isFlying()) && event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                Tool tool = getToolAssociatedWithItem(event.getItem());
                if (tool != null) {
                    event.setUseInteractedBlock(Event.Result.DENY);
                    event.setUseItemInHand(Event.Result.DENY);
                    event.setCancelled(true);

                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        //Build
                        Block clicked = event.getClickedBlock();
                        BlockFace face = event.getBlockFace();
                        if (clicked == null || face == null) {
                            boolean airBrushing = getSettings(event.getPlayer()).isAirBrush();
                            int range = getSettings(event.getPlayer()).getRange();
                            List<Block> inLine = event.getPlayer().getLastTwoTargetBlocks((Set<Material>)null, airBrushing ? range + range / 2 : 150);
                            if (inLine.size() < 2) {
                                event.getPlayer().sendMessage(ChatColor.RED.toString() + ChatColor.ITALIC + "Range too short to determine clicked blocks!");
                                return;
                            } else {
                                clicked = inLine.get(inLine.size()-1);
                                face = clicked.getFace(inLine.get(inLine.size()-2));
                                if (clicked.getType() == Material.AIR && !airBrushing) {
                                    event.getPlayer().sendMessage(ChatColor.RED.toString() + ChatColor.ITALIC + "There aren't nearby blocks there");
                                    return;
                                }
                            }
                        }
                        Change change = tool.processClick(event.getPlayer(), face, clicked.getLocation());
                        if(change.getSize() != 0){
                            ArrayList<Change> playerChanges = changes.get(event.getPlayer().getName());
                            if(playerChanges == null){
                                playerChanges = new ArrayList<>();
                            }
                            playerChanges.add(change);
                            changes.put(event.getPlayer().getName(), playerChanges);
                        }
                        if(getSettings(event.getPlayer()).isVerbose()){
                            if(change.getSize() == 0){
                                event.getPlayer().sendMessage(ChatColor.AQUA.toString()+ChatColor.ITALIC.toString()+"Nothing to change");
                            } else {
                                event.getPlayer().sendMessage(ChatColor.BLUE.toString()+ChatColor.ITALIC.toString()+change.getSize()+" blocks changed");
                            }
                        }
                    } else {
                        //Undo
                        int nextIndex = getChanges(event.getPlayer()).size() - 1;
                        if (nextIndex < 0) {
                            event.getPlayer().sendMessage(ChatColor.RED + "Nothing to undo");
                        } else {
                            int changedBlocks = getChanges(event.getPlayer()).remove(nextIndex).revert();
                            event.getPlayer().sendMessage(ChatColor.GREEN.toString() + changedBlocks + " blocks changed back");
                        }
                    }
                }
            }
        }
    }

    public Tool getToolInHandOfPlayer(Player player) {
        return getToolAssociatedWithItem(player.getInventory().getItemInMainHand());
    }

    public Tool getToolAssociatedWithItem(ItemStack item) {
        if (item != null && item.getType() == Material.GOLD_PICKAXE) {
            if (item.hasItemMeta()) {
                if (BUILD_TOOL_NAME.equals(item.getItemMeta().getDisplayName())) {
                    List<String> lore = item.getItemMeta().getLore();
                    if (lore.size() == 1) {
                        String loreText = lore.get(0);
                        if (loreText.startsWith(BUILD_TOOL_LORE_PREFIX)) {
                            String loreSuffix = loreText.substring(BUILD_TOOL_LORE_PREFIX.length());
                            try {
                                int pickID = Integer.parseInt(loreSuffix);
                                return tools.get(pickID);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
