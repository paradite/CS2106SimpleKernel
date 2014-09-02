package zhuliang.nus.cs2105;

import java.util.List;

/**
 * Class for processes
 * Created by paradite on 2/9/14.
 */
public class Process implements Comparable<Process>{
    public String name;
    public int priority;
    public int PID;
    public List<Resource> resources_using;
    public List<Resource> resources_blocking;
    public final int STATUS_ERROR = -1;
    public final int STATUS_READY = 0;
    public final int STATUS_RUNNING = 1;
    public final int STATUS_BLOCKED = 2;
//    Status:
//     -1 - error
//      0 - ready
//      1 - running
//      2 - blocked
    public int status;
    public Process parent;
    public Process[] children;

    public Process(String name, int priority, int PID, Process parent) {
        this.name = name;
        this.priority = priority;
        this.PID = PID;
        this.parent = parent;
    }

    @Override
    public int compareTo(Process o) {
        if(this.priority > o.priority){
            return 1;
        }else if(this.priority < o.priority){
            return -1;
        }else{
            return 0;
        }
    }
}
