package retrivers;

import model.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jetbrains.annotations.NotNull;
import util.GitUtils;
import util.RegularExpression;
import util.VersionUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class CommitRetriever {

    Git git;
    Repository repo;
    VersionRetriever versionRetriever;
    List<RevCommit> commitList;

    public CommitRetriever(String repositoryPath, VersionRetriever versionRetriever) {
        this.repo = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repo);
        this.versionRetriever = versionRetriever;
    }

    private List<RevCommit> retrieveAssociatedCommits(@NotNull List<RevCommit> commits, Ticket ticket) {
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
    public List<Ticket> associateTicketAndCommit(List<Ticket> tickets) {
        try {
            List<RevCommit> commits = this.retrieveCommit();
            for (Ticket ticket : tickets) {
                List<RevCommit> associatedCommits = this.retrieveAssociatedCommits(commits, ticket);

                ticket.setAssociatedCommits(associatedCommits);
            }
            tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return tickets;
    }

    public List<ReleaseCommits> getReleaseCommits(VersionRetriever versionRetriever, List<RevCommit> commits) throws GitAPIException, IOException {
        List<ReleaseCommits> releaseCommits = new ArrayList<>();
        LocalDate lowerBound = LocalDate.of(1900, 1, 1);
        for(Version version : versionRetriever.getProjVersions()) {
            ReleaseCommits releaseCommit = GitUtils.getCommitsOfRelease(commits, version, lowerBound);
            if(releaseCommit != null) {
                List<JavaClass> javaClasses = getClasses(releaseCommit.getLastCommit());
                releaseCommit.setJavaClasses(javaClasses);
                releaseCommits.add(releaseCommit);
            }
            lowerBound = version.getDate();
        }

        return releaseCommits;
    }

    private List<JavaClass> getClasses(RevCommit commit) throws IOException {

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

                if(release == null) throw new RuntimeException();

                javaClasses.add(new JavaClass(
                        treeWalk.getPathString(),
                        new String(this.repo.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8),
                        release));
            }
        }
        treeWalk.close();

        return javaClasses;
    }

    private void retrieveChanges(List<RevCommit> commits) {
        for(RevCommit commit: commits) {
            List<JavaClassChange> javaClassChangeList = retrieveChanges(commit);


        }
    }

    private List<JavaClassChange> retrieveChanges(RevCommit commit) {
        List<JavaClassChange> javaClassChangeList = new ArrayList<>();
        try {
            ObjectReader reader = git.getRepository().newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = git.getRepository().resolve(commit.getName() + "~1^{tree}");
            oldTreeIter.reset(reader, oldTree);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = git.getRepository().resolve(commit.getName() + "^{tree}");
            newTreeIter.reset(reader, newTree);

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(git.getRepository());
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);

            for (DiffEntry entry : entries) {
                JavaClass javaClass = new JavaClass(
                        entry.getNewPath(),
                        new String(this.repo.open(entry.getNewId().toObjectId()).getBytes(), StandardCharsets.UTF_8),
                        VersionUtil.retrieveNextRelease(
                                versionRetriever,
                                GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()))
                );
                JavaClassChange newJavaClassChange = new JavaClassChange(javaClass, entry.getChangeType());
                javaClassChangeList.add(newJavaClassChange);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return javaClassChangeList;
    }


}
