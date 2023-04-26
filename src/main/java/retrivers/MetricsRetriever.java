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

    private MetricsRetriever() {}

    /**
     * This method set the buggyness to true of all classes that have been modified by fix commits of tickets and compute the number of fixed defects in each class for each version.
     * @param releaseCommitsList The list of the project ReleaseCommits.
     * @param tickets The list of the project tickets.
     * @param commitRetriever Project commitRetriever.
     * @param versionRetriever Project versionRetriever.
     */
    public static void computeBuggynessAndFixedDefects(List<ReleaseCommits> releaseCommitsList, @NotNull List<Ticket> tickets, CommitRetriever commitRetriever, VersionRetriever versionRetriever) {

        for(Ticket ticket: tickets){
            for (RevCommit commit : ticket.getAssociatedCommits()) {
                //For each commit associated to a ticket, set all classes touched in commit as buggy in all the affected versions of the ticket.
                ReleaseCommits releaseCommits = VersionUtil.retrieveCommitRelease(
                        versionRetriever,
                        GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()),
                        releaseCommitsList);

                if (releaseCommits != null) {
                    List<ChangedJavaClass> classChangedList = commitRetriever.retrieveChanges(commit);

                    for (ChangedJavaClass javaClass : classChangedList) {
                        JavaClassUtil.updateJavaBuggyness(javaClass, releaseCommitsList, ticket.getAffectedReleases());
                    }
                }
            }

            //For each ticket, update the number of fixed defects of classes present in the last commit of the ticket (the fixed commit).
            List<ChangedJavaClass> classChangedList = commitRetriever.retrieveChanges(ticket.getLastCommit());
            JavaClassUtil.updateNumberOfFixedDefects(versionRetriever, ticket.getLastCommit(), classChangedList, releaseCommitsList);
        }
    }

    public static void computeMetrics(List<ReleaseCommits> releaseCommitsList, CommitRetriever commitRetriever) {

        //Add the size metric in all the classes of the release.
        addSizeLabel(releaseCommitsList);
        try {
            computeLocData(releaseCommitsList, commitRetriever);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static void computeLocData(@NotNull List<ReleaseCommits> releaseCommitsList, CommitRetriever commitRetriever) throws IOException {
        for(ReleaseCommits rc: releaseCommitsList) {
            for (JavaClass javaClass : rc.getJavaClasses()) {
                commitRetriever.computeAddedAndDeletedLinesList(javaClass);
                computeLocAndChurnMetrics(javaClass);
            }
        }
    }

    private static void computeLocAndChurnMetrics(@NotNull JavaClass javaClass) {

        int sumLOC = 0;
        int maxLOC = 0;
        double avgLOC = 0;
        int churn = 0;
        int maxChurn = 0;
        double avgChurn = 0;
        int sumOfTheDeletedLOC = 0;
        int maxDeletedLOC = 0;
        double avgDeletedLOC = 0;

        for(int i=0; i<javaClass.getMetrics().getAddedLinesList().size(); i++) {

            int currentLOC = javaClass.getMetrics().getAddedLinesList().get(i);
            int currentDeletedLOC = javaClass.getMetrics().getDeletedLinesList().get(i);
            int currentDiff = Math.abs(currentLOC - currentDeletedLOC);

            sumLOC = sumLOC + currentLOC;
            churn = churn + currentDiff;
            sumOfTheDeletedLOC = sumOfTheDeletedLOC + currentDeletedLOC;

            if(currentLOC > maxLOC) {
                maxLOC = currentLOC;
            }
            if(currentDiff > maxChurn) {
                maxChurn = currentDiff;
            }
            if(currentDeletedLOC > maxDeletedLOC) {
                maxDeletedLOC = currentDeletedLOC;
            }

        }

        //If a class has 0 revisions, its AvgLocAdded and AvgChurn are 0 (see initialization above).
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty()) {
            avgLOC = 1.0*sumLOC/javaClass.getMetrics().getAddedLinesList().size();
        }
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty() || !javaClass.getMetrics().getDeletedLinesList().isEmpty()) {
            avgChurn = 1.0*churn/(javaClass.getMetrics().getAddedLinesList().size() + javaClass.getMetrics().getDeletedLinesList().size());
        }
        if(!javaClass.getMetrics().getDeletedLinesList().isEmpty()) {
            avgDeletedLOC = 1.0*sumOfTheDeletedLOC/javaClass.getMetrics().getDeletedLinesList().size();
        }

        javaClass.getMetrics().setLocAdded(sumLOC);
        javaClass.getMetrics().setMaxLocAdded(maxLOC);
        javaClass.getMetrics().setAvgLocAdded(avgLOC);
        javaClass.getMetrics().setChurn(churn);
        javaClass.getMetrics().setMaxChurn(maxChurn);
        javaClass.getMetrics().setAvgChurn(avgChurn);
        javaClass.getMetrics().setLocDeleted(sumOfTheDeletedLOC);
        javaClass.getMetrics().setMaxLocDeleted(maxDeletedLOC);
        javaClass.getMetrics().setAvgLocDeleted(avgDeletedLOC);
    }

    public static void addSizeLabel(@NotNull List<ReleaseCommits> releaseCommitsList) {

        for(ReleaseCommits rc: releaseCommitsList) {
            for(JavaClass javaClass: rc.getJavaClasses()) {
                String[] lines = javaClass.getContent().split("\r\n|\r|\n");
                javaClass.getMetrics().setSize(lines.length);
            }
        }
    }
}