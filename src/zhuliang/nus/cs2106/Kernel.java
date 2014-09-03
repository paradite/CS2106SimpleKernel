package zhuliang.nus.cs2106;

import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

public class Kernel {
    static final String FILENAME = "testGR-sample";
    static final String IN       = FILENAME + ".txt";
    static final String OUT      = FILENAME + ".out";

//    Texts
    static final String TEXT_ERROR = "error";
    static final String TEXT_CRITICAL_ERROR = "critical error";
    static final String TEXT_INIT = "init";
    static final String TEXT_CREATE = "cr";
    static final String TEXT_DESTROY = "de";
    static final String TEXT_REQUEST = "req";
    static final String TEXT_RELEASE = "rel";
    static final String TEXT_TIMEOUT = "to";

    static Scanner sc;
    PrintStream out = System.out;
    static final char radix = ' ';
    private boolean init = false;

//    Signals:
    public final int SIGNAL_SUCCESS = 1;
    public final int SIGNAL_NOTFOUND = 0;

//    Status:
//     -1 - error
//      0 - ready
//      1 - running
//      2 - blocked
    public final int STATUS_ERROR = -1;
    public final int STATUS_READY = 0;
    public final int STATUS_RUNNING = 1;
    public final int STATUS_BLOCKED = 2;

//    Process related
    public int currentPID = 0;
    Process initProcess;
    public Process currentProcess;
    public ArrayList<LinkedList<Process>> readyList;
    public Process tempProcess;

//    Resources related
    private Resource[] resources;

    /**
     * Request resource with current process
     * @param resTag    The string representing the resource
     * @param unit      Number of units
     * @return          Status of the process after the request
     */
    public int requestResource(String resTag, int unit){
        Resource res = getResourceFromTag(resTag);
//        Check if Resource is valid
        if(res == null){
            return STATUS_ERROR;
        }
//        Check if unit requested is valid
        if(unit > res.max_unit){
//            Requesting for more than max, return error
            return STATUS_ERROR;
        }else if(unit <= res.status){
//            There are enough resource units
//            Update resouce units
            res.status = res.status - unit;
//            Add resource into the list of resources being used by the process
            currentProcess.resources_using.add(res);
//            Add process into the list of processes using the resource
            scheduler();
            return STATUS_RUNNING;
        }else if(unit > res.status){
//            Not enough resource units, block the process
            currentProcess.status = STATUS_BLOCKED;
            currentProcess.resources_blocking.add(res);
            removefromRL(currentProcess);
            res.waiting_list.add(currentProcess);
            scheduler();
            return STATUS_BLOCKED;
        }else{
//            This should not happen
            return STATUS_ERROR;
        }
    }

    /**
     * Add a process into the readyList
     * @param p Process to be added
     * @return  int representing the priority of the process added if successfully added, or an error
     */
    private int addtoRL(Process p){
        if(p == null || p.priority < 0 || p.priority > 2){
//            Invalid process
            return STATUS_ERROR;
        }else{
            int priority = p.priority;
            readyList.get(priority).add(p);
            return priority;
        }
    }

    /**
     * Remove a process from the readyList
     * @param p Process to be removed
     * @return  Signal of error or success
     */
    private int removefromRL(Process p){
        if(p == null || p.priority < 0 || p.priority > 2){
//            Invalid process
            return STATUS_ERROR;
        }else{
            int priority = p.priority;
            boolean success =  readyList.get(priority).remove(p);
            if(success){
                return SIGNAL_SUCCESS;
            }else{
                return SIGNAL_NOTFOUND;
            }
        }
    }

    /**
     * Remove a process from the waiting list of all resources
     * @param p Process to be removed
     * @return  Signal
     */
    private int removefromWaitingLists(Process p){
        if(p == null || p.priority < 0 || p.priority > 2){
//            Invalid process
            return STATUS_ERROR;
        }else{
            int priority = p.priority;
            boolean success = false;
//            Try removing the process from all waitling lists
            for (int i = 0; i < resources.length; i++) {
                success =  resources[i].waiting_list.remove(p);
                if(success){
                    break;
                }else{
                    success = false;
                }
            }
            if(success){
                return SIGNAL_SUCCESS;
            }else{
                return SIGNAL_NOTFOUND;
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
     * @return      Resource
     */
    private Resource getResourceFromTag(String tag){
        if(tag.length()< 2){
            return null;
        }
        String idString = tag.substring(1);
        int id = Integer.parseInt(idString);
        print_state("ID: " + id);
        return resources[id];
    }

    /**
     * Timeout function
     */
    private void timeOut(){
        removefromRL(currentProcess);
        tempProcess = currentProcess;
        tempProcess.status = STATUS_READY;
        addtoRL(tempProcess);
        scheduler();
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
            currentProcess = readyList.get(2).getFirst();
        }else if(!readyList.get(1).isEmpty()){
//            Exists a process with priority 1
            currentProcess = readyList.get(1).getFirst();
        }else if(!readyList.get(0).isEmpty()){
//            Exists a process with priority 0
            currentProcess = readyList.get(0).getFirst();
        }else{
//            This should not happen
            print_state(TEXT_CRITICAL_ERROR);
        }
//        Print out the running process
        if(currentProcess != null){
            print_state(currentProcess.name);
        }else{
            print_state(TEXT_ERROR);
        }
    }

    /**
     * Parse the instruction read and try to execute
     * @param inst  Instruction from input
     */
    private void execute(String inst) {
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
        if(inst.equals(TEXT_INIT)){
            init = false;
//            New line for new test case
            out.println();
            initialize();
        }else{
//            Get the different parts of instruction
            String[] inst_parts = inst.split(" ");
            String inst_real = inst_parts[0];
            if(inst_real.equals(TEXT_CREATE)){
//                Get the name and priority of the process
                String name = inst_parts[1];
                String stringPriority = inst_parts[2];
                int priority = Integer.parseInt(stringPriority);
                if(priority < 0 || priority > 2){
//                    Priority invalid, terminate execution
                    print_state(TEXT_ERROR);
                    return;
                }
                createProcess(name, priority);
            }else if(inst_real.equals(TEXT_DESTROY)){


            }else if(inst_real.equals(TEXT_REQUEST)){
                String name = inst_parts[1];
                int unit = Integer.parseInt(inst_parts[2]);
                int MAX_RESOURCE_UNIT = 4;
                if(unit < 0 || unit > MAX_RESOURCE_UNIT){
                    print_state(TEXT_ERROR);
                    return;
                }
                int result = requestResource(name, unit);
                if(result == STATUS_ERROR){
                    print_state(TEXT_ERROR);
                    return;
                }
            }else if(inst_real.equals(TEXT_RELEASE)){
//                TODO: Implement release
            }else if(inst_real.equals(TEXT_TIMEOUT)){
                timeOut();

            }else{
//                Invalid instruction, return error
                print_state(TEXT_ERROR);
                return;
            }
        }
//        Post-execution

    }

    /**
     * Method to create a new process for create instruction
     * @param name      name of the process
     * @param priority  priority of the process
     */
    private void createProcess(String name, int priority) {
        Process newProcess = new Process(name, priority, getPID(), currentProcess);
        addtoRL(newProcess);
        scheduler();
    }

    /**
     * Method to destroy a process by recursion
     * @param name  Name of the process
     * @return      Signal
     */
    private int destroyProcess(String name){
        return STATUS_ERROR;
    }

    /**
     * Initialize the kernel with init process and resources
     */
    private void initialize(){
//        Initialize the resources
        resources = new Resource[5];
        for (int i = 1; i <= 4; i++) {
            resources[i] = new Resource(i, "R" + i);
        }

//        Initialize the processes and ready list
        initProcess = new Process(TEXT_INIT, 0, getPID(), null);
        readyList = new ArrayList<LinkedList<Process>>();
        readyList.add(0, new LinkedList<Process>());
        readyList.add(1, new LinkedList<Process>());
        readyList.add(2, new LinkedList<Process>());
        addtoRL(initProcess);
        currentProcess = initProcess;

        init = true;
    }

    /**
     * Main function for the kernel
     * @throws Exception
     */
    private void run() throws Exception {
//        For output to file
//        out = new PrintStream(new FileOutputStream(OUT));
        String inst;
        while(sc.hasNextLine()) {
            inst = sc.nextLine();
            out.println("Instruction: " + inst);
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
        out.println(state);
//        out.print(state + " ");
    }

    /**
     * Main
     * @param args          Arguments
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        FileReader fr = new FileReader(IN);
        sc = new Scanner(fr);
        new Kernel().run();
    }

}
