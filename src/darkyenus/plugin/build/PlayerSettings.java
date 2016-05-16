package darkyenus.plugin.build;

/**
 *
 * @author Darkyen
 */
public class PlayerSettings {
    
    private int range = 0;
    private boolean airBrush = false;
    private boolean verbose = true;

    /**
     * @return the range
     */
    public int getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(int range) {
        this.range = range;
    }

    /**
     * @return the airBrush
     */
    public boolean isAirBrush() {
        return airBrush;
    }

    /**
     * @param airBrush the airBrush to set
     */
    public void setAirBrush(boolean airBrush) {
        this.airBrush = airBrush;
    }

    /**
     * @return the verbose
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
}
