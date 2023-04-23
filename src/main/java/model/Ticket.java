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
    List<Version> affectedReleases;
    Version openingRelease;
    Version fixedRelease;
    Version injectedRelease;
    VersionRetriever versionRetriever;
    List<RevCommit> associatedCommits;

    public Ticket(@NotNull String creationDate, @NotNull String resolutionDate, String key, List<Version> affectedReleases, @NotNull VersionRetriever versionRetriever) {
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

    public List<Version> getAffectedReleases() {
        return affectedReleases;
    }

    public Version getOpeningRelease() {
        return openingRelease;
    }

    public void setOpeningRelease(Version openingRelease) {
        this.openingRelease = openingRelease;
    }

    public Version getFixedRelease() {
        return fixedRelease;
    }

    public List<RevCommit> getAssociatedCommits() {
        return associatedCommits;
    }

    public void setFixedRelease(Version fixedRelease) {
        this.fixedRelease = fixedRelease;
        computeAffectedRelease();
    }

    public Version getInjectedRelease() {
        return injectedRelease;
    }

    public void setInjectedRelease(Version release) {
        this.injectedRelease = release;
        computeAffectedRelease();
    }

    private void setInjectedRelease(List<Version> affectedReleases) {
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

        List<Version> releases = new ArrayList<>();
        for (Version version : versionRetriever.getProjVersions()) {
            if ((version.getIndex() >= this.injectedRelease.getIndex()) && (version.getIndex() < this.fixedRelease.getIndex())) {
                releases.add(version);
            }
        }

        this.affectedReleases = releases;
    }
}
