package zhuliang.nus.cs2105;

import java.io.FileReader;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;

public class Kernel {
    static final String FILENAME = "testGR-sample";
    static final String IN       = FILENAME + ".txt";
    static final String OUT      = FILENAME + ".out";

//    Texts
    static final String TEXT_ERROR = "error";
    static final String TEXT_CREATE = "cr";
    static final String TEXT_TIMEOUT = "to";
    static final String TEXT_REQUEST = "req";

    static Scanner sc;
    PrintStream out = System.out;
    static final char radix = ' ';
    private boolean init = false;

//    Status related:
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
    public LinkedList<Process> RL;

//    Resources related
    private Resource[] resources;


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
            return STATUS_RUNNING;
        }else if(unit > res.status){
//            Not enough resource units, block the process
            currentProcess.status = STATUS_BLOCKED;
            currentProcess.resources_blocking.add(res);
            RL.remove(currentProcess);
            res.waiting_list.add(currentProcess);
            return STATUS_BLOCKED;
        }else{
//            This should not happen
            return STATUS_ERROR;
        }
    }

    private void initialize(){
//        Initialize the resources
        resources = new Resource[5];
        for (int i = 1; i <= 4; i++) {
            resources[i] = new Resource(i, "R" + i);
        }

//        Initialize the process
        initProcess = new Process("init", 0, getPID(), null);
        RL = new LinkedList<Process>();
        RL.add(initProcess);
        currentProcess = initProcess;

        init = true;
    }

    private int getPID(){
        currentPID++;
        return currentPID;
    }

    private Resource getResourceFromTag(String tag){
        if(tag.length()< 2){
            return null;
        }
        String idString = tag.substring(1);
        int id = Integer.parseInt(idString);
        print_state("ID: " + id);
        return resources[id];
    }

    private void execute(String inst) {
//        Initialize if not initialized
        if(!init){
            initialize();
        }

//        Read and Execute instructions
//        Reinitialize if instruction is initialize
        if(inst.equals("init")){
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
                Process newProcess = new Process(name,priority,getPID(),currentProcess);
                RL.add(newProcess);
            }
        }
//        Post-execution
        Scheduler();
        if(currentProcess != null){
            print_state(currentProcess.name);
        }else{
            print_state(TEXT_ERROR);
        }

    }

    private void Scheduler() {
//        Sort the ready list by priority
        Collections.sort(RL);
//        Set the running process
//        There should be at least one process in RL
        assert (!RL.isEmpty());
//        Only change running process if it is a higher priority process
        if(RL.getLast().priority > currentProcess.priority){
            currentProcess = RL.getLast();
        }
    }

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

    private void print_state(String state){
        out.println(state);
//        out.print(state + " ");
    }

    public static void main(String args[]) throws Exception {
        FileReader fr = new FileReader(IN);
        sc = new Scanner(fr);
        new Kernel().run();
    }

}
