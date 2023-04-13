package retrivers;

import model.VersionInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import util.JSONUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class VersionRetriever {

    public ArrayList<VersionInfo> projVersions;

    public VersionRetriever(String projName) {
        //Fills the arraylist with releases dates and orders them
        //Ignores releases with missing dates
        try {
            getVersions(projName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getVersions(String projName) throws IOException {

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = JSONUtils.readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        this.projVersions = createVersionArray(versions);

        sortRelease(this.projVersions);

        setIndex(this.projVersions);
    }

    private void setIndex(@NotNull ArrayList<VersionInfo> versions) {
        int i = 0;
        for(VersionInfo versionInfo : versions) {
            versionInfo.setIndex(i);
            i++;
        }
    }

    /**Get versions info from issues
    * */
    public ArrayList<VersionInfo> getAffectedVersions(@NotNull JSONArray versions) {
        String id;
        ArrayList<VersionInfo> affectedVersions = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++ ) {
            if(versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("id")) {
                    id = versions.getJSONObject(i).get("id").toString();
                    VersionInfo v = searchVersion(id);
                    if(v == null) throw new RuntimeException(); //Create a new exception or ignore the case with v == null
                    affectedVersions.add(v);
                }
            }
        }
        return affectedVersions;
    }

    private @Nullable VersionInfo searchVersion(String id) {
        for(VersionInfo version: this.projVersions) {
            if(Objects.equals(version.getId(), id)) {
                return version;
            }
        }
        return null;
    }

    private @NotNull ArrayList<VersionInfo> createVersionArray(@NotNull JSONArray versions) {
        ArrayList<VersionInfo> versionList = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++ ) {
            String name = "";
            String id = "";
            if(versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(), name, id, versionList);
            }
        }

        System.out.println("Versions: " + versionList.size());
        return versionList;
    }

    private void sortRelease(@NotNull ArrayList<VersionInfo> releases) {
        releases.sort(Comparator.comparing(VersionInfo::getDate));
    }

    public void addRelease(String strDate, String name, String id, @NotNull ArrayList<VersionInfo> releases) {
        LocalDate date = LocalDate.parse(strDate);
        VersionInfo newRelease = new VersionInfo(id, name, date);
        releases.add(newRelease);
        //System.out.println("Version: " + newRelease.getName());
    }


}