package zhuliang.nus.cs2106;

/**
 * Utility Class
 * Created by paradite on 16/9/14.
 */
public class Utils {
    //    Signals:
    static public final int SIGNAL_SUCCESS = 1;
    static public final int SIGNAL_NOTFOUND = 0;
    static public final int SIGNAL_ALREADYEXIST = 5;
    //    Status:
//     -1 - error
//      0 - ready
//      1 - running
//      2 - blocked
    static public final int STATUS_ERROR = -1;
    static public final int STATUS_READY = 0;
    static public final int STATUS_RUNNING = 1;
    static public final int STATUS_BLOCKED = 2;
    //    Texts
    static final String TEXT_ERROR = "error";
    static final String TEXT_CRITICAL_ERROR = "critical error";
    static final String TEXT_INIT = "init";
    static final String TEXT_CREATE = "cr";
    static final String TEXT_DESTROY = "de";
    static final String TEXT_REQUEST = "req";
    static final String TEXT_RELEASE = "rel";
    static final String TEXT_TIMEOUT = "to";

    static final String FILENAME = "testGR-sample";
    static final String OUT      = FILENAME + ".out";
    static String IN             = FILENAME + ".txt";
}
