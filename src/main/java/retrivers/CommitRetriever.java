package retrivers;

import model.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jetbrains.annotations.NotNull;
import utils.GitUtils;
import utils.JavaClassUtil;
import utils.RegularExpression;
import utils.VersionUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommitRetriever {

    private final Git git;
    private final Repository repo;
    private final VersionRetriever versionRetriever;
    private List<RevCommit> commitList;

    public CommitRetriever(String repositoryPath, VersionRetriever versionRetriever) {
        this.repo = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repo);
        this.versionRetriever = versionRetriever;
    }

    private @NotNull List<RevCommit> retrieveAssociatedCommits(@NotNull List<RevCommit> commits, Ticket ticket) {
        List<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits) {
            if(RegularExpression.matchRegex(commit.getFullMessage(), ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

    public List<RevCommit> retrieveCommit() throws GitAPIException {
        if(commitList != null) return commitList;

        Iterable<RevCommit> commitIterable = git.log().call();

        List<RevCommit> commits = new ArrayList<>();
        List<Version> projVersions = versionRetriever.getProjVersions();
        Version lastVersion = projVersions.get(projVersions.size()-1);

        for(RevCommit commit: commitIterable) {
            if (!GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()).isAfter(lastVersion.getDate())){
                commits.add(commit);
            }
        }

        commits.sort(Comparator.comparing(o -> o.getCommitterIdent().getWhen()));

        this.commitList = commits;

        return commits;
    }

    /** Associate the tickets with the commits that reference them. Moreover, discard the tickets that don't have any commits.
     *
     * @param tickets: tickets list that must be associate to the relative commits
     * @return the modified list
     */
    public void associateTicketAndCommit(@NotNull List<Ticket> tickets) {
        try {
            List<RevCommit> commits = this.retrieveCommit();
            for (Ticket ticket : tickets) {
                List<RevCommit> associatedCommits = this.retrieveAssociatedCommits(commits, ticket);
                List<RevCommit> consistentCommits = new ArrayList<>();

                for(RevCommit commit: associatedCommits) {
                    LocalDate when = GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen());

                    //TODO controllare se i commit presi sono giusti -> controllare se le classi buggy sono giuste
                    if(!ticket.getFixedRelease().getDate().isBefore(when) && //commitDate <= fixedVersionDate
                        !ticket.getInjectedRelease().getDate().isAfter(when)) { //commitDate > injectedVersionDate
                        consistentCommits.add(commit);
                    }
                }
                ticket.setAssociatedCommits(consistentCommits);
            }
            tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty()); //Discard tickets that have no associated commits
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ReleaseCommits> getReleaseCommits(@NotNull VersionRetriever versionRetriever, List<RevCommit> commits) throws IOException {

        List<ReleaseCommits> releaseCommits = new ArrayList<>();
        LocalDate lowerBound = LocalDate.of(1900, 1, 1);
        for(Version version : versionRetriever.getProjVersions()) {
            ReleaseCommits releaseCommit = GitUtils.getCommitsOfRelease(commits, version, lowerBound);
            if(releaseCommit != null) {
                List<JavaClass> javaClasses = getClasses(releaseCommit.getLastCommit());
                releaseCommit.setJavaClasses(javaClasses);
                releaseCommits.add(releaseCommit);
                JavaClassUtil.updateJavaClassCommits(this, releaseCommit.getCommits(), javaClasses);
            }
            lowerBound = version.getDate();
        }

        return releaseCommits;
    }

    private @NotNull List<JavaClass> getClasses(@NotNull RevCommit commit) throws IOException {

        List<JavaClass> javaClasses = new ArrayList<>();

        RevTree tree = commit.getTree();    //We get the tree of the files and the directories that were belong to the repository when commit was pushed
        TreeWalk treeWalk = new TreeWalk(this.repo);    //We use a TreeWalk to iterate over all files in the Tree recursively
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while(treeWalk.next()) {
            //We are keeping only Java classes that are not involved in tests
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                //We are retrieving (name class, content class) couples
                Version release = VersionUtil.retrieveNextRelease(versionRetriever, GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()));

                if(release == null) throw new RuntimeException(); //Throw an exception if there isn't a version after the commit

                javaClasses.add(new JavaClass(
                        treeWalk.getPathString(),
                        new String(this.repo.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8),
                        release));
            }
        }
        treeWalk.close();

        return javaClasses;
    }

    public List<ChangedJavaClass> retrieveChanges(@NotNull RevCommit commit) {
        List<ChangedJavaClass> changedJavaClassList = new ArrayList<>();
        try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            RevCommit parentComm = commit.getParent(0);

            diffFormatter.setRepository(this.repo);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

            List<DiffEntry> entries = diffFormatter.scan(parentComm.getTree(), commit.getTree());

            for (DiffEntry entry : entries) {
                ChangedJavaClass newChangedJavaClass = new ChangedJavaClass(entry.getNewPath());
                changedJavaClassList.add(newChangedJavaClass);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            //commit has no parents: this is the first commit, so add all classes
            try {
                List<JavaClass> javaClasses = getClasses(commit);
                changedJavaClassList = JavaClassUtil.createChangedJavaClass(javaClasses);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        return changedJavaClassList;
    }

    public String getContentOfClassByCommit(String className, @NotNull RevCommit commit) throws IOException {

        RevTree tree = commit.getTree();
        // Tree walk to iterate over all files in the Tree recursively

        TreeWalk treeWalk = new TreeWalk(this.repo);

        treeWalk.addTree(tree);

        treeWalk.setRecursive(true);

        while (treeWalk.next()) {
            // We are keeping only Java classes that are not involved in tests
            if (treeWalk.getPathString().equals(className)) {
                String content = new String(this.repo.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8);
                treeWalk.close();
                return content;
            }
        }

        treeWalk.close();
        // If here it mean no class with name className is present
        return null;
    }

    /**This method initializes two lists:
     * - List of numbers of added lines by each commit; every entry is associated to one specific commit
     * - List of numbers of deleted lines by each commit; every entry is associated to one specific commit
     * These lists will be used to calculate sum, max & avg*/
    public void computeAddedAndDeletedLinesList(@NotNull JavaClass javaClass) throws IOException {

        for(RevCommit comm : javaClass.getCommits()) {
            try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

                RevCommit parentComm = comm.getParent(0);

                diffFormatter.setRepository(this.repo);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

                List<DiffEntry> diffs = diffFormatter.scan(parentComm.getTree(), comm.getTree());
                for(DiffEntry entry : diffs) {
                    if(entry.getNewPath().equals(javaClass.getName())) {
                        javaClass.getMetrics().getAddedLinesList().add(getAddedLines(diffFormatter, entry));
                        javaClass.getMetrics().getDeletedLinesList().add(getDeletedLines(diffFormatter, entry));
                        break;
                    }
                }

            } catch(ArrayIndexOutOfBoundsException e) {
                //commit has no parents: skip this commit, return an empty list and go on
            }
        }
    }

    private int getAddedLines(@NotNull DiffFormatter diffFormatter, DiffEntry entry) throws IOException {

        int addedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
        }
        return addedLines;

    }

    private int getDeletedLines(@NotNull DiffFormatter diffFormatter, DiffEntry entry) throws IOException {

        int deletedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            deletedLines += edit.getEndA() - edit.getBeginA();
        }
        return deletedLines;

    }
}
