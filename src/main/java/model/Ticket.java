package model;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import retrivers.VersionRetriever;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Ticket {

    String key;
    LocalDate creationDate;
    LocalDate resolutionDate;
    List<VersionInfo> affectedReleases;
    VersionInfo openingRelease;
    VersionInfo fixedRelease;
    VersionInfo injectedRelease;
    VersionRetriever versionRetriever;
    List<RevCommit> associatedCommits;

    public Ticket(@NotNull String creationDate, @NotNull String resolutionDate, String key, List<VersionInfo> affectedReleases, @NotNull VersionRetriever versionRetriever) {
        this.creationDate = LocalDate.parse(creationDate.substring(0, 10));
        this.resolutionDate = LocalDate.parse(resolutionDate.substring(0, 10));
        this.key = key;
        setVersionRetriever(versionRetriever);
        setInjectedRelease(affectedReleases);
    }

    public VersionRetriever getVersionRetriever() {
        return versionRetriever;
    }

    public void setVersionRetriever(VersionRetriever versionRetriever) {
        if(versionRetriever == null) {
            throw new RuntimeException();
        }
        this.versionRetriever = versionRetriever;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setAssociatedCommits(List<RevCommit> associatedCommits) {
        this.associatedCommits = associatedCommits;
    }

    public LocalDate getResolutionDate() {
        return resolutionDate;
    }

    public String getKey() {
        return key;
    }

    public List<VersionInfo> getAffectedReleases() {
        return affectedReleases;
    }

    public VersionInfo getOpeningRelease() {
        return openingRelease;
    }

    public void setOpeningRelease(VersionInfo openingRelease) {
        this.openingRelease = openingRelease;
    }

    public VersionInfo getFixedRelease() {
        return fixedRelease;
    }

    public List<RevCommit> getAssociatedCommits() {
        return associatedCommits;
    }

    public void setFixedRelease(VersionInfo fixedRelease) {
        this.fixedRelease = fixedRelease;
        computeAffectedRelease();
    }

    public VersionInfo getInjectedRelease() {
        return injectedRelease;
    }

    public void setInjectedRelease(VersionInfo release) {
        this.injectedRelease = release;
        computeAffectedRelease();
    }

    private void setInjectedRelease(List<VersionInfo> affectedReleases) {
        if(!affectedReleases.isEmpty()) {
            this.injectedRelease = affectedReleases.get(0);
            computeAffectedRelease();
        } else {
            this.injectedRelease = null;
        }
    }

    public void computeAffectedRelease() {
        // Execute the method only if the ticket has fixed and injected release
        if(this.injectedRelease == null || this.fixedRelease == null) return;

        List<VersionInfo> releases = new ArrayList<>();
        for (VersionInfo versionInfo : versionRetriever.getProjVersions()) {
            if ((versionInfo.getIndex() >= this.injectedRelease.getIndex()) && (versionInfo.getIndex() < this.fixedRelease.getIndex())) {
                releases.add(versionInfo);
            }
        }

        this.affectedReleases = releases;
    }
}
