package retrivers;

import model.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import util.GitUtils;

import java.util.ArrayList;

public class CommitRetriever {

    Git git;
    Repository repo;

    public CommitRetriever(String repositoryPath) {
        this.repo = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repo);
    }

    /*public void retrieveReleaseInfoForTickets(ArrayList<Ticket> tickets) {
        try {
            Iterable<RevCommit> commitIterable = git.log().call();

            ArrayList<RevCommit> commits = new ArrayList<>();
            for(RevCommit commit: commitIterable) {
                commits.add(commit);
            }

            for(Ticket ticket: tickets) {
                //TODO prendo il commit giusto? Data OpeningVersion > data FixVersion
                RevCommit commit = retrieveCommit(commits, ticket);
                if(commit != null) setReleaseInfoInTicket(commit, ticket);
            }

        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }*/

    public ArrayList<RevCommit> retrieveAssociatedCommit(@NotNull ArrayList<RevCommit> commits, Ticket ticket) {
        ArrayList<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits) {
            if(commit.getFullMessage().contains(ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

    public ArrayList<RevCommit> retrieveCommit() throws GitAPIException {
        Iterable<RevCommit> commitIterable = git.log().call();

        ArrayList<RevCommit> commits = new ArrayList<>();
        for(RevCommit commit: commitIterable) {
            commits.add(commit);
        }

        return commits;
    }

    /** Associate the tickets with the commits that reference them. Moreover, discard the tickets that don't have any commits.*/
    public ArrayList<Ticket> associateTicketAndCommit(CommitRetriever commitRetriever, ArrayList<Ticket> tickets) {
        try {
            ArrayList<RevCommit> commits = commitRetriever.retrieveCommit();
            for (Ticket ticket : tickets) {
                ArrayList<RevCommit> associatedCommits = commitRetriever.retrieveAssociatedCommit(commits, ticket);
                ticket.setAssociatedCommits(associatedCommits);
                //GitUtils.printCommit(associatedCommits);
            }
            tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return tickets;
    }

}
