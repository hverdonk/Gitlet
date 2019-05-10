package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

import static gitlet.Utils.*;

/** Class for creating commit objects, the things that hold
 * blobs of data and their pointers.
 *  @author Hannah Verdonk */
public class Commit implements Serializable {

    /** Stores a copy of every tracked/staged file
     * as a commit object.  MSG is the commit
     * message entered by the user. PARENTSHA is
     * the pointer to this commit's parent. */
    Commit(String msg, String parentSha) {
        message = msg;
        time = LocalDateTime.now();
        parent = parentSha;
        secondParent = null;

        data = getParent().data();
        if (!getStaged().index().isEmpty()) {
            if (data == null) {
                data = getStaged().index();
            } else {
                data.putAll(getStaged().index());
            }
        }

        for (String filename : getRemoved().removed()) {
            if (data.containsKey(filename)) {
                data.remove(filename);
            }
        }

        Object[] tmp = data.values().toArray();
        String[] toSerialize = Arrays.copyOf(tmp, tmp.length, String[].class);

        String[] total = new String[toSerialize.length + 3];

        total[0] = message;
        total[1] = time.toString();
        total[2] = parent;
        System.arraycopy(toSerialize, 0, total, 3, toSerialize.length);

        sha = Utils.sha1((Object[]) total);
    }

    /** Builds the initial commit. */
    Commit() {
        message = "initial commit";
        time = LocalDateTime.of(YEAR, 1, 1, 0, 0);
        parent = null;
        secondParent = null;
        data = new HashMap<>();
        sha = sha1(message, time.toString());

        File removed = new File(Main.GITLET + "removed");
        RemovedSet r = new RemovedSet();
        Utils.writeObject(removed, r);
    }

    /** Builds a merge commit. MSG is which two
     * branches got merged, PARENTSHA is the current
     * branch, SECONDPARENTSHA is the merged branch,
     * BLOBFILES are the blobs this commit should contain. */
    Commit(String msg, String parentSha, String secondParentSha,
           HashMap<String, String> blobfiles) {
        message = msg;
        time = LocalDateTime.now();
        parent = parentSha;
        secondParent = secondParentSha;
        data = blobfiles;

        Object[] tmp = data.values().toArray();
        String[] toSerialize = Arrays.copyOf(tmp, tmp.length, String[].class);

        String[] total = new String[toSerialize.length + 3];

        total[0] = message;
        total[1] = time.toString();
        total[2] = parent;
        System.arraycopy(toSerialize, 0, total, 3, toSerialize.length);

        sha = Utils.sha1((Object[]) total);
    }

    /** Returns the deserialized Stage object
     * so it's contents are accessible. */
    static Stage getStaged() {
        File stage = new File(Main.STAGE);
        return readObject(stage, Stage.class);
    }

    /** Returns the deserialized parent commit,
     * making it's contents accessible. */
    Commit getParent() {
        File parentCommit = new File(Main.COMMITS + Main.S + this.parent());
        return readObject(parentCommit, Commit.class);
    }

    /** Returns the deserialized HashSet of
     * all files to be removed from tracking. */
    static RemovedSet getRemoved() {
        File removed = new File(Main.GITLET + Main.S + "removed");
        return readObject(removed, RemovedSet.class);
    }

    /** Returns the commit object at the head of the
     * current branch. */
    static Commit getLastCommit() {
        File active = new File(Main.BRANCHES + Main.S + "current");
        String currBranchName = readContentsAsString(active);
        File currBranch = new File(Main.BRANCHES + Main.S + currBranchName);
        String lastCommitName = readContentsAsString(currBranch);

        File lastCommit = new File(Main.COMMITS + Main.S + lastCommitName);
        return readObject(lastCommit, Commit.class);
    }




    /** Returns the shaCode of this commit's parent. */
    public String parent() {
        return this.parent;
    }

    /** Returns the shaCode of this commit's second parent,
     * or null if it has no second parent. */
    public String secondParent() {
        return secondParent;
    }

    /** Returns the time this commit was made. */
    public LocalDateTime time() {
        return this.time;
    }

    /** Returns this commit's sha1 hashcode. */
    public String shaCode() {
        return this.sha;
    }

    /** Returns this commit's commit message. */
    public String message() {
        return this.message;
    }

    /** Returns the ArrayList<String> of blob shaCodes
     * stored by this commit. */
    public HashMap<String, String> data() {
        return this.data;
    }



    /** Time the commit was made. */
    private LocalDateTime time;

    /** Message associated with the commit. */
    private String message;

    /** SHA1 hashcode of this commit. */
    private String sha;

    /** Most recent ancestor of this commit,
     * represented by it's shaCode. */
    private String parent;

    /** A second parent of this commit,
     * created by the merge command. */
    private String secondParent;

    /** Mapping of file names to blob objects
     * stored by this commit. */
    private HashMap<String, String> data;

    /** A field for the initial commit's year, so
     * style check will shut the hell up. */
    static final int YEAR = 1970;

}
