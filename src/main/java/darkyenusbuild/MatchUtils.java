package darkyenusbuild;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

/**
 *
 */
@SuppressWarnings({"deprecation", "unused"})
public class MatchUtils {

    /** @param s0 text user entered
     *  @param s1 text of result */
    public static int levenshteinDistance(CharSequence s0, CharSequence s1, int insertCost, int replaceCost, int deleteCost) {
        int len0 = s0.length() + 1;
        int len1 = s1.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newCost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++)
            cost[i] = i * deleteCost;

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int i1 = 1; i1 < len1; i1++) {
            // initial cost of skipping prefix in String s1
            newCost[0] = i1 * insertCost;

            // transformation cost for each letter in s0
            for (int i0 = 1; i0 < len0; i0++) {
                // matching current letters in both strings
                int match = (s0.charAt(i0 - 1) == s1.charAt(i1 - 1)) ? 0 : replaceCost;

                // computing cost for each transformation
                int cost_insert = cost[i0] + insertCost; //          \/
                int cost_replace = cost[i0 - 1] + match; //          _|
                int cost_delete = newCost[i0 - 1] + deleteCost; //   >

                // keep minimum cost
                newCost[i0] = min(cost_insert, cost_delete, cost_replace);
            }

            // swap cost/newCost arrays
            int[] swap = cost;
            cost = newCost;
            newCost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }

    private static int min (int a, int b, int c) {
        if (a <= b && a <= c) return a;
        if (b <= a && b <= c) return b;
        return c;
    }

    public static <T> MatchResult<T> match(T[] from, Function<T, CharSequence> toString, CharSequence target) {
        final int BAD_SCORE = 1000;

        //Insert only search
        final int[] scores = new int[from.length];
        int goodScores = 0;
        for (int i = 0; i < from.length; i++) {
            final T item = from[i];
            final int score = levenshteinDistance(target, toString.apply(item), 1, BAD_SCORE, BAD_SCORE);
            if(score == 0){
                //Perfect match
                return new MatchResult<>(item);
            } else {
                if(score < BAD_SCORE) {
                    goodScores++;
                }
                scores[i] = score;
            }
        }

        if(goodScores == 1){
            //Clear winner however bad it may be
            for (int i = 0; i < from.length; i++) {
                if(scores[i] < BAD_SCORE) {
                    return new MatchResult<>(from[i]);
                }
            }
            assert false;
        }

        boolean findCanBeUnambiguous = true;

        if (goodScores == 0) {
            //No good scores, search again with weights allowing other edits than adding
            findCanBeUnambiguous = false;
            for (int i = 0; i < from.length; i++) {
                scores[i] =  levenshteinDistance(target, toString.apply(from[i]), 1, 6, 3);
            }
        }

        int bestScore = BAD_SCORE;
        int bestScoreAmbiguityThreshold = BAD_SCORE;
        int bestScoreIndex = -1;
        boolean bestScoreIsUnambiguous = false;

        for (int i = 0; i < from.length; i++) {
            final int score = scores[i];
            if(score < bestScore) {
                bestScoreAmbiguityThreshold = bestScore + Math.max(bestScore / 3, 2);
                bestScoreIsUnambiguous = bestScore < bestScoreAmbiguityThreshold;
                bestScore = score;
                bestScoreIndex = i;
            } else if(score < bestScoreAmbiguityThreshold){
                bestScoreIsUnambiguous = false;
            }
        }

        if (bestScoreIsUnambiguous && findCanBeUnambiguous) {
            return new MatchResult<>(from[bestScoreIndex]);
        } else {
            final List<MatchResultItem> results = new ArrayList<>();
            for (int i = 0; i < from.length; i++) {
                final int score = scores[i];
                if (score < BAD_SCORE) {
                    results.add(new MatchResultItem(i, score));
                }
            }
            Collections.sort(results);

            final int resultItems = Math.min(8, results.size());
            //noinspection unchecked
            final T[] resultArray = (T[]) Array.newInstance(from.getClass().getComponentType(), resultItems);
            for (int i = 0; i < resultItems; i++) {
                resultArray[i] = from[results.get(i).index];
            }
            return new MatchResult<>(resultArray);
        }
    }

    public static <E extends Enum<E>> MatchResult<E> match(Class<E> type, String searched) {
        final EnumMatcher<E> matcher = matcher(type);
        try {
            return new MatchResult<>(matcher.VALUES[Integer.parseInt(searched)]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
        }
        return matcher.match(searched);
    }

    public static MatchResult<OfflinePlayer> matchPlayer(String from) {
        return MatchUtils.match(Bukkit.getOfflinePlayers(), offPlayer -> offPlayer.getName().toLowerCase(), from.toLowerCase());
    }

    public static void matchPlayerFail(MatchResult<OfflinePlayer> result, CommandSender sender){
        if(sender == null)return;
        if(result.results.length == 0){
            sender.sendMessage(ChatColor.RED+"Player not found");
        } else if(result.results.length == 1){
            sender.sendMessage(ChatColor.RED+"Player not found. "+ChatColor.DARK_RED+"Did you mean: " + ChatColor.RESET + result.results[0].getName() + ChatColor.DARK_RED + " ?");
        } else {
            //Will be concatenated at compile time
            final StringBuilder sb = new StringBuilder(ChatColor.RED+"Player not found. "+ChatColor.DARK_RED+"Did you mean: " + ChatColor.RESET);
            for (int i = 0; i < result.results.length; i++) {
                if(i != 0) {
                    sb.append(ChatColor.DARK_RED);
                    sb.append(i + 1 == result.results.length ? " or " : ", ");
                    sb.append(ChatColor.RESET);
                }
                final OfflinePlayer player = result.results[i];
                if(player.isOnline()) {
                    sb.append(ChatColor.DARK_GREEN);
                }
                sb.append(player.getName());
            }
            //noinspection StringConcatenationInsideStringBufferAppend
            sb.append(ChatColor.DARK_RED + "?");
            sender.sendMessage(sb.toString());
        }
    }

    public static <T> void matchFail(String noun, MatchResult<T> result, CommandSender sender){
        if(sender == null)return;
        if(result.results.length == 0){
            sender.sendMessage(ChatColor.RED+noun+" not found");
        } else if(result.results.length == 1){
            sender.sendMessage(ChatColor.RED+noun+" not found "+ChatColor.DARK_RED+"Did you mean: " + ChatColor.RESET + result.results[0] + ChatColor.DARK_RED + " ?");
        } else {
            final StringBuilder sb = new StringBuilder(ChatColor.RED+noun+" not found "+ChatColor.DARK_RED+"Did you mean: " + ChatColor.RESET);
            for (int i = 0; i < result.results.length; i++) {
                if(i != 0) {
                    sb.append(ChatColor.DARK_RED);
                    sb.append(i + 1 == result.results.length ? " or " : ", ");
                    sb.append(ChatColor.RESET);
                }
                sb.append(result.results[i]);
            }
            //noinspection StringConcatenationInsideStringBufferAppend
            sb.append(ChatColor.DARK_RED + "?");
            sender.sendMessage(sb.toString());
        }
    }

    public static OfflinePlayer findPlayer(String name, CommandSender sender) {
        final MatchResult<OfflinePlayer> result = matchPlayer(name);
        if(result.isDefinite) {
            return result.result();
        } else {
            matchPlayerFail(result, sender);
            return null;
        }
    }

    public static Player findOnlinePlayer(String name, CommandSender sender) {
        final MatchResult<OfflinePlayer> result = matchPlayer(name);
        if(result.isDefinite) {
            final OfflinePlayer offlinePlayer = result.result();
            final Player player = offlinePlayer.getPlayer();
            if(player == null) {
                if(sender != null) {
                    sender.sendMessage(ChatColor.RED.toString() + offlinePlayer.getName() + " is offline");
                }
                return null;
            } else {
                return player;
            }
        } else {
            matchPlayerFail(result, sender);
            return null;
        }
    }

    public static MaterialSpec matchMaterialData(String name, CommandSender sender) {
        final int dataSplit = name.indexOf(':');
        final String material;
        final String data;
        if (dataSplit == -1){
            material = name;
            data = null;
        }else {
            material = name.substring(0, dataSplit);
            final String dataRaw = name.substring(dataSplit + 1);
            if(dataRaw.isEmpty() || "*".equals(dataRaw) || "any".equalsIgnoreCase(dataRaw)) {
                data = null;
            } else {
                data = dataRaw;
            }
        }

        final MatchResult<Material> materialMatch = match(Material.class, material);
        if(!materialMatch.isDefinite) {
            matchFail("Material", materialMatch, sender);
            return null;
        }

        //We have material, now data

        final Material mat = materialMatch.result();

        if(data == null) {
            //No data, just material!
            return new MaterialSpec(mat);
        }

        try {
            final int dataNumber = Integer.parseInt(data);
            if((dataNumber < 0 || dataNumber >= 16) && sender != null) {
                sender.sendMessage(ChatColor.GRAY+"Hint: Data value is never negative and never 16 or more");
            }
            return new MaterialSpec(mat, (byte)dataNumber);
        } catch (NumberFormatException ignored) {
        }

        //Data is not number, try aliases

        //Maybe color?
        {
            final MatchResult<DyeColor> colorMatch = match(DyeColor.class, data);
            if (colorMatch.isDefinite) {
                if(mat == Material.INK_SACK) {
                    return new MaterialSpec(mat, colorMatch.result().getDyeData());
                } else {
                    return new MaterialSpec(mat, colorMatch.result().getWoolData());
                }
            }
        }

        //Maybe wood type?
        {
            final MatchResult<TreeSpecies> treeMatch = match(TreeSpecies.class, data);
            if (treeMatch.isDefinite) {
                if(mat == Material.LOG_2 || mat == Material.LEAVES_2) {
                    return new MaterialSpec(mat, (byte) (treeMatch.result().getData() - 4));
                } else {
                    return new MaterialSpec(mat, treeMatch.result().getData());
                }
            }
        }

        //Maybe stone type?
        {
            final MatchResult<StoneType> stoneMatch = match(StoneType.class, data);
            if (stoneMatch.isDefinite) {
                return new MaterialSpec(mat, stoneMatch.result().data);
            }
        }

        //Maybe special value: inherit?
        {
            if(data.equals("?")) {
                return new MaterialSpec(mat, MaterialSpec.DATA_INHERIT);
            }
        }

        if(sender != null) {
            sender.sendMessage(ChatColor.RED+"Failed to match data, try some number, color, wood or stone type");
        }
        return null;
    }

    @SuppressWarnings("unused")//Used by reflection
    private enum StoneType {
        STONE(0),
        GRANITE(1),
        POLISHED_GRANITE(2),
        DIORITE(3),
        POLISHED_DIORITE(4),
        ANDESITE(5),
        POLISHED_ANDESITE(6)
        ;
        public final byte data;

        StoneType(int data) {
            this.data = (byte)data;
        }
    }

    private static final class MatchResultItem implements Comparable<MatchResultItem> {
        public final int index;
        public final int score;

        private MatchResultItem (int index, int score) {
            this.index = index;
            this.score = score;
        }

        @Override
        public int compareTo (MatchResultItem o) {
            return score - o.score;
        }
    }

    private static final Map<Class<?>, EnumMatcher<?>> MATCHER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> EnumMatcher<E> matcher (Class<E> type) {
        final EnumMatcher<?> existing = MATCHER_CACHE.get(type);
        if (existing != null) return (EnumMatcher<E>)existing;

        assert type.isEnum();
        final EnumMatcher<E> newMatcher = new EnumMatcher<>(type.getEnumConstants());
        MATCHER_CACHE.put(type, newMatcher);
        return newMatcher;
    }

    private static final class EnumMatcher<E extends Enum<E>> implements Function<E, CharSequence> {

        private final E[] VALUES;
        private final String[] NAMES;

        private EnumMatcher (E[] values) {
            VALUES = values;
            NAMES = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                NAMES[i] = simplify(values[i].toString());
            }
        }

        private String simplify (String string) {
            StringBuilder fromBuilder = new StringBuilder();
            for (char character : string.toCharArray()) {
                if (Character.isLetterOrDigit(character)) {
                    fromBuilder.append(Character.toLowerCase(character));
                }
            }
            return fromBuilder.toString();
        }

        @Override
        public String apply(E e) {
            return NAMES[e.ordinal()];
        }

        public MatchResult<E> match(String searched) {
            return MatchUtils.match(VALUES, this, simplify(searched));
        }
    }

    public static final class MatchResult<T> {
        public final boolean isDefinite;
        public final T[] results;

        public MatchResult(T[] results) {
            this.isDefinite = false;
            this.results = results;
        }

        public MatchResult(T result) {
            this.isDefinite = true;
            //noinspection unchecked
            this.results = (T[])Array.newInstance(result.getClass(), 1);
            this.results[0] = result;
        }

        public T result() {
            return results[0];
        }
    }

    public static class MaterialSpec {
        public final Material material;
        public final int data;
        public final boolean hasData;

        public MaterialSpec(Material material, int data) {
            this.material = material;
            this.data = data;
            this.hasData = data != DATA_INHERIT;
        }

        public MaterialSpec(Material material) {
            this.material = material;
            this.data = 0;
            this.hasData = false;
        }

        public ItemStack toItemStack(int amount) {
            if(hasData) {
                return new ItemStack(material, amount, (short)data);
            } else {
                return new ItemStack(material, amount);
            }
        }

        @Override
        public String toString() {
            if(hasData) {
                return material.toString()+":"+data;
            } else {
                return material.toString();
            }
        }

        public static final int DATA_INHERIT = Integer.MAX_VALUE;
    }

    @SuppressWarnings("unused")
    public static final class StringBuilderCommandSender implements CommandSender {

        public final StringBuilder sb = new StringBuilder();

        @Override
        public void sendMessage(String message) {
            sb.append(message).append('\n');
        }

        @Override
        public void sendMessage(String[] messages) {
            for (String message : messages) {
                sb.append(message).append('\n');
            }
        }

        @Override
        public Server getServer() {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public boolean isPermissionSet(String name) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public boolean isPermissionSet(Permission perm) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public boolean hasPermission(String name) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public boolean hasPermission(Permission perm) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public void removeAttachment(PermissionAttachment attachment) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public void recalculatePermissions() {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public Set<PermissionAttachmentInfo> getEffectivePermissions() {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public boolean isOp() {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }

        @Override
        public void setOp(boolean value) {
            throw new UnsupportedOperationException("StringBuilderCommandSender");
        }
    }
}