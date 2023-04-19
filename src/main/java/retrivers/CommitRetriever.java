package retrivers;

import model.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jetbrains.annotations.NotNull;
import util.GitUtils;
import util.RegularExpression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommitRetriever {

    Git git;
    Repository repo;

    public CommitRetriever(String repositoryPath) {
        this.repo = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repo);
    }

    private List<RevCommit> retrieveAssociatedCommit(@NotNull List<RevCommit> commits, Ticket ticket) {
        List<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits) {
            if(RegularExpression.matchRegex(commit.getFullMessage(), ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

    public List<RevCommit> retrieveCommit() throws GitAPIException {
        Iterable<RevCommit> commitIterable = git.log().call();

        List<RevCommit> commits = new ArrayList<>();
        for(RevCommit commit: commitIterable) {
            commits.add(commit);
        }

        return commits;
    }

    /** Associate the tickets with the commits that reference them. Moreover, discard the tickets that don't have any commits.*/
    public List<Ticket> associateTicketAndCommit(CommitRetriever commitRetriever, List<Ticket> tickets) {
        try {
            List<RevCommit> commits = commitRetriever.retrieveCommit();
            for (Ticket ticket : tickets) {
                List<RevCommit> associatedCommits = commitRetriever.retrieveAssociatedCommit(commits, ticket);
                ticket.setAssociatedCommits(associatedCommits);
                //GitUtils.printCommit(associatedCommits);
            }
            tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return tickets;
    }

    public void retrieveChangesFromTickets(List<Ticket> tickets) {
        for(Ticket ticket: tickets) {
            retrieveChanges(ticket.getAssociatedCommits());
        }
    }

    private void retrieveChanges(List<RevCommit> commits) {
        for(RevCommit commit: commits) {
            retrieveChanges(commit);
        }
    }

    public void retrieveChanges(RevCommit commit) {
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
                System.out.println(entry.getNewPath() + " " + entry.getChangeType());

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
