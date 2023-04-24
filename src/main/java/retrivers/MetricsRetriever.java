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

import java.io.IOException;
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

    public static void computeMetrics(List<ReleaseCommits> releaseCommitsList, CommitRetriever commitRetriever) {

        addSizeLabel(releaseCommitsList);
        try {
            computeLocData(releaseCommitsList, commitRetriever);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static void computeLocData(List<ReleaseCommits> releaseCommitsList, CommitRetriever commitRetriever) throws IOException {
        for(ReleaseCommits rc: releaseCommitsList) {
            for (JavaClass javaClass : rc.getJavaClasses()) {
                commitRetriever.computeAddedAndDeletedLinesList(javaClass);
                computeLocAndChurnMetrics(javaClass);
            }
        }
    }

    private static void computeLocAndChurnMetrics(JavaClass javaClass) {

        int sumLOC = 0;
        int maxLOC = 0;
        double avgLOC = 0;
        int churn = 0;
        int maxChurn = 0;
        double avgChurn = 0;

        for(int i=0; i<javaClass.getMetrics().getAddedLinesList().size(); i++) {

            int currentLOC = javaClass.getMetrics().getAddedLinesList().get(i);
            int currentDiff = Math.abs(javaClass.getMetrics().getAddedLinesList().get(i) - javaClass.getMetrics().getDeletedLinesList().get(i));

            sumLOC = sumLOC + currentLOC;
            churn = churn + currentDiff;

            if(currentLOC > maxLOC) {
                maxLOC = currentLOC;
            }
            if(currentDiff > maxChurn) {
                maxChurn = currentDiff;
            }

        }

        //If a class has 0 revisions, its AvgLocAdded and AvgChurn are 0 (see initialization above).
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty()) {
            avgLOC = 1.0*sumLOC/javaClass.getMetrics().getAddedLinesList().size();
        }
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty()) {
            avgChurn = 1.0*churn/javaClass.getMetrics().getAddedLinesList().size();
        }

        javaClass.getMetrics().setLocAdded(sumLOC);
        javaClass.getMetrics().setMaxLocAdded(maxLOC);
        javaClass.getMetrics().setAvgLocAdded(avgLOC);
        javaClass.getMetrics().setChurn(churn);
        javaClass.getMetrics().setMaxChurn(maxChurn);
        javaClass.getMetrics().setAvgChurn(avgChurn);

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