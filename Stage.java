package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

import static gitlet.Utils.*;

/** A temporary storage space for files to be committed.
 * @author Hannah Verdonk */
public class Stage implements Serializable {

    /** Creates a new, empty stage. */
    Stage() {
        index = new HashMap<>();
    }

    /** Adds File F with name NAME to the stage, and sets it
     * to be tracked. */
    public void add(File f, String name) {

        if (inParentCommit(f, name)) {
            File removed = new File(Main.GITLET + "removed");
            RemovedSet r = readObject(removed, RemovedSet.class);

            if (r.removed().contains(name)) {
                r.removed().remove(name);
            }
            writeObject(removed, r);

            if (this.index.containsKey(name)) {
                this.index.remove(name);
            }
            return;
        } else {
            Blob b = new Blob(f);
            String sha = b.shaCode();
            File blobFile = new File(Main.BLOBS + Main.S + sha);

            if (!this.index.containsKey(name)) {
                File delBlobFile = new File(Main.BLOBS + Main.S
                        + this.index.get(name));
                delBlobFile.delete();
                this.index.put(name, b.shaCode());
                writeObject(blobFile, b);
            } else if (this.index.get(name).equals(sha)) {
                return;
            } else {
                this.index.put(name, b.shaCode());
                writeObject(blobFile, b);
            }
        }
    }


    /** Stops file F with name NAME from being tracked and
     * removes it from the stage, if it has
     * been added. */
    public void remove(File f, String name) {
        RemovedSet r = Commit.getRemoved();
        boolean untracked = !inParentCommit(f, name)
                || r.removed().contains(name);

        if (!index.containsKey(name) && untracked) {
            throw new GitletException("No reason to remove the file.");
        } else {
            if (index.containsKey(name)) {
                index.remove(name);
            }

            if (inParentCommit(f, name) && !r.removed().contains(name)) {
                r.removed().add(name);
                File removed = new File(Main.GITLET + Main.S + "removed");
                writeObject(removed, r);

                if (f.exists()) {
                    Utils.restrictedDelete(f);
                }
            }
        }
    }


    /** Returns TRUE if File F with name NAME exists in
     * the head commit of my current branch. */
    boolean inParentCommit(File f, String name) {
        Commit head = Commit.getLastCommit();
        HashMap<String, String> commitContents = head.data();
        if (commitContents == null) {
            return false;
        } else if (!commitContents.containsKey(name)) {
            return false;
        } else if (!f.exists()) {
            return commitContents.containsKey(name);
        } else {
            String blobSha = commitContents.get(name);
            File blobFile = new File(Main.BLOBS + Main.S + blobSha);
            Blob b = readObject(blobFile, Blob.class);
            byte[] committedFileContents = b.fileContents();
            byte[] currFileContents = readContents(f);
            return Arrays.equals(currFileContents, committedFileContents);
        }
    }


    /** Returns the contents of the stage. */
    public HashMap<String, String> index() {
        return index;
    }

    /** The location of the stored files. Keys
     * are file names in the working directory,
     * values are the SHA1 codes of blob file
     * contents. */
    private HashMap<String, String> index;

}
