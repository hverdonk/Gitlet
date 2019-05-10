package gitlet;


import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;

/** A class for copying user-provided files.
 *
 * @author Hannah Verdonk */
public class Blob implements Serializable {

    /** Creates a Blob object for a file F. */
    Blob(File f) {
        fileContents = readContents(f);
        hash = sha1(fileContents);
    }

    /** Returns the file contents stored by this blob. */
    public byte[] fileContents() {
        return this.fileContents;
    }

    /** Returns the SHA1 hashcode of this blob. */
    public String shaCode() {
        return this.hash;
    }



    /** The file contents stored by this blob. */
    private byte[] fileContents;

    /** The SHA1 hashcode of this blob,
     * obtained by passing the byte[] of the
     * provided file's contents to SHA1(). */
    private String hash;


}
