package zhuliang.nus.cs2106;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.*;

public class Kernel {

    final Boolean DEBUG = false;

    static Scanner sc;
    PrintStream out = System.out;
    static final char radix = ' ';
    static private boolean init = false;

    //    KernelProcess related
//    PID number
    private int currentPID = 0;
    //    HashMap Index for all the processes
    private HashMap<String, KernelProcess> currentProcesses;
    KernelProcess initKernelProcess;
    public KernelProcess currentKernelProcess;
    public ArrayList<LinkedList<KernelProcess>> readyList;
    public KernelProcess tempKernelProcess;

    //    Resources related
    private KernelResource[] kernelResources;
    private final int MAX_RESOURCE_UNIT = 4;

    /**
     * Request resource with current process and call the method to do the allocation
     * @param resTag    The string representing the resource
     * @param unit      Number of units
     * @return          Status of the process after the request
     */
    public int requestResource(String resTag, int unit){
        KernelResource res = getResourceFromTag(resTag);
        KernelProcess processRequesting = currentKernelProcess;
//        Try to allocate resource to the process
        return allocateResource(unit, res, processRequesting);
    }

    /**
     * Method to try allocate the resource to the process
     * @param unit  number of units requesting
     * @param res   Resource being requested
     * @param processRequesting Process requesting
     * @return int Signal
     */
    private int allocateResource(int unit, KernelResource res, KernelProcess processRequesting) {
        //        Check if KernelResource is valid
        if(res == null){
            return Utils.STATUS_ERROR;
        }
//        Check if unit requested is valid
        if(unit > res.max_unit){
//            Requesting for more than max, return error
            return Utils.STATUS_ERROR;
        }else if(unit <= res.status){
//            There are enough resource units
//            Update resouce units
            res.status = res.status - unit;
//            Check if the process is already using the resource
            ResourceUnitPair respair = processRequesting.getUsingPairFromRes(res);
            if(respair != null){
                int unit_using = respair.getUnit();
                respair.setUnit(unit_using + unit);
            }else{
//                Add resource and number of units required into the list of kernelResources being used by the process
                ResourceUnitPair resPair = new ResourceUnitPair(res, unit);
                processRequesting.resources_using.add(resPair);
//                Add process into the list of processes using the resource
                res.using_list.add(processRequesting);
            }
            processRequesting.status = Utils.STATUS_READY;
            addtoRL(processRequesting);
            return Utils.STATUS_RUNNING;
        }else if(unit > res.status){
//            Not enough resource units, block the process
            processRequesting.status = Utils.STATUS_BLOCKED;
            ResourceUnitPair resPair = new ResourceUnitPair(res, unit);
            processRequesting.resources_blocking.add(resPair);
            removefromRL(processRequesting);
            res.blocked_list.add(processRequesting);
            return Utils.STATUS_BLOCKED;
        }else{
//            This should not happen
            return Utils.STATUS_ERROR;
        }
    }

    /**
     * Release resource from current process
     * @param resTag    The string representing the resource
     * @param unit      Number of units
     * @return          Status of the process after the request
     */
    public int releaseResource(String resTag, int unit){
        KernelResource res = getResourceFromTag(resTag);
//        Check if KernelResource is valid
        if(res == null){
            return Utils.STATUS_ERROR;
        }
//        Check if process is using the resource
        ResourceUnitPair respair = currentKernelProcess.getUsingPairFromRes(res);
        if(respair == null){
            return Utils.STATUS_ERROR;
        }

//        Check if unit released is valid
        if(unit > res.max_unit || unit > respair.getUnit()){
//            Releasing for more than max or what the process is using, return error
            return Utils.STATUS_ERROR;
        }else if(unit <= respair.getUnit()){
            int unit_using = respair.getUnit();
            respair.setUnit(unit_using - unit);
//            Release the resource and allocate to blocked processes if unit using is 0
            if(unit_using == unit){
                boolean result = currentKernelProcess.resources_using.remove(respair);
                if(!result){
                    return Utils.STATUS_ERROR;
                }
                res.using_list.remove(currentKernelProcess);
//                Get and remove the process that is being blocked
                KernelProcess p = res.getAndRemoveBlocked();
                if(p != null){
//                    Reallocate the resource to the process
                    ResourceUnitPair verified_respair = p.getAndRemoveBlockingPairFromRes(res);
                    if(verified_respair != null){
                        allocateResource(verified_respair.getUnit(), verified_respair.getRes(), p);
                    }else {
                        return Utils.STATUS_ERROR;
                    }

                }
            }
            return Utils.STATUS_RUNNING;
        }else{
//            This should not happen
            return Utils.STATUS_ERROR;
        }
    }

    /**
     * Add a process into the readyList if not inside
     * @param p KernelProcess to be added
     * @return  int representing the priority of the process added if successfully added, or an error
     */
    private int addtoRL(KernelProcess p){
        if(p == null || p.priority < 0 || p.priority > 2){
//            Invalid process
            return Utils.STATUS_ERROR;
        }else{
            int priority = p.priority;
            if(!readyList.get(priority).contains(p)){
                readyList.get(priority).add(p);
            }else{
                return Utils.SIGNAL_ALREADYEXIST;
            }
            return priority;
        }
    }

    /**
     * Remove a process from the readyList
     * @param p KernelProcess to be removed
     * @return  Signal of error or success
     */
    private int removefromRL(KernelProcess p){
        if(p == null || p.priority < 0 || p.priority > 2){
//            Invalid process
            return Utils.STATUS_ERROR;
        }else{
            int priority = p.priority;
            boolean success =  readyList.get(priority).remove(p);
            if(success){
                return Utils.SIGNAL_SUCCESS;
            }else{
                return Utils.SIGNAL_NOTFOUND;
            }
        }
    }

    /**
     * Remove a process from the waiting list of all kernelResources
     * @param p KernelProcess to be removed
     * @return  Signal
     */
    private int removefromWaitingLists(KernelProcess p){
        if(p == null || p.priority < 0 || p.priority > 2){
//            Invalid process
            return Utils.STATUS_ERROR;
        }else{
            int priority = p.priority;
            boolean success = false;
//            Try removing the process from all waitling lists
            for (int i = 0; i < kernelResources.length; i++) {
                success =  kernelResources[i].blocked_list.remove(p);
                if(success){
                    break;
                }else{
                    success = false;
                }
            }
            if(success){
                return Utils.SIGNAL_SUCCESS;
            }else{
                return Utils.SIGNAL_NOTFOUND;
            }
        }
    }

    /**
     * Get a new PID in running number
     * @return  integer
     */
    private int getPID(){
        currentPID++;
        return currentPID;
    }

    /**
     * Get the corresponding resource from the resource tag string
     * @param tag   String for resource tag, eg. "R1"
     * @return      KernelResource
     */
    private KernelResource getResourceFromTag(String tag){
        if(tag.length()< 2){
            return null;
        }
        String idString = tag.substring(1);
        int id = Integer.parseInt(idString);
        return kernelResources[id];
    }

    /**
     * Timeout function
     */
    private void timeOut(){
        removefromRL(currentKernelProcess);
        tempKernelProcess = currentKernelProcess;
        tempKernelProcess.status = Utils.STATUS_READY;
        addtoRL(tempKernelProcess);
    }

    /**
     * Scheduler function
     * Called after each instruction is successfully executed
     */
    private void scheduler() {
//        Set the running process
//        There should be at least one process in readyList
        assert ( !(readyList.get(0).isEmpty() && readyList.get(1).isEmpty() && readyList.get(2).isEmpty()) );
//        Choose the process with highest priority following FIFO
        if(!readyList.get(2).isEmpty()){
//            Exists a process with priority 2
            currentKernelProcess = readyList.get(2).getFirst();
        }else if(!readyList.get(1).isEmpty()){
//            Exists a process with priority 1
            currentKernelProcess = readyList.get(1).getFirst();
        }else if(!readyList.get(0).isEmpty()){
//            Exists a process with priority 0
            currentKernelProcess = readyList.get(0).getFirst();
        }else{
//            This should not happen
            print_state(Utils.TEXT_CRITICAL_ERROR);
        }
//        Print out the running process
        if(currentKernelProcess != null){
            print_state(currentKernelProcess.name);
        }else{
            print_state(Utils.TEXT_ERROR);
        }
    }

    /**
     * Parse the instruction read and try to execute
     * @param inst  Instruction from input
     */
    private void execute(String inst) {
        int result = Utils.SIGNAL_SUCCESS;
//        Initialize if not initialized
        if(!init){
            initialize();
        }
//        Read and Execute instructions
//        Ignore if empty
        if(inst.isEmpty()){
            return;
        }
//        Reinitialize if instruction is initialize
        if(inst.equals(Utils.TEXT_INIT)){
            init = false;
//            New line for new test case
            out.println();
            initialize();
        }else{
//            Get the different parts of instruction
            String[] inst_parts = inst.split(" ");
            String inst_real = inst_parts[0];
            if(inst_real.equals(Utils.TEXT_CREATE)){
//                Get the name and priority of the process
                String name = inst_parts[1];
                String stringPriority = inst_parts[2];
                int priority = Integer.parseInt(stringPriority);
                if(priority < 0 || priority > 2){
//                    Priority invalid, terminate execution
                    result = Utils.STATUS_ERROR;
                }else {
                    createProcess(name, priority);
                }
            }else if(inst_real.equals(Utils.TEXT_DESTROY)){
//                Get the name of the process to be destroyed
                String name = inst_parts[1];
                result = destroyProcess(name);
            }else if(inst_real.equals(Utils.TEXT_REQUEST)){
                String name = inst_parts[1];
                int unit = Integer.parseInt(inst_parts[2]);
                if(unit < 0 || unit > MAX_RESOURCE_UNIT){
                    result = Utils.STATUS_ERROR;
                }else{
                    result = requestResource(name, unit);
                }
            }else if(inst_real.equals(Utils.TEXT_RELEASE)){
                String name = inst_parts[1];
                int unit = Integer.parseInt(inst_parts[2]);
                result = releaseResource(name, unit);
            }else if(inst_real.equals(Utils.TEXT_TIMEOUT)){
                timeOut();
            }else{
//                Invalid instruction, return error
                print_state(Utils.TEXT_ERROR);
            }
            //        Post-execution
            if(result == Utils.STATUS_ERROR){
                print_state(Utils.TEXT_ERROR);
            }else {
                scheduler();
            }
        }

    }

    /**
     * Method to create a new process for create instruction
     * @param name      name of the process
     * @param priority  priority of the process
     */
    private KernelProcess createProcess(String name, int priority) {
        int current_PID = getPID();
        KernelProcess newKernelProcess = new KernelProcess(name, priority, current_PID, currentKernelProcess);
//        Update the childrenList field of the parent
        if(!name.equals(Utils.TEXT_INIT)){
            currentKernelProcess.childrenList.add(newKernelProcess);
        }
//        Add the process to the index
        currentProcesses.put(name, newKernelProcess);
//        Add the process to ready list
        addtoRL(newKernelProcess);
        return newKernelProcess;
    }

    /**
     * Wrapper method to destroy a process by recursion
     * @param name  Name of the process
     * @return      Signalf
     */
    private int destroyProcess(String name){
//        Query for the process in the index
        KernelProcess p = getKernelProcess(name);
        if(p == null){
            return Utils.STATUS_ERROR;
        }
        killProcessTree(p);
        return Utils.SIGNAL_SUCCESS;
    }

    /**
     * Actual method that kills the process recursively
     * @param p KernelProcess to be killed
     */
    private void killProcessTree(KernelProcess p){
        KernelProcess children;
//        Recursively call killProcessTree for all the childrenList
        while(p.childrenList != null && !p.childrenList.isEmpty()){
            children = p.childrenList.removeLast();
            killProcessTree(children);
        }
//        Remove from RL
        removefromRL(p);

//        Free resources
        freeResources(p);
        p.removeFromParent();
        p.parent = null;

    }

    /**
     * Free the resources used by the process or blocking the process, update the pointers in both processes and resources
     * @param p KernelProcess to free resources from
     */
    private int freeResources(KernelProcess p){
//        Remove pointer of the first resource being used from process
        ResourceUnitPair resUsingPair;
        KernelResource resUsing;
        int resUsingUnit;
        while(!p.resources_using.isEmpty()){
            resUsingPair = p.resources_using.removeFirst();
            resUsing = resUsingPair.getRes();
            resUsingUnit = resUsingPair.getUnit();
//            Remove process pointer from resource's using list
            resUsing.using_list.remove(p);
//            Update the status of the resources
            resUsing.status = resUsing.status + resUsingUnit;
//            Allocate the resource to processes on the waiting list, if any
            if(!resUsing.blocked_list.isEmpty()){
                //                Get and remove the process that is being blocked
                KernelProcess pWaiting = resUsing.getAndRemoveBlocked();
                if(pWaiting != null){
//                    Reallocate the resource to the process
                    ResourceUnitPair verified_respair = pWaiting.getAndRemoveBlockingPairFromRes(resUsing);
                    if(verified_respair != null){
                        allocateResource(verified_respair.getUnit(), verified_respair.getRes(), pWaiting);
                    }else {
                        return Utils.STATUS_ERROR;
                    }
                }
            }
        }
//        Remove pointers of all resources blocking the process
        ResourceUnitPair resBlocking;
        while(!p.resources_blocking.isEmpty()){
            resBlocking = p.resources_blocking.removeFirst();
//            Remove process pointer from resource's waiting list
            resBlocking.getRes().blocked_list.remove(p);
        }
        return Utils.SIGNAL_SUCCESS;
    }

    /**
     * Initialize the kernel with init process and kernelResources
     */
    private void initialize(){
//        Initialize the kernelResources
        kernelResources = new KernelResource[5];
        for (int i = 1; i <= 4; i++) {
            kernelResources[i] = new KernelResource(i, "R" + i);
        }

//        Initialize the processes and ready list
        currentProcesses = new HashMap<String, KernelProcess>();
        readyList = new ArrayList<LinkedList<KernelProcess>>();
        readyList.add(0, new LinkedList<KernelProcess>());
        readyList.add(1, new LinkedList<KernelProcess>());
        readyList.add(2, new LinkedList<KernelProcess>());
        initKernelProcess = createProcess(Utils.TEXT_INIT, 0);
        currentKernelProcess = initKernelProcess;
        init = true;
        print_state(Utils.TEXT_INIT);
    }
    /**
     * Main function for the kernel
     * @throws Exception
     */
    private void run() throws Exception {
        if(!DEBUG){
            out = new PrintStream(new FileOutputStream(Utils.OUT));
        }
//        For output to file
        String inst;
        while(sc.hasNextLine()) {
            inst = sc.nextLine();
            if(DEBUG){
                out.println("Instruction: " + inst);
            }
            execute(inst);
        }
        sc.close();
        out.close();
    }

    /**
     * Print the current state
     * @param state String to be printed
     */
    private void print_state(String state){
        if(DEBUG){
            out.println(state);
        }else if(state.equals(Utils.TEXT_ERROR)){
            out.print(state);
        }else{
            out.print(state + " ");
        }
    }

    private KernelProcess getKernelProcess(String name) {
        return currentProcesses.get(name);
    }

    /**
     * Main
     * @param args          Arguments
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        FileReader fr = new FileReader(Utils.IN);
        sc = new Scanner(fr);
        new Kernel().run();
    }
}
