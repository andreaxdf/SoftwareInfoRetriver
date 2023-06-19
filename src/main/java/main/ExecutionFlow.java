package main;

import enums.FilenamesEnum;
import model.ClassifierEvaluation;
import model.ReleaseInfo;
import model.Ticket;
import org.jetbrains.annotations.NotNull;
import retrivers.*;
import utils.TicketUtils;
import view.FileCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ExecutionFlow {
    private static final Logger logger = Logger.getLogger(ExecutionFlow.class.getName());



    private ExecutionFlow() {}

    public static void collectData(String projName) throws Exception {

        TicketRetriever ticketRetriever = new TicketRetriever(projName);
        CommitRetriever commitRetriever = ticketRetriever.getCommitRetriever();
        VersionRetriever versionRetriever = ticketRetriever.getVersionRetriever();

        //Retrieve of all project tickets that are valid ticket.
        List<Ticket> tickets = ticketRetriever.getTickets();
        logger.info("Tickets retrieved.");

        //Retrieve the release information about commits, classes and metrics that involve the release.
        List<ReleaseInfo> allTheReleaseInfo = commitRetriever.getReleaseCommits(versionRetriever, commitRetriever.retrieveCommit());
        logger.info("Information about commits retrieved.");
        MetricsRetriever.computeMetrics(allTheReleaseInfo, tickets, commitRetriever, versionRetriever);
        logger.info("Metrics computed.");
        FileCreator.writeOnCsv(projName, allTheReleaseInfo, FilenamesEnum.METRICS, 0);
        logger.info("Csv file created.");

        //----------------------------------------------------------- WALK FORWARD -----------------------------------------------------------

        logger.info("Starting walk forward.");

        List<ReleaseInfo> releaseInfoListHalved = discardHalfReleases(allTheReleaseInfo);

        //Iterate starting by 1 so that the walk forward starts from using at least one training set.
        for(int i = 1; i < releaseInfoListHalved.size(); i++) {
            //Selection of the tickets opened until the i-th release.
            List<Ticket> ticketsUntilRelease = TicketUtils.getTicketsUntilRelease(tickets, i);

            //Non viene aggiornata la buggyness del testing set.
            MetricsRetriever.computeBuggyness(releaseInfoListHalved.subList(0, i), ticketsUntilRelease, commitRetriever, versionRetriever);

            FileCreator.writeOnArff(projName, releaseInfoListHalved.subList(0, i), FilenamesEnum.TRAINING, i);
            ArrayList<ReleaseInfo> testingRelease = new ArrayList<>();
            testingRelease.add(releaseInfoListHalved.get(i));
            FileCreator.writeOnArff(projName, testingRelease, FilenamesEnum.TESTING, i);
            logger.info(i + ") Iteration completed.");
        }
        logger.info("Arff file created.");
        logger.info("Starting Weka evaluation.");
        WekaInfoRetriever wekaInfoRetriever = new WekaInfoRetriever(projName, allTheReleaseInfo.size()/2);
        List<ClassifierEvaluation> classifierEvaluationList = wekaInfoRetriever.retrieveClassifiersEvaluation(projName);
        FileCreator.writeEvaluationDataOnCsv(projName, classifierEvaluationList);
        logger.info("Finished Weka evaluation.");

    }

    private static @NotNull List<ReleaseInfo> discardHalfReleases(@NotNull List<ReleaseInfo> releaseInfoList) {

        int n = releaseInfoList.size();

        releaseInfoList.sort((o1, o2) -> {
            Integer i1 = o1.getRelease().getIndex();
            Integer i2 = o2.getRelease().getIndex();
            return i1.compareTo(i2);
        });

        return releaseInfoList.subList(0, n/2+1);
    }

}