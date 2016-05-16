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

    public static final String OFFICIAL_TOOL_NAME = ChatColor.GOLD + "Building Pickaxe";
    public static final String OFFICIAL_LORE_PREFIX = ChatColor.GRAY + "Tool #";
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
                    Tool inHand = getToolAssociatedWithItem(player.getItemInHand());
                    if ((player.getItemInHand() == null || player.getItemInHand().getTypeId() == 0 || player.getItemInHand().getAmount() == 0) || inHand != null) {
                        try {
                            if (inHand != null) {
                                if (inHand.getCreator().equals(player.getName())) {
                                    Tool newTool = new Tool(new Tokenizer(args));
                                    newTool.setId(inHand.getId());
                                    newTool.setCreator(inHand.getCreator());
                                    tools.put(inHand.getId(), newTool);
                                    player.sendMessage(ChatColor.GREEN + "Tool modified!");
                                } else {
                                    player.sendMessage(ChatColor.RED + "You cannot modify someone else's tool.");
                                }
                            } else {
                                Tool newTool = new Tool(new Tokenizer(args));
                                newTool.setId(nextToolID);
                                newTool.setCreator(player.getName());
                                tools.put(nextToolID, newTool);
                                ItemStack toolItemStack = new ItemStack(Material.GOLD_PICKAXE, 1);
                                ItemMeta meta = toolItemStack.getItemMeta();
                                meta.setDisplayName(OFFICIAL_TOOL_NAME);
                                meta.setLore(Collections.singletonList(OFFICIAL_LORE_PREFIX + nextToolID));
                                toolItemStack.setItemMeta(meta);
                                nextToolID++;
                                player.getInventory().setItem(player.getInventory().getHeldItemSlot(), toolItemStack);
                                player.sendMessage(ChatColor.GREEN + "Tool created!");
                            }
                        } catch (ParsingUtils.SyntaxException e) {
                            player.sendMessage(ChatColor.RED + "Syntax error: " + ChatColor.WHITE + ChatColor.ITALIC + e.getMessage());
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Empty your hands first.");
                    }
                } else {
                    player.sendMessage("TODO STUB (This is a help, in case you didn't notice)");
                }
                return true;
            } else if (command.getName().equals("darkyenusbuild")) {
                player.sendMessage("TODO STUB (Darkyenus Build is curretly being created by Darkyen)");
                return true;
            } else if (command.getName().equals("tool")) {
                Tool inHand = getToolAssociatedWithItem(player.getItemInHand());
                if(inHand == null){
                    player.sendMessage(ChatColor.RED+"You are not holding any tool.");
                }else{
                    if (args.length == 0) {
                        inHand.sendInfo(player);
                    }else{
                        player.sendMessage("TODO STUB (Name change not yet implemented)");
                    }
                }
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Sorry, in game use only.");
            return true;
        }
        return false;
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
                            int changedBlocks = getChanges(event.getPlayer()).remove(nextIndex).changeBack();
                            event.getPlayer().sendMessage(ChatColor.GREEN.toString() + changedBlocks + " blocks changed back");
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("CallToThreadDumpStack")
    public Tool getToolAssociatedWithItem(ItemStack item) {
        if (item != null && item.getType() == Material.GOLD_PICKAXE) {
            if (item.hasItemMeta()) {
                if (OFFICIAL_TOOL_NAME.equals(item.getItemMeta().getDisplayName())) {
                    List<String> lore = item.getItemMeta().getLore();
                    if (lore.size() == 1) {
                        String loreText = lore.get(0);
                        if (loreText.startsWith(OFFICIAL_LORE_PREFIX)) {
                            String loreSuffix = loreText.substring(OFFICIAL_LORE_PREFIX.length());
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
