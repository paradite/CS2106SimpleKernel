package zhuliang.nus.cs2105;

import sun.awt.image.ImageWatched;

import java.util.LinkedList;

/**
 * Class for resources
 * Created by paradite on 2/9/14.
 */
public class Resource {
    public String RID;
//    Number of free units
    public int max_unit;
    public int status;
    public LinkedList<Process> waiting_list;

    public Resource(int unit, String RID) {
        this.max_unit = unit;
        this.status = unit;
        this.RID = RID;
    }
}
