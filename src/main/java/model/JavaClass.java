package model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class JavaClass {

    private final String name;
    private final String content;
    private final Version release;
    private final List<RevCommit> commits = new ArrayList<>();

    private Metrics metrics = new Metrics();

    public JavaClass(String name, String content, Version release) {
        this.name = name;
        this.content = content;
        this.release = release;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public List<RevCommit> getCommits() {
        return commits;
    }

    public void addCommit(RevCommit commit) {
        this.commits.add(commit);
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }
}
