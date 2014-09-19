package zhuliang.nus.cs2106;

import java.util.LinkedList;

/**
 * Class for processes
 * Created by paradite on 2/9/14.
 */
public class KernelProcess implements Comparable<KernelProcess>{
    public String name;
    public int priority;
    public int PID;
    public LinkedList<ResourceUnitPair> resources_using;
    public LinkedList<ResourceUnitPair> resources_blocking;
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
    public KernelProcess parent;
    public LinkedList<KernelProcess> childrenList;

    public KernelProcess(String name, int priority, int PID, KernelProcess parent) {
        this.name = name;
        this.priority = priority;
        this.PID = PID;
        this.parent = parent;
//        LinkedList of HashMaps to store the resource pointer and the units of resource being used
        this.resources_using = new LinkedList<ResourceUnitPair>();
        this.resources_blocking = new LinkedList<ResourceUnitPair>();
        this.childrenList = new LinkedList<KernelProcess>();
    }

    @Override
    public int compareTo(KernelProcess o) {
        if(this.priority > o.priority){
            return 1;
        }else if(this.priority < o.priority){
            return -1;
        }else{
            return 0;
        }
    }

    /**
     * Method to get and remove the resource unit pair blocking the process
     * @param res   {@link KernelResource} to check
     * @return  {@link ResourceUnitPair} or null
     */
    public ResourceUnitPair getAndRemoveBlockingPairFromRes(KernelResource res){
        ResourceUnitPair respair = null;
        for(ResourceUnitPair pair:this.resources_blocking){
            if(pair.getRes().equals(res)){
                respair = pair;
                this.resources_blocking.remove(pair);
                break;
            }
        }
        return respair;
    }

    /**
     * Method to get the resource unit pair being used by the process
     * @param res   {@link KernelResource} to check
     * @return  {@link ResourceUnitPair} or null
     */
    public ResourceUnitPair getUsingPairFromRes(KernelResource res){
        ResourceUnitPair respair = null;
        for(ResourceUnitPair pair:this.resources_using){
            if(pair.getRes().equals(res)){
                respair = pair;
                break;
            }
        }
        return respair;
    }

    public void removeFromParent(){
        this.parent.childrenList.remove(this);
    }

    /**
     * Check if process p is the children of this process
     * @param p another process to be checked
     * @return  true if it is a children of this process
     */
    public boolean checkIfIsChild(KernelProcess p) {
//        Return false if process does not exist
        if(p == null){
            return false;
        }
        KernelProcess children;
        if(this.childrenList != null && !this.childrenList.isEmpty()){
            children = this.childrenList.removeLast();
//            Return true if p is this process or p is child of this process or p is recursively child of this process
            return children.equals(p) || checkIfIsChild(p) || checkIfIsChild(children);
        }
        return false;
    }
}
