package view;

import enums.CsvNamesEnum;
import model.JavaClass;
import model.ReleaseCommits;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
public class FileCreator {



    private FileCreator() {}

    private static String enumToString(@NotNull CsvNamesEnum csvEnum, int csvIndex) {

        return switch (csvEnum) {
            case TRAINING -> "_TR" + csvIndex;
            case TESTING -> "_TE" + csvIndex;
            case BUGGY -> "_buggy_classes";
            case CURRENT -> "_current_classes";
        };

    }

    public static void writeOnCsv(String projName, List<ReleaseCommits> rcList, CsvNamesEnum csvEnum, int csvIndex) throws IOException {

        String csvNameStr = enumToString(csvEnum, csvIndex);

        String pathname = projName + csvNameStr + ".csv";

        File file = new File(pathname);

        if(file.exists() && !file.delete()) {
            throw new IOException(); //Exception: file deletion impossible
        }

        try(FileWriter fw = new FileWriter(pathname)) {

            fw.write("VERSION," +
                    "JAVA_CLASS," +
                    "SIZE," +
                    "LOC_ADDED," +
                    "MAX_LOC_ADDED," +
                    "AVG_LOC_ADDED," +
                    "LOC_DELETED," +
                    "MAX_LOC_DELETED," +
                    "AVG_LOC_DELETED," +
                    "CHURN," +
                    "MAX_CHURN," +
                    "AVG_CHURN," +
                    "FIXED_DEFECTS," +
                    "NUMBER_OF_COMMITS," +
                    "NUMBER_OF_AUTHORS," +
                    "IS_BUGGY,");

            int count;

            for(ReleaseCommits rc: rcList) {
                count = 0;
                for(JavaClass javaClass: rc.getJavaClasses()) {
                    fw.write("\n");

                    fw.write(rc.getRelease().getIndex() + ","); //VERSION
                    fw.write(javaClass.getName() + ","); //JAVA_CLASS
                    fw.write(javaClass.getMetrics().getSize() + ","); //SIZE
                    fw.write(javaClass.getMetrics().getLocAdded() + ","); //LOC_ADDED
                    fw.write(javaClass.getMetrics().getMaxLocAdded() + ","); //MAX_LOC_ADDED
                    fw.write(javaClass.getMetrics().getAvgLocAdded() + ","); //AVG_LOC_ADDED
                    fw.write(javaClass.getMetrics().getLocDeleted() + ","); //LOC_DELETED
                    fw.write(javaClass.getMetrics().getMaxLocDeleted() + ","); //MAX_LOC_DELETED
                    fw.write(javaClass.getMetrics().getAvgLocDeleted() + ","); //AVG_LOC_DELETED
                    fw.write(javaClass.getMetrics().getChurn() + ","); //CHURN
                    fw.write(javaClass.getMetrics().getMaxChurn() + ","); //MAX_CHURN
                    fw.write(javaClass.getMetrics().getAvgChurn() + ","); //AVG_CHURN
                    fw.write(javaClass.getMetrics().getFixedDefects() + ","); //FIXED_DEFECTS
                    fw.write(javaClass.getCommits().size() + ","); //NUMBER_OF_COMMITS
                    fw.write(javaClass.getMetrics().getnAuth() + ","); //NUMBER_OF_AUTHORS
                    fw.write(javaClass.getMetrics().isBuggy()); //IS_BUGGY

                    if(javaClass.getMetrics().isBuggyness()) {
                        count++;
                    }
                }

                rc.setBuggyClasses(count);
            }
        }
    }
}
