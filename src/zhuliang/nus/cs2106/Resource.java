package zhuliang.nus.cs2106;

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
    public LinkedList<Process> using_list;

    public Resource(int unit, String RID) {
        this.max_unit = unit;
        this.status = unit;
        this.RID = RID;
        this.waiting_list = new LinkedList<Process>();
        this.using_list = new LinkedList<Process>();
    }
}
