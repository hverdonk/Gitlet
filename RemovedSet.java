package gitlet;

import java.io.Serializable;
import java.util.HashSet;

/** I made this class just so I could read back HashSets
 * without getting a casting error. Keys are File names,
 * values are whether or not Gitlet is tracking those files.
 * @author Hannah Verdonk */
public class RemovedSet implements Serializable {

    /** Creates a HashSet for storing files marked for
     * removal. */
    RemovedSet() {
        removed = new HashSet<>();
    }

    /** Returns the HashMap of tracked files. */
    public HashSet<String> removed() {
        return removed;
    }

    /** A listing of which files are being tracked. */
    private HashSet<String> removed;

}
