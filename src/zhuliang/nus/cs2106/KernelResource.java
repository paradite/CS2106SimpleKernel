package zhuliang.nus.cs2106;

import java.util.LinkedList;

/**
 * Class for resources
 * Created by paradite on 2/9/14.
 */
public class KernelResource {
    public String RID;
//    Number of free units
    public int max_unit;
    public int status;
    public LinkedList<KernelProcess> blocked_list;
    public LinkedList<KernelProcess> using_list;

    public KernelResource(int unit, String RID) {
        this.max_unit = unit;
        this.status = unit;
        this.RID = RID;
        this.blocked_list = new LinkedList<KernelProcess>();
        this.using_list = new LinkedList<KernelProcess>();
    }

    public KernelProcess getAndRemoveBlocked(){
        if(this.blocked_list == null || this.blocked_list.isEmpty()){
            return null;
        }else{
            return this.blocked_list.removeFirst();
        }
    }
}
