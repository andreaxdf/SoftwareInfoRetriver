package util;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class GitUtils {

    private GitUtils() {}

    public static LocalDate castToLocalDate(Date date) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return LocalDate.parse(dateFormatter.format(date));
    }

    public static Repository getRepository(String repoPath) {
        try {
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            return repositoryBuilder.setGitDir(new File(repoPath + "/.git")).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void printCommit(List<RevCommit> commits) {
        for(RevCommit commit: commits) {
            System.out.println("Commit: " + commit.getAuthorIdent().getName());
            System.out.println(commit.getShortMessage());
        }
    }
}
