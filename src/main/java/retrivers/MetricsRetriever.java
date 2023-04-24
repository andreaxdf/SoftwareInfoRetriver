package retrivers;

import model.ChangedJavaClass;
import model.JavaClass;
import model.ReleaseCommits;
import model.Ticket;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import util.GitUtils;
import util.JavaClassUtil;
import util.VersionUtil;

import java.util.List;

public class MetricsRetriever {

    /**
     * This method set the buggyness to true of all classes that have been modified by fix commits of tickets.
     * @param releaseCommitsList The list of the project ReleaseCommits.
     * @param tickets The list of the project tickets.
     * @param commitRetriever Project commitRetriever.
     * @param versionRetriever Project versionRetriever.
     */
    public static void addBuggynessLabel(List<ReleaseCommits> releaseCommitsList, @NotNull List<Ticket> tickets, CommitRetriever commitRetriever, VersionRetriever versionRetriever) {

        for(Ticket ticket: tickets){
            for (RevCommit commit : ticket.getAssociatedCommits()) {
                ReleaseCommits releaseCommits = VersionUtil.retrieveCommitRelease(
                        versionRetriever,
                        GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()),
                        releaseCommitsList);

                if (releaseCommits != null) {
                    List<ChangedJavaClass> classChangedList = commitRetriever.retrieveChanges(commit);

                    for (ChangedJavaClass javaClass : classChangedList) {
                        JavaClassUtil.updateJavaBuggyness(javaClass, releaseCommitsList, ticket.getAffectedReleases(), commit);
                    }
                }


            }
        }
    }

    public static void computeMetrics(List<ReleaseCommits> releaseCommitsList) {

        addSizeLabel(releaseCommitsList);

    }

    public static void addSizeLabel(List<ReleaseCommits> releaseCommitsList) {

        for(ReleaseCommits rc: releaseCommitsList) {
            for(JavaClass javaClass: rc.getJavaClasses()) {
                String[] lines = javaClass.getContent().split("\r\n|\r|\n");
                javaClass.getMetrics().setSize(lines.length);
            }
        }
    }
}
