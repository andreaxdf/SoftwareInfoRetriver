package retrivers;

import model.ReleaseCommits;
import model.Ticket;
import model.VersionInfo;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<VersionInfo> projVersions = versionRetriever.getProjVersions();

        for(RevCommit commit: commitIterable) {
            if (!GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()).isAfter(projVersions.get(projVersions.size()-1).getDate())){
                commits.add(commit);
            }
        }

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
        LocalDate date = LocalDate.of(1900, 1, 1);
        for(VersionInfo versionInfo: versionRetriever.getProjVersions()) {
            ReleaseCommits releaseCommit = GitUtils.getCommitsOfRelease(commits, versionInfo, date);
            if(releaseCommit != null) {
                Map<String, String> javaClasses = getClasses(releaseCommit.getLastCommit());
                releaseCommit.setJavaClasses(javaClasses);
                releaseCommits.add(releaseCommit);
            }
            date = versionInfo.getDate();
        }

        return releaseCommits;
    }

    private Map<String, String> getClasses(RevCommit commit) throws IOException {

        Map<String, String> javaClasses = new HashMap<>();

        RevTree tree = commit.getTree();    //We get the tree of the files and the directories that were belong to the repository when commit was pushed
        TreeWalk treeWalk = new TreeWalk(this.repo);    //We use a TreeWalk to iterate over all files in the Tree recursively
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while(treeWalk.next()) {
            //We are keeping only Java classes that are not involved in tests
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                //We are retrieving (name class, content class) couples
                javaClasses.put(treeWalk.getPathString(), new String(this.repo.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8));
            }
        }
        treeWalk.close();

        return javaClasses;
    }

    private void retrieveChanges(List<RevCommit> commits) {
        for(RevCommit commit: commits) {
            Map<String, String> classMap = retrieveChanges(commit);
        }
    }


    private Map<String, String> retrieveChanges(RevCommit commit) {
        Map<String, String> classMap = new HashMap<>();
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
                classMap.put(entry.getNewPath(), new String(this.repo.open(entry.getNewId().toObjectId()).getBytes(), StandardCharsets.UTF_8));
                System.out.println(entry.getNewPath() + " " + entry.getChangeType());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return classMap;
    }


}
