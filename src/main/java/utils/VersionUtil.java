package utils;

import model.ReleaseInfo;
import model.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrivers.VersionRetriever;

import java.time.LocalDate;
import java.util.List;

public class VersionUtil {

    private VersionUtil() {}

    public static @Nullable Version retrieveNextRelease(VersionRetriever versionRetriever, LocalDate date) {
        for(Version version : versionRetriever.getProjVersions()) {
            LocalDate releaseDate = version.getDate();
            if(!releaseDate.isBefore(date)) {
                return version;
            }
        }
        return null;
    }

    /**
     * Retrieve the ReleaseCommits in the rcList with version immediately after the parameter date.
     * @param versionRetriever Project version retriever
     * @param date Commit date; we use it for retrieve the version of the commit.
     * @param rcList List of the project ReleaseCommits
     * @return The ReleaseCommits with version immediately after the parameter date
     */
    public static @Nullable ReleaseInfo retrieveCommitRelease(VersionRetriever versionRetriever, LocalDate date, @NotNull List<ReleaseInfo> rcList) {
        Version version = retrieveNextRelease(versionRetriever, date);

        for(ReleaseInfo rc: rcList) {
            if(rc.getRelease() == version) return rc;
        }

        return null;
    }
}
