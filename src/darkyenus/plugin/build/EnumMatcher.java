package darkyenus.plugin.build;

import java.util.*;

/**
 *
 */
public final class EnumMatcher<E extends Enum<E>> {

    private final E[] VALUES;
    private final String[] NAMES;

    private EnumMatcher(E[] values) {
        VALUES = values;
        NAMES = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            NAMES[i] = simplify(values[i].toString());
        }
    }

    private static final Map<Class<?>, EnumMatcher<?>> MATCHER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> EnumMatcher<E> matcher(Class<E> type){
        final EnumMatcher<?> existing = MATCHER_CACHE.get(type);
        if(existing != null) return (EnumMatcher<E>) existing;

        assert type.isEnum();
        final EnumMatcher<E> newMatcher = new EnumMatcher<>(type.getEnumConstants());
        MATCHER_CACHE.put(type, newMatcher);
        return newMatcher;
    }

    public static <E extends Enum<E>> List<E> match(Class<E> type, String searched) {
        return matcher(type).match(searched);
    }

    public static <E extends Enum<E>> E matchOne(Class<E> type, String searched) {
        return matcher(type).matchOne(searched);
    }

    /**
     * Tries to match element from name
     * @param searched to match from
     * @return found elements in order of relevance, single item if perfect match, empty list if not found
     */
    @SuppressWarnings("unchecked")
    public List<E> match(String searched){
        searched = simplify(searched);
        if(searched.isEmpty()) return (List<E>)Collections.EMPTY_LIST;

        final int minimalDistance = 25;
        final List<FindResult> results = new ArrayList<>();

        final String[] names = NAMES;
        final E[] values = VALUES;

        for (int i = 0; i < names.length; i++) {
            final String nam = names[i];
            if(nam.equals(searched)){
                return Collections.singletonList(values[i]);
            }

            //Favor adding chars from removing or changing them
            final int relevance = computeLevenshteinDistance(searched, nam, 1, 4, 8);

            if(relevance < minimalDistance){
                results.add(new FindResult(i, relevance));
            }
        }

        if(results.isEmpty()){
            return (List<E>)Collections.EMPTY_LIST;
        } else {
            results.sort(Comparator.naturalOrder());
            final int maxResults = 5;
            final ArrayList<E> result = new ArrayList<>();
            for (int i = 0; i < Math.min(maxResults, results.size()); i++) {
                final FindResult findResult = results.get(i);
                result.add(values[findResult.data]);
            }
            return result;
        }
    }

    public E matchOne(String searched){
        final List<E> result = match(searched);
        if(result == null || result.isEmpty()){
            return null;
        }else{
            return result.get(0);
        }
    }

    private static String simplify(String string){
        StringBuilder fromBuilder = new StringBuilder();
        for(char character:string.toCharArray()){
            if(Character.isLetterOrDigit(character)){
                fromBuilder.append(Character.toLowerCase(character));
            }
        }
        return fromBuilder.toString();
    }


    /**
     * Computes Levenshtein distance of two strings.
     * (Minimum number of single character modifications needed to get second string from first)
     * Author: Meta @ vidasConcurrentes
     *
     * @param str1       first string
     * @param str2       second string
     * @param add        weight of add operation (1 is default, higher it is, more "expensive" this operation is)
     * @param delete     weight of delete operation
     * @param substitute weight of substitute operation
     * @return Levenshtein distance of two strings using given weights
     */
    private static int computeLevenshteinDistance(String str1, String str2, int add, int delete, int substitute) {
        return computeLevenshteinDistance(str1.toCharArray(), str2.toCharArray(), add, delete, substitute);
    }

    /*
     * Author: Meta @ vidasConcurrentes
     * Related to: http://vidasconcurrentes.blogspot.com/2011/06/distancia-de-levenshtein-distancia-de.html
     *
     * This is the class which implements the Weighted Levenshtein Distance
     * To do so, we take the base algorithm and make some modifications, as follows:
     *              ·       multiply first column for the Delete weight
     *              ·       multiply first row for the Insert weight
     *              ·       add the Delete weight when checking for the [i-1][j] value
     *              ·       add the Insert weight when checking for the [i][j-1] value
     *              ·       make the substitution cost the Substitution weight
     */
    private static int computeLevenshteinDistance(char[] str1, char[] str2, int insert, int delete, int substitute) {
        int[][] distance = new int[str1.length + 1][str2.length + 1];

        for (int i = 0; i <= str1.length; i++)
            distance[i][0] = i * delete;    // non-weighted algorithm doesn't take Delete weight into account
        for (int j = 0; j <= str2.length; j++)
            distance[0][j] = j * insert;    // non-weighted algorithm doesn't take Insert weight into account
        for (int i = 1; i <= str1.length; i++) {
            for (int j = 1; j <= str2.length; j++) {
                distance[i][j] = minimum(distance[i - 1][j] + delete,      // would be +1 instead of +delete
                        distance[i][j - 1] + insert,                                      // would be +1 instead of +insert
                        distance[i - 1][j - 1] + ((str1[i - 1] == str2[j - 1]) ? 0 : substitute));      // would be 1 instead of substitute
            }
        }
        return distance[str1.length][str2.length];
    }

    private static int minimum(int a, int b, int c) {
        if (a <= b && a <= c)
            return a;
        if (b <= a && b <= c)
            return b;
        return c;
    }

    private static final class FindResult implements Comparable<FindResult> {
        public final int data;
        public final int relevance;

        private FindResult(int data, int relevance) {
            this.data = data;
            this.relevance = relevance;
        }

        @Override
        public int compareTo(FindResult o) {
            return relevance - o.relevance;
        }
    }
}
