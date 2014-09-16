package zhuliang.nus.cs2106;

/**
 * Helper Class to encapsulate a pair of KernelResource and the number units being used
 * Created by paradite on 11/9/14.
 */
public class ResourceUnitPair {
    private KernelResource res;
    private Integer unit;

    public ResourceUnitPair(KernelResource res, Integer unit) {
        super();
        this.res = res;
        this.unit = unit;
    }

    public void setUnit(Integer unit) {
        this.unit = unit;
    }

    public KernelResource getRes() {
        return res;
    }

    public Integer getUnit() {
        return unit;
    }
}
