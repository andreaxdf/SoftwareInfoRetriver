package utils;

import model.ChangedJavaClass;
import model.JavaClass;
import model.ReleaseInfo;
import model.Version;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import retrivers.CommitRetriever;
import retrivers.VersionRetriever;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaClassUtil {

    private JavaClassUtil() {}

    public static void updateJavaBuggyness(ChangedJavaClass className, @NotNull List<ReleaseInfo> releaseInfoList, List<Version> affectedReleases) {

        for(ReleaseInfo rc: releaseInfoList) {
            if(affectedReleases.contains(rc.getRelease())) { //Get the affected release and update the buggyness of the java class
                List<JavaClass> javaClasses = rc.getJavaClasses(); //Get the java classes of the release
                findClassAndSetBuggyness(className, javaClasses);
            }
        }
    }

    /**
     * This method find the modified class into the class of the release, set its buggyness to true and associate the commit to the class.
     *
     * @param className   the name of the searched class
     * @param javaClasses the list of java classes in the release
     */
    private static void findClassAndSetBuggyness(ChangedJavaClass className, @NotNull List<JavaClass> javaClasses) {
        for(JavaClass javaClass: javaClasses) {
            if(Objects.equals(javaClass.getName(), className.getJavaClassName())) {
                javaClass.getMetrics().setClassBuggyness(true);
                return;
            }
        }
    }

    public static void updateNumberOfFixedDefects(VersionRetriever versionRetriever, @NotNull List<RevCommit> commits, List<ReleaseInfo> releaseInfoList, CommitRetriever commitRetriever) {

        for(RevCommit commit: commits){
            List<ChangedJavaClass> classChangedList = commitRetriever.retrieveChanges(commit);
            ReleaseInfo releaseInfo = VersionUtil.retrieveCommitRelease(
                    versionRetriever,
                    GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()),
                    releaseInfoList);

            if (releaseInfo != null) {

                for (ChangedJavaClass javaClass : classChangedList) {
                    updateFixedDefects(releaseInfo, javaClass.getJavaClassName());
                }
            }
        }
    }

    private static void updateFixedDefects(@NotNull ReleaseInfo releaseInfo, String className) {

        for(JavaClass javaClass: releaseInfo.getJavaClasses()) {
            if(Objects.equals(javaClass.getName(), className)) {
                javaClass.getMetrics().updateFixedDefects();

                return;
            }
        }
    }

    public static void updateJavaClassCommits(CommitRetriever commitRetriever, @NotNull List<RevCommit> commits, List<JavaClass> javaClasses) {

        for(RevCommit commit: commits) {
            List<ChangedJavaClass> changedJavaClassList = commitRetriever.retrieveChanges(commit);

            for(ChangedJavaClass changedJavaClass: changedJavaClassList) {
                for(JavaClass javaClass: javaClasses) {
                    if (Objects.equals(changedJavaClass.getJavaClassName(), javaClass.getName())) {
                        javaClass.addCommit(commit);
                        break;
                    }
                }
            }
        }
    }

    public static @NotNull List<ChangedJavaClass> createChangedJavaClass(@NotNull List<JavaClass> javaClasses) {
        List<ChangedJavaClass> changedJavaClassList = new ArrayList<>();

        for(JavaClass javaClass: javaClasses) {
            changedJavaClassList.add(new ChangedJavaClass(
                    javaClass.getName()
            ));
        }

        return changedJavaClassList;
    }
}
