package util;

import model.ReleaseCommits;
import model.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrivers.VersionRetriever;

import java.time.LocalDate;
import java.util.List;

public class VersionUtil {

    public static void printVersion(List<Version> versionList) {
        for(Version version: versionList) {
            System.out.println("Version: "
                    + version.getId() + ", "
                    + version.getName() + ", "
                    + version.getDate());
        }
    }

    public static @Nullable Version retrieveNextRelease(VersionRetriever versionRetriever, LocalDate date) {
        for(Version version : versionRetriever.getProjVersions()) {
            LocalDate releaseDate = version.getDate();
            if(!releaseDate.isBefore(date)) {
                return version;
            }
        }
        return null;
    }

    public static @Nullable ReleaseCommits retrieveCommitRelease(VersionRetriever versionRetriever, LocalDate date, @NotNull List<ReleaseCommits> rcList) {
        Version version = retrieveNextRelease(versionRetriever, date);

        for(ReleaseCommits rc: rcList) {
            if(rc.getRelease() == version) return rc;
        }

        return null;
    }
}
