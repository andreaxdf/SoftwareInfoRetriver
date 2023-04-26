package model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;


public class ReleaseCommits {

    private Version release;
    private List<RevCommit> commits;
    private RevCommit lastCommit;
    /**Classes that were present when the release was deployed; they are the classes presented in the last commit
     *
     */
    private List<JavaClass> javaClasses;
    private int buggyClasses;

    public ReleaseCommits(Version release, List<RevCommit> commits, RevCommit lastCommit) {
        this.release = release;
        this.commits = commits;
        this.lastCommit = lastCommit;
        this.javaClasses = null;
    }

    /**
     * @return the release
     */
    public Version getRelease() {
        return release;
    }

    /**
     * @param release the release to set
     */
    public void setRelease(Version release) {
        this.release = release;
    }

    /**
     * @return the commits
     */
    public List<RevCommit> getCommits() {
        return commits;
    }

    /**
     * @param commits the commits to set
     */
    public void setCommits(List<RevCommit> commits) {
        this.commits = commits;
    }

    /**
     * @return the lastCommit
     */
    public RevCommit getLastCommit() {
        return lastCommit;
    }

    /**
     * @param lastCommit the lastCommit to set
     */
    public void setLastCommit(RevCommit lastCommit) {
        this.lastCommit = lastCommit;
    }

    /**
     * @return the javaClasses
     */
    public List<JavaClass> getJavaClasses() {
        return javaClasses;
    }

    /**
     * @param javaClasses the javaClasses to set
     */
    public void setJavaClasses(List<JavaClass> javaClasses) {
        this.javaClasses = javaClasses;
    }

    public int getBuggyClasses() {
        return buggyClasses;
    }

    public void setBuggyClasses(int buggyClasses) {
        this.buggyClasses = buggyClasses;
    }
}
