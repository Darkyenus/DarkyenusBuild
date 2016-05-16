package darkyenus.plugin.build;

/**
 *
 * @author Darkyen
 */
public class ParsingUtils {
    public static boolean startsWith(String what, StringContainer boxForRest, String... oneOfThose) {
        for (String maybeThisOne : oneOfThose) {
            if (what.startsWith(maybeThisOne)) {
                boxForRest.string = what.substring(maybeThisOne.length());
                return true;
            }
        }
        return false;
    }
    
    public static boolean startsWith(String what, String... oneOfThose) {
        for (String maybeThisOne : oneOfThose) {
            if (what.startsWith(maybeThisOne)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean startsWith(String what,BooleanContainer booleanContainer, String... oneOfThose) {
        for (String maybeThisOne : oneOfThose) {
            if (what.startsWith(maybeThisOne)) {
                booleanContainer.booleanValue = true;
                return true;
            }
        }
        return false;
    }

    public static class BooleanContainer{
        public boolean booleanValue = false;
    }
    
    public static class StringContainer {

        public String string = null;
    }
    
    public static class SyntaxException extends Error {

        public SyntaxException(String reason) {
            super(reason);
        }
    }
}
