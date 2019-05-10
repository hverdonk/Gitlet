package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import static gitlet.Utils.*;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Hannah Verdonk, with ideas and suggestions from Rae Berkley
 *  and Kathy Liu.
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            File g = new File(GITLET);
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            } else if (!args[0].equals("init") && !g.exists()) {
                String m = "Not in an initialized Gitlet directory.";
                throw new GitletException(m);
            }

            if (args.length > 1) {
                doLongSwitch(args);
            } else {
                doShortSwitch(args);
            }

        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Checks if the number of args EXPECTED are provided
     * by the user in ARGS. */
    static void validArgs(int expected, String... args) {
        if (args.length != expected) {
            throw new GitletException("Incorrect operands.");
        }
    }

    /** Checks if the correct number of arguments ARGS for each
     * type of checkout command are provided by the user. */
    static void validCheckoutArgs(String... args) {
        if (args.length == 3 && !args[1].equals("--")) {
            throw new GitletException("Incorrect operands.");
        } else if (args.length == 4 && !args[2].equals("--")) {
            throw new GitletException("Incorrect operands.");
        } else if (args.length == 2) {
            File curr = new File(BRANCHES + S + "current");
            String currBranch = readContentsAsString(curr);
            File branchName = new File(BRANCHES + S + args[1]);
            if (!branchName.exists()) {
                throw new GitletException("No such branch exists.");
            } else if (args[1].equals(currBranch)) {
                String m = "No need to checkout the current branch.";
                throw new GitletException(m);
            }
        }
    }

    /** Checks if the provided ARGS are valid for
     * the merge command. */
    static void validMerge(String... args) {
        validArgs(2, args);

        File active = new File(BRANCHES + S + "current");
        String currBranchName = readContentsAsString(active);
        if (currBranchName.equals(args[1])) {
            throw new GitletException("Cannot merge a branch with itself.");
        }

        File branch = new File(BRANCHES + S + args[1]);
        if (!branch.exists()) {
            String m = "A branch with that name does not exist.";
            throw new GitletException(m);
        }

        Stage st = readObject(new File(STAGE), Stage.class);
        File removed = new File(GITLET + S + "removed");
        RemovedSet r = readObject(removed, RemovedSet.class);
        if (!st.index().isEmpty() || !r.removed().isEmpty()) {
            throw new GitletException("You have uncommitted changes.");
        }
    }

    /** Parses commands with one argument in ARGS. */
    static void doShortSwitch(String... args) {
        switch (args[0]) {
        case "init":
            validArgs(1, args);
            doInit();
            break;
        case "log":
            validArgs(1, args);
            doLog();
            break;
        case "global-log":
            validArgs(1, args);
            doGlobalLog();
            break;
        case "status":
            validArgs(1, args);
            doStatus();
            break;
        default:
            throw new GitletException("No command with that name exists.");
        }
    }

    /** Parses commands with more than one argument in ARGS. */
    static void doLongSwitch(String... args) {
        switch (args[0]) {
        case "add":
            validArgs(2, args);
            String curr = System.getProperty("user.dir");
            File toAdd = new File(curr + S + args[1]);
            doAdd(toAdd, args[1]);
            break;
        case "commit":
            if (args.length == 1 || args[1].equals("")) {
                throw new GitletException("Please enter a commit message.");
            }
            validArgs(2, args);
            doCommit(args[1]);
            break;
        case "rm":
            validArgs(2, args);
            String temp = System.getProperty("user.dir");
            File toRemove = new File(temp + S + args[1]);
            doRemove(toRemove, args[1]);
            break;
        case "find":
            validArgs(2, args);
            doFind(args[1]);
            break;
        case "checkout":
            validCheckoutArgs(args);
            doCheckout(args);
            break;
        case "branch":
            validArgs(2, args);
            doBranch(args[1]);
            break;
        case "rm-branch":
            validArgs(2, args);
            doRemoveBranch(args[1]);
            break;
        case "reset":
            validArgs(2, args);
            untrackedFileCheck();
            doReset(args[1]);
            break;
        case "merge":
            untrackedFileCheck();
            validMerge(args);
            doMerge(args[1]);
            break;
        default:
            throw new GitletException("No command with that name exists.");
        }
    }

    /** Performs the init command. */
    static void doInit() {
        if ((new File(GITLET)).exists()) {
            String m1 = "A Gitlet version-control system already ";
            String m2 = "exists in the current directory.";
            throw new GitletException(m1 + m2);
        } else {
            File stageFile = new File(STAGE);
            File branchesFile = new File(BRANCHES);
            File blobsFile = new File(BLOBS);
            File commitsFile = new File(COMMITS);

            (new File(GITLET)).mkdir();
            Utils.writeObject(stageFile, new Stage());
            branchesFile.mkdir();
            blobsFile.mkdir();
            commitsFile.mkdir();

            Commit initial = new Commit();
            File firstCommit = new File(COMMITS + S + initial.shaCode());
            Utils.writeObject(firstCommit, initial);

            File master = new File(BRANCHES + S + "master");
            Utils.writeContents(master, initial.shaCode());
            File currBranch = new File(BRANCHES + S + "current");
            Utils.writeContents(currBranch, "master");
        }
    }

    /** Performs the add command. Adds file F with
     * name NAME to the stage. */
    static void doAdd(File f, String name) {
        if (!f.exists()) {
            throw new GitletException("File does not exist.");
        }

        File stageFile = new File(STAGE);

        Stage index = Utils.readObject(stageFile, Stage.class);
        index.add(f, name);
        Utils.writeObject(stageFile, index);
    }

    /** Performs the rm command. Removes file F with
     * name NAME from the stage. */
    static void doRemove(File f, String name) {
        File stageFile = new File(STAGE);

        Stage index = Utils.readObject(stageFile, Stage.class);
        index.remove(f, name);
        Utils.writeObject(stageFile, index);
    }

    /** Performs the commit command. Makes a commit
     * object with MESSAGE. */
    static void doCommit(String message) {
        File removed = new File(GITLET + S + "removed");
        RemovedSet r = readObject(removed, RemovedSet.class);
        if (Commit.getStaged().index().isEmpty() && r.removed().isEmpty()) {
            throw new GitletException("No changes added to the commit.");
        }

        File active = new File(BRANCHES + S + "current");
        String currBranchName = readContentsAsString(active);
        File currBranch = new File(BRANCHES + S + currBranchName);

        String lastCommit = readContentsAsString(currBranch);

        Commit c = new Commit(message, lastCommit);
        File newCommit = new File(COMMITS + S + c.shaCode());

        Utils.writeObject(newCommit, c);

        File newBranch = new File(BRANCHES + S + currBranchName);
        Utils.writeContents(newBranch, c.shaCode());

        RemovedSet newR = new RemovedSet();
        Utils.writeObject(removed, newR);

        Utils.writeObject(new File(STAGE), new Stage());

    }

    /** Performs the branch command. Creates a
     * branch with NAME. */
    static void doBranch(String name) {
        File b = new File(BRANCHES + S + name);
        if (b.exists()) {
            String m = "A branch with that name already exists.";
            throw new GitletException(m);
        }

        File active = new File(BRANCHES + S + "current");
        String currBranchName = readContentsAsString(active);
        File currBranch = new File(BRANCHES + S + currBranchName);

        String lastCommit = readContentsAsString(currBranch);

        File newBranch = new File(BRANCHES + S + name);
        Utils.writeContents(newBranch, lastCommit);
    }

    /** Performs the rm-branch command. Removes
     * branch with NAME. */
    static void doRemoveBranch(String name) {
        File active = new File(BRANCHES + S + "current");
        String currBranchName = readContentsAsString(active);

        if (currBranchName.equals(name)) {
            throw new GitletException("Cannot remove the current branch.");
        }

        File removeBranch = new File(BRANCHES + S + name);
        if (!removeBranch.exists()) {
            String m = "A branch with that name does not exist.";
            throw new GitletException(m);
        }

        removeBranch.delete();

    }

    /** Performs the checkout command. Takes in
     * ARGS provided by the user. */
    static void doCheckout(String... args) {
        if (args.length == 2) {
            checkoutBranch(args);
        } else if (args.length == 3) {
            checkoutFile(args);
        } else if (args.length == 4) {
            checkoutID(args);
        }
    }


    /** A helper method for the doCheckout method.
     * Takes in ARGS input by the user. */
    static void checkoutFile(String... args) {
        Commit c = Commit.getLastCommit();

        if (!c.data().containsKey(args[2])) {
            String m = "File does not exist in that commit.";
            throw new GitletException(m);
        } else {
            String blobFileName = c.data().get(args[2]);
            File blobFile = new File(BLOBS + S + blobFileName);
            Blob b = readObject(blobFile, Blob.class);

            String curr = System.getProperty("user.dir");
            String contents = new String(b.fileContents(),
                    StandardCharsets.UTF_8);
            File dest = new File(curr + S + args[2]);
            writeContents(dest, contents);
        }
    }

    /** A helper method for the doCheckout method.
     * Takes in ARGS input by the user. */
    static void checkoutID(String... args) {
        File lastCommit = null;

        if (args[1].length() < UID_LENGTH) {
            List<String> allCommits = plainFilenamesIn(COMMITS);
            boolean foundCommit = false;
            for (String commit : allCommits) {
                String temp = commit.substring(0, args[1].length());
                if (temp.equals(args[1])) {
                    foundCommit = true;
                    lastCommit = new File(COMMITS + S + commit);
                    break;
                }
            }
            if (!foundCommit) {
                throw new GitletException("No commit with that id exists.");
            }
        } else {
            lastCommit = new File(COMMITS + S + args[1]);
        }

        if (!lastCommit.exists()) {
            throw new GitletException("No commit with that id exists.");
        }

        Commit c = readObject(lastCommit, Commit.class);

        if (!c.data().containsKey(args[3])) {
            String m = "File does not exist in that commit.";
            throw new GitletException(m);
        } else {
            String blobFileName = c.data().get(args[3]);
            File blobFile = new File(BLOBS + S + blobFileName);
            Blob b = readObject(blobFile, Blob.class);

            String contents = new String(b.fileContents(),
                    StandardCharsets.UTF_8);
            File dest = new File(System.getProperty("user.dir") + S + args[3]);
            writeContents(dest, contents);
        }
    }

    /** A helper method for the doCheckout method.
     * Takes in ARGS input by the user. */
    static void checkoutBranch(String... args) {
        untrackedFileCheck();
        File destBranch = new File(BRANCHES + S + args[1]);
        String branchCommitName = readContentsAsString(destBranch);
        File branchCommit = new File(COMMITS + S + branchCommitName);
        Commit c = Utils.readObject(branchCommit, Commit.class);

        HashMap<String, String> blobCodes = c.data();

        File currDir = new File(System.getProperty("user.dir"));
        List<String> currFiles = plainFilenamesIn(currDir);
        if (currFiles != null) {
            for (String name : currFiles) {
                if (!blobCodes.containsKey(name)) {
                    String curr = System.getProperty("user.dir");
                    File temp = new File(curr + S + name);
                    restrictedDelete(temp);
                }
            }
        }

        if (!blobCodes.isEmpty()) {
            for (String filename : blobCodes.keySet()) {
                String shaCode = blobCodes.get(filename);
                File blobFile = new File(BLOBS + S + shaCode);
                Blob b = readObject(blobFile, Blob.class);

                String curr = System.getProperty("user.dir");
                String contents = new String(b.fileContents(),
                        StandardCharsets.UTF_8);
                File dest = new File(curr + S + filename);
                writeContents(dest, contents);
            }
        }

        File currBranchHolder = new File(BRANCHES + S + "current");
        File removed = new File(GITLET + S + "removed");

        writeContents(currBranchHolder, args[1]);
        writeObject(new File(STAGE), new Stage());
        writeObject(removed, new RemovedSet());

    }

    /** Performs the log command. */
    static void doLog() {
        Commit last = Commit.getLastCommit();
        while (last.parent() != null) {
            System.out.println("===");
            System.out.println("commit " + last.shaCode());
            if (last.secondParent() != null) {
                String parent1 = last.parent().substring(0, 8);
                String parent2 = last.secondParent().substring(0, 8);
                System.out.println("Merge: " + parent1 + " " + parent2);
            }

            ZoneId oldZone = ZoneId.of("UTC");
            ZoneId newZone = ZoneId.of("America/Los_Angeles");
            ZonedDateTime converted = last.time().atZone(oldZone)
                    .withZoneSameInstant(newZone);
            System.out.printf("Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                    converted);
            System.out.println("");
            System.out.println(last.message());
            System.out.println("");

            File parent = new File(COMMITS + S + last.parent());
            last = readObject(parent, Commit.class);
        }
        System.out.println("===");
        System.out.println("commit " + last.shaCode());

        ZoneId oldZone = ZoneId.of("UTC");
        ZoneId newZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime converted = last.time().atZone(oldZone)
                .withZoneSameInstant(newZone);
        System.out.printf("Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                converted);
        System.out.println("");
        System.out.println(last.message());
        System.out.println("");
    }

    /** Performs the global-log command. */
    static void doGlobalLog() {
        File[] commitFiles = (new File(COMMITS)).listFiles();
        if (commitFiles == null) {
            return;
        }
        for (File f : commitFiles) {
            Commit c = readObject(f, Commit.class);
            System.out.println("===");
            System.out.println("commit " + c.shaCode());
            if (c.secondParent() != null) {
                String parent1 = c.parent().substring(0, 8);
                String parent2 = c.secondParent().substring(0, 8);
                System.out.println("Merge: " + parent1 + " " + parent2);
            }

            ZoneId oldZone = ZoneId.of("UTC");
            ZoneId newZone = ZoneId.of("America/Los_Angeles");
            ZonedDateTime converted = c.time().atZone(oldZone)
                    .withZoneSameInstant(newZone);
            System.out.printf("Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                    converted);
            System.out.println("");
            System.out.println(c.message());
            System.out.println("");
        }

    }

    /** Performs the status command. */
    static void doStatus() {
        System.out.println("=== Branches ===");
        File active = new File(BRANCHES + S + "current");
        String currBranchName = readContentsAsString(active);
        for (String branchName : plainFilenamesIn(new File(BRANCHES))) {
            if (branchName.equals(currBranchName)) {
                System.out.print("*");
            }
            if (!branchName.equals("current")) {
                System.out.println(branchName);
            }
        }
        System.out.println("");

        System.out.println("=== Staged Files ===");
        Stage currStage = readObject(new File(STAGE), Stage.class);
        Object[] keys = currStage.index().keySet().toArray();
        Arrays.sort(keys);
        for (Object key : keys) {
            System.out.println(key);
        }
        System.out.println("");

        System.out.println("=== Removed Files ===");
        File removed = new File(GITLET + S + "removed");
        RemovedSet r = readObject(removed, RemovedSet.class);
        Object[] names = r.removed().toArray();
        Arrays.sort(names);
        for (Object key : names) {
            System.out.println(key);
        }
        System.out.println("");

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");
    }

    /** Performs the find command. Finds any commits that
     * contain MESSAGE. */
    static void doFind(String message) {
        boolean found = false;
        File[] commitFiles = (new File(COMMITS)).listFiles();
        if (commitFiles != null) {
            for (File f : commitFiles) {
                Commit c = readObject(f, Commit.class);
                if (message.equals(c.message())) {
                    System.out.println(c.shaCode());
                    found = true;
                }
            }
        }
        if (!found) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /** Performs the reset command. Resets the working
     * directory to the contents of the commit with
     * COMMITID. */
    static void doReset(String commitID) {
        File lastCommit = null;

        if (commitID.length() < UID_LENGTH) {
            List<String> allCommits = plainFilenamesIn(COMMITS);
            boolean foundCommit = false;
            for (String commit : allCommits) {
                String temp = commit.substring(0, commitID.length());
                if (temp.equals(commitID)) {
                    foundCommit = true;
                    lastCommit = new File(COMMITS + S + commit);
                    break;
                }
            }
            if (!foundCommit) {
                throw new GitletException("No commit with that id exists.");
            }
        } else {
            lastCommit = new File(COMMITS + S + commitID);
        }

        if (!lastCommit.exists()) {
            throw new GitletException("No commit with that id exists.");
        }

        Commit c = readObject(lastCommit, Commit.class);
        Commit curr = Commit.getLastCommit();

        File currDirFile = new File(System.getProperty("user.dir"));
        List<String> currFiles = plainFilenamesIn(currDirFile);
        if (currFiles != null) {
            for (String name : currFiles) {
                if (!c.data().containsKey(name)
                        && curr.data().containsKey(name)) {
                    String currDir = System.getProperty("user.dir");
                    File temp = new File(currDir + S + name);
                    restrictedDelete(temp);
                }
            }
        }

        for (String name : c.data().keySet()) {
            String b = c.data().get(name);
            File blobFile = new File(BLOBS + S + b);
            Blob blobby = readObject(blobFile, Blob.class);

            String contents = new String(blobby.fileContents(),
                    StandardCharsets.UTF_8);
            File dest = new File(System.getProperty("user.dir") + S + name);
            writeContents(dest, contents);
        }

        File currBranchFile = new File(BRANCHES + S + "current");
        String currBranchName = readContentsAsString(currBranchFile);
        File currBranch = new File(BRANCHES + S + currBranchName);
        writeContents(currBranch, commitID);
    }

    /** Performs the merge command. Merges branch
     * BRANCHNAME into the current branch. */
    static void doMerge(String branchName) {
        HashMap<String, String> newContents = new HashMap<>();

        File splitFile = new File(COMMITS + S + findSplitPoint(branchName));
        File branchFile = new File(BRANCHES + S + branchName);
        String otherSha = readContentsAsString(branchFile);
        File otherCommit = new File(COMMITS + S + otherSha);
        Commit split = readObject(splitFile, Commit.class);
        Commit other = readObject(otherCommit, Commit.class);
        Commit curr = Commit.getLastCommit();

        Set<String> otherNames = other.data().keySet();
        Set<String> currNames = curr.data().keySet();


        if (split.shaCode().equals(other.shaCode())) {
            String m = "Given branch is an ancestor of the current branch.";
            System.out.println(m);
            return;
        } else if (split.shaCode().equals(curr.shaCode())) {
            doReset(other.shaCode());
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        if (split.data() != null) {
            Set<String> splitNames = split.data().keySet();
            for (String name : splitNames) {
                if (currNames.contains(name) && otherNames.contains(name)) {
                    if (curr.data().get(name).equals(other.data().get(name))) {
                        newContents.put(name, curr.data().get(name));
                        currNames.remove(name);
                        otherNames.remove(name);
                    } else if (curr.data().get(name)
                            .equals(split.data().get(name))) {
                        checkoutID("checkout", other.shaCode(), "--", name);
                        newContents.put(name, other.data().get(name));
                        String c = System.getProperty("user.dir");
                        File dest = new File(c + S + name);
                        otherNames.remove(name);
                        currNames.remove(name);
                    } else if (other.data().get(name)
                            .equals(split.data().get(name))) {
                        newContents.put(name, other.data().get(name));
                        otherNames.remove(name);
                        currNames.remove(name);
                    }
                } else if (currNames.contains(name)) {
                    if (curr.data().get(name).equals(split.data().get(name))) {
                        String c = System.getProperty("user.dir");
                        File dest = new File(c + S + name);
                        restrictedDelete(dest);
                        currNames.remove(name);
                    }
                } else if (otherNames.contains(name)) {
                    if (other.data().get(name).equals(split.data().get(name))) {
                        otherNames.remove(name);
                    }
                }
            }
        }
        List<String> toRemove1 = new ArrayList<>();
        for (String name : currNames) {
            if (other.data().get(name) == null) {
                toRemove1.add(name);
                newContents.put(name, curr.data().get(name));
            } else if (curr.data().get(name).equals(other.data().get(name))) {
                newContents.put(name, curr.data().get(name));
                currNames.remove(name);
                otherNames.remove(name);
            }
        }
        currNames.removeAll(toRemove1);

        List<String> toRemove2 = new ArrayList<>();
        for (String name : otherNames) {
            if (curr.data().get(name) == null) {
                checkoutID("checkout", other.shaCode(), "--", name);
                newContents.put(name, other.data().get(name));
                toRemove2.add(name);
                File dest = new File(System.getProperty("user.dir") + S + name);
            }
        }
        otherNames.removeAll(toRemove2);

        String top = "<<<<<<< HEAD" + "\n";
        String middle = "=======" + "\n";
        String bottom = ">>>>>>>" + "\n";
        boolean isConflict = !currNames.isEmpty() || !otherNames.isEmpty();

        for (String name : currNames) {
            File dest = new File(System.getProperty("user.dir") + S + name);

            Blob bCurr = getBlob(curr.data().get(name));
            String currContents = new String(bCurr.fileContents(),
                    StandardCharsets.UTF_8);

            String otherContents = "";
            if (other.data().get(name) != null) {
                Blob bOther = getBlob(other.data().get(name));
                otherContents = new String(bOther.fileContents(),
                        StandardCharsets.UTF_8);
                otherNames.remove(name);
            }

            writeContents(dest, top, currContents,
                    middle, otherContents, bottom);

            Blob merged = new Blob(dest);
            File blobFile = new File(BLOBS + S + merged.shaCode());
            writeObject(blobFile, merged);
            newContents.put(name, merged.shaCode());
        }

        for (String name : otherNames) {
            File dest = new File(System.getProperty("user.dir") + S + name);
            Blob bOther = getBlob(other.data().get(name));

            String otherContents = new String(bOther.fileContents(),
                    StandardCharsets.UTF_8);

            writeContents(dest, top, middle, otherContents, bottom);

            Blob merged = new Blob(dest);
            File blobFile = new File(BLOBS + S + merged.shaCode());
            writeObject(blobFile, merged);
            newContents.put(name, merged.shaCode());
        }

        File active = new File(Main.BRANCHES + Main.S + "current");
        String currBranchName = readContentsAsString(active);
        String m = "Merged " + branchName + " into " + currBranchName + ".";
        Commit mergeCommit = new Commit(m, curr.shaCode(),
                other.shaCode(), newContents);
        File mergeFile = new File(COMMITS + S + mergeCommit.shaCode());
        writeObject(mergeFile, mergeCommit);
        File currBranch = new File(BRANCHES + S + currBranchName);
        writeContents(currBranch, mergeCommit.shaCode());

        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Returns the blob object named BLOBSHA. */
    static Blob getBlob(String blobSha) {
        File blob = new File(BLOBS + S + blobSha);
        return readObject(blob, Blob.class);
    }

    /** Returns the split point between the current branch
     * and BRANCHNAME. */
    static String findSplitPoint(String branchName) {
        HashSet<String> currBranchCommits = new HashSet<>();
        Commit c = Commit.getLastCommit();
        while (c.parent() != null) {
            currBranchCommits.add(c.shaCode());
            c = c.getParent();
        }
        currBranchCommits.add(c.shaCode());

        File givenBranchFile = new File(BRANCHES + S + branchName);
        String commitName = readContentsAsString(givenBranchFile);
        File commitFile = new File(COMMITS + S + commitName);
        Commit c2 = readObject(commitFile, Commit.class);
        while (true) {
            if (currBranchCommits.contains(c2.shaCode())) {
                return c2.shaCode();
            }
            c2 = c2.getParent();
        }
    }

    /** A shortname for the system-dependent
     * file separator character. */
    static final String S = File.separator;

    /** A String representing the path to the
     * .gitlet directory. */
    static final String GITLET = System.getProperty("user.dir")
            + S + ".gitlet" + S;

    /** A String representing the path to the
     * stage file. */
    static final String STAGE = GITLET + "stage";

    /** A String representing the path to the
     * branches directory. */
    static final String BRANCHES = GITLET + "branches";

    /** A String representing the path to the
     * blobs directory. */
    static final String BLOBS = GITLET + "blobs";

    /** A String representing the path to the
     * commits directory. */
    static final String COMMITS = GITLET + "commits";

}
