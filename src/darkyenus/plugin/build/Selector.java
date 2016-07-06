package darkyenus.plugin.build;

import darkyenus.plugin.build.ParsingUtils.SyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.function.Function;

/**
 *
 * @author Darkyen
 */
@SuppressWarnings("UnusedParameters")
public abstract class Selector {

    private final int[] offsets = new int[3];
    private FaceModifier faceModifier = FaceModifier.NONE;

    private void setOffsets(int[] offsets) {
        System.arraycopy(offsets, 0, this.offsets, 0, 3);
    }

    public void getBlocks(Tool.BlockAggregate blockAggregate, Player player, Location click, BlockFace blockFace) {
        getRawLocations(blockAggregate, player, click.getBlockX() + offsets[0], click.getBlockY() + offsets[1], click.getBlockZ() + offsets[2], faceModifier.modify(blockFace, player));
    }

    abstract void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace);

    public void sendInfo(CommandSender sender) {
        sendInfoRaw(sender);
        for (int offset : offsets) {
            if(offset != 0){
                sender.sendMessage(ChatColor.BLUE + "   Offsets: " + ChatColor.WHITE + Arrays.toString(offsets));
                break;
            }
        }

        if(faceModifier != FaceModifier.NONE){
            sender.sendMessage(ChatColor.BLUE + "   Face modifier: " + ChatColor.WHITE + faceModifier.toString().toLowerCase());
        }
    }

    abstract void sendInfoRaw(CommandSender sender);

    @SuppressWarnings("SpellCheckingInspection")
    public static Selector createSelector(CommandSender sender, Tokenizer tokenizer) throws SyntaxException {
        if(!tokenizer.hasNext()) {
            throw new SyntaxException("Selector is missing");
        }

        FaceModifier explicitModifier = null;
        FaceModifier implicitModifer = null;
        {
            final int mark = tokenizer.mark();
            final FaceModifier newModifier = FaceModifier.match(tokenizer.next());
            if(newModifier != null){
                explicitModifier = newModifier;
            }else{
                tokenizer.rollback(mark);
            }
        }

        final String selectorName = tokenizer.next().toLowerCase();
        final Function<int[], Selector> selectorFunction;

        switch (selectorName) {
            case "block":
            case "cube":
            case "box":
                selectorFunction = Selector::createCubeSelector;
                break;
            case "rect":
            case "rectangle":
                selectorFunction = Selector::createRectangleSelector;
                break;
            case "square":
                selectorFunction = Selector::createSquareSelector;
                break;
            case "sphere":
            case "ball":
            case "orb":
                selectorFunction = Selector::createSphereSelector;
                break;
            case "disk":
            case "disc":
                selectorFunction = Selector::createDiskSelector;
                break;
            case "h-disk":
            case "hdisk":
            case "h-disc":
            case "hdisc":
                selectorFunction = Selector::createDiskSelector;
                implicitModifer = FaceModifier.TOPS;
                break;
            case "column":
                selectorFunction = Selector::createColumnSelector;
                break;
            case "floor":
                selectorFunction = Selector::createColumnSelector;
                implicitModifer = FaceModifier.SIDES;
                break;
            case "wall":
            case "vall":
            case "vvall":
                selectorFunction = Selector::createColumnSelector;
                implicitModifer = FaceModifier.UP;
                break;
            case "chunk":
                selectorFunction = Selector::createChunkSelector;
                break;
            case "chunklayer":
                selectorFunction = Selector::createChunkLayerSelector;
                break;
            default:
                throw new SyntaxException("Unrecognized selector "+selectorName);
        }

        final int[] dimensions;
        final int[] offsets;

        {
            final String maybeDimensions = tokenizer.peek();
            dimensions = getDimensions(maybeDimensions);
        }

        if(dimensions.length != 0){
            tokenizer.next();//Take peeked dimensions

            String maybeOffsets = tokenizer.peek();
            if(maybeOffsets.startsWith("+")){
                tokenizer.next();//Take peeked offsets
                offsets = getDimensions(getDimensions(maybeOffsets.substring(1)), 3, 0);
            } else if(maybeOffsets.startsWith("-")){
                tokenizer.next();//Take peeked offsets
                offsets = getDimensions(getDimensions(maybeOffsets.substring(1)), 3, 0);
                if(offsets != null){
                    for (int i = 0; i < offsets.length; i++) {
                        offsets[i] = -offsets[i];
                    }
                }
            } else {
                offsets = null;
            }
        } else {
            offsets = null;
        }

        final Selector selector = selectorFunction.apply(dimensions);
        if(offsets != null){
            selector.setOffsets(offsets);
        }

        if(implicitModifer != null){
            if(explicitModifier != null){
                sender.sendMessage(ChatColor.RED+"Warning:"+ChatColor.RESET+" "+selectorName+" has implicit face modifier "+implicitModifer+" - overriden with "+explicitModifier);
            } else {
                selector.faceModifier = implicitModifer;
            }
        }

        if(explicitModifier != null){
            selector.faceModifier = explicitModifier;
        }

        return selector;
    }

    //<editor-fold defaultstate="collapsed" desc="Utility Methods">

    private static final int[] NO_DIMENSIONS = new int[0];

    private static int[] getDimensions(String text) {
        int dimensionCount = 1;
        for (int i = 0; i < text.length(); i++) {
            if(text.charAt(i) == 'x') dimensionCount++;
        }

        final int[] dimensions = new int[dimensionCount];
        int dimIndex = 0;
        boolean dimSizeChanged = false;
        boolean dimSizeNegative = false;
        int dimSize = 0;

        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if(c == '-'){
                if(!dimSizeChanged) {
                    dimSizeNegative = true;
                    dimSizeChanged = true;
                } else {
                    return NO_DIMENSIONS;
                }
            } else if(c >= '0' && c <= '9'){
                dimSize *= 10;
                dimSize += c - '0';
                dimSizeChanged = true;
            } else if(c == 'x') {
                if(dimSizeNegative){
                    dimSize = -dimSize;
                    dimSizeNegative = false;
                }
                dimensions[dimIndex] = dimSize;
                dimIndex++;
                dimSize = 0;
                dimSizeChanged = false;
            } else {
                return NO_DIMENSIONS;
            }
        }
        if(dimSizeNegative){
            dimSize = -dimSize;
        }
        dimensions[dimIndex] = dimSize;

        return dimensions;
    }


    private static int[] getDimensions(int[] raw, int dimensionCount, int defaultValue) {
        int[] finalDimensions = new int[dimensionCount];
        for (int i = 0; i < dimensionCount; i++) {
            if (i < raw.length) {
                finalDimensions[i] = raw[i];
            } else {
                finalDimensions[i] = defaultValue;
            }
        }
        return finalDimensions;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Selector Methods">
    public static Selector createDefaultSelector() {
        return createOneBlockSelector();
    }

    private static int sign(int of){
        if(of < 0)return -1;
        else return 1;
    }

    private static Selector createOneBlockSelector() {
        return new Selector() {
            @Override
            public void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                aggregate.add(clickX, clickY, clickZ);
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   One Block");
            }
        };
    }

    private static Selector createColumnSelector(int[] rawDimensions) {
        final int height = getDimensions(rawDimensions, 1, 4)[0];
        return new Selector() {
            @Override
            public void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                final int sign = sign(height);
                for (int i = 0; i < Math.abs(height); i++) {
                    aggregate.add(clickX + blockFace.getModX() * i * sign, clickY + blockFace.getModY() * i * sign, clickZ + blockFace.getModZ() * i * sign);
                }
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   Target-oriented column "+height+" long");
            }
        };
    }

    private static Selector createCubeSelector(int[] rawDimensions) {
        final int[] dimensions = getDimensions(rawDimensions, 3, 1);
        return new Selector() {
            @Override
            public void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                for (int xOff = -dimensions[0] / 2; xOff < dimensions[0] - (dimensions[0] / 2); xOff++) {
                    for (int yOff = -dimensions[1] / 2; yOff < dimensions[1] - (dimensions[1] / 2); yOff++) {
                        for (int zOff = -dimensions[2] / 2; zOff < dimensions[0] - (dimensions[2] / 2); zOff++) {
                            aggregate.add(clickX + xOff, clickY + yOff, clickZ + zOff);
                        }
                    }
                }
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   Cube " + dimensionsToString(dimensions));
            }
        };
    }

    private static Selector createRectangleSelector(int[] rawDimensions) {
        final int[] dimensions = getDimensions(rawDimensions, 2, 1);
        return new Selector() {
            @Override
            public void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                for (int aOff = -dimensions[0] / 2; aOff < dimensions[0] - (dimensions[0] / 2); aOff++) {
                    for (int bOff = -dimensions[1] / 2; bOff < dimensions[1] - (dimensions[1] / 2); bOff++) {
                        switch (blockFace) {
                            case UP:
                            case DOWN:
                                aggregate.add(clickX + aOff, clickY, clickZ + bOff);
                                break;
                            case SOUTH:
                            case NORTH:
                                aggregate.add(clickX, clickY + aOff, clickZ + bOff);
                                break;
                            case EAST:
                            case WEST:
                                aggregate.add(clickX + aOff, clickY + bOff, clickZ + bOff);
                                break;
                        }
                    }

                }
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   Target-oriented rectangle " + dimensionsToString(dimensions));
            }
        };
    }

    private static Selector createSquareSelector(int[] rawDimensions) {
        if(rawDimensions.length >= 1){
            return createRectangleSelector(new int[]{rawDimensions[0], rawDimensions[0]});
        } else {
            return createRectangleSelector(new int[]{1, 1});
        }
    }

    private static Selector createSphereSelector(int[] rawDimensions) {
        final int[] dimensions = getDimensions(rawDimensions, 1, 1);
        final int radius = dimensions[0];
        final int radius2 = radius * radius;

        return new Selector() {
            @Override
            public void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                for (int xOff = -radius; xOff <= radius; xOff++) {
                    for (int yOff = -radius; yOff <= radius; yOff++) {
                        for (int zOff = -radius; zOff <= radius; zOff++) {
                            if (xOff * xOff + yOff * yOff + zOff * zOff < radius2) {
                                aggregate.add(clickX + xOff, clickY + yOff, clickZ + zOff);
                            }
                        }
                    }
                }
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   Sphere with radius " + dimensionsToString(dimensions));
            }
        };
    }

    private static Selector createDiskSelector (int[] rawDimensions) {
        final int[] dimensions = getDimensions(rawDimensions, 1, 1);
        final int radius = dimensions[0];
        final int radius2 = radius * radius;

        return new Selector() {
            @Override
            public void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                for (int aOff = -radius; aOff <= radius; aOff++) {
                    for (int bOff = -radius; bOff <= radius; bOff++) {
                        if (aOff * aOff + bOff * bOff <= radius2) {
                            switch (blockFace) {
                                case UP:
                                case DOWN:
                                    aggregate.add(clickX + aOff, clickY, clickZ + bOff);
                                    break;
                                case SOUTH:
                                case NORTH:
                                    aggregate.add(clickX, clickY + aOff, clickZ + bOff);
                                    break;
                                case EAST:
                                case WEST:
                                    aggregate.add(clickX + aOff, clickY + bOff, clickZ);
                                    break;
                            }
                        }
                    }
                }
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   Target oriented disk with radius " + radius);
            }
        };
    }

    private static Selector createChunkSelector(int[] rawDimensions) {
        final int around = Math.min(getDimensions(rawDimensions, 1, 0)[0], 3);

        return new Selector() {
            @Override
            void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                final int chunkX = (clickX >> 4) << 4;
                final int chunkZ = (clickZ >> 4) << 4;

                final int regionX = chunkX - (around << 4);
                final int regionZ = chunkZ - (around << 4);

                final int regionSizeXZ = 16 + (around << 4) * 2;
                final int regionSizeY = player.getWorld().getMaxHeight();

                for (int x = 0; x < regionSizeXZ; x++) {
                    for (int y = 0; y < regionSizeY; y++) {
                        for (int z = 0; z < regionSizeXZ; z++) {
                            aggregate.add(regionX + x, y, regionZ + z);
                        }
                    }
                }
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   Chunk (and "+ around +" around)");
            }
        };
    }

    private static Selector createChunkLayerSelector(int[] rawDimensions) {
        final int[] dimensions = getDimensions(rawDimensions, 1, 1);

        final int yOff = dimensions[0] < 0 ? dimensions[0] : 0;
        final int height = dimensions[0] < 0 ? 0 : dimensions[0];

        return new Selector() {
            @Override
            void getRawLocations(Tool.BlockAggregate aggregate, Player player, int clickX, int clickY, int clickZ, BlockFace blockFace) {
                final Chunk chunk = player.getWorld().getChunkAt(clickX / 16, clickZ / 16);
                final int chunkX = chunk.getX() * 16;
                final int chunkZ = chunk.getZ() * 16;

                for (int y = clickY + yOff; y < clickY + height; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            aggregate.add(chunkX + x, y, chunkZ + z);
                        }
                    }
                }
            }

            @Override
            void sendInfoRaw(CommandSender sender) {
                sender.sendMessage(ChatColor.BLUE + "   ChunkLayer "+dimensions[0]+" blocks tall");
            }
        };
    }

    //</editor-fold>

    private static CharSequence dimensionsToString(int[] dimensions){
        if(dimensions == null || dimensions.length == 0) return "";
        final StringBuilder sb = new StringBuilder();
        sb.append(dimensions[0]);
        for (int i = 1; i < dimensions.length; i++) {
            sb.append('x').append(dimensions[i]);
        }
        return sb;
    }

    @SuppressWarnings("unused")
    public enum FaceModifier {
        NONE() {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                return clicked;
            }
        },
        TOPS("horizontal","h","flat","tops") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                if(clicked != BlockFace.DOWN && clicked != BlockFace.UP){
                    if (player.getLocation().getPitch() < 0) {
                        return BlockFace.DOWN;
                    } else {
                        return BlockFace.UP;
                    }
                }
                return clicked;
            }
        },
        SIDES("vertical","v","side","sides") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                if(clicked == BlockFace.DOWN || clicked == BlockFace.UP){
                    final int direction = (int) (((player.getLocation().getYaw() + 45) / 360f) * 4f) & 3;
                    switch (direction) {
                        case 0:
                            return BlockFace.NORTH;
                        case 1:
                            return BlockFace.EAST;
                        case 2:
                            return BlockFace.SOUTH;
                        case 3:
                            return BlockFace.WEST;
                        default:
                            System.err.println("Failed to transform yaw to direction "+player.getLocation().getYaw());
                    }
                }
                return clicked;
            }
        },
        UP("up","top") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                return BlockFace.UP;
            }
        },
        DOWN("down","bottom") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                return BlockFace.DOWN;
            }
        },
        EAST("east") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                return BlockFace.EAST;
            }
        },
        WEST("west") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                return BlockFace.WEST;
            }
        },
        NORTH("north") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                return BlockFace.NORTH;
            }
        },
        SOUTH("south") {
            @Override
            BlockFace modify(BlockFace clicked, Player player) {
                return BlockFace.SOUTH;
            }
        };

        private final String[] names;

        FaceModifier(String...names) {
            this.names = names;
        }

        private boolean hasName(String lowerCase){
            for (String name : names) {
                if(name.equals(lowerCase))return true;
            }
            return false;
        }

        private static final FaceModifier[] VALUES = values();

        public static FaceModifier match(String alias){
            final String lowerCase = alias.toLowerCase();
            for (FaceModifier value : VALUES) {
                if(value.hasName(lowerCase))return value;
            }
            return null;
        }

        abstract BlockFace modify(BlockFace clicked, Player player);
    }
}
