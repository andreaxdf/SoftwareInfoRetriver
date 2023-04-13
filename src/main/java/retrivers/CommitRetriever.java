package retrivers;

import model.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public @Nullable ArrayList<RevCommit> retrieveCommit(Ticket ticket) throws GitAPIException {
        Iterable<RevCommit> commitIterable = git.log().call();

        ArrayList<RevCommit> commits = new ArrayList<>();
        for(RevCommit commit: commitIterable) {
            commits.add(commit);
        }

        ArrayList<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits) {
            if(commit.getFullMessage().contains(ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

}
