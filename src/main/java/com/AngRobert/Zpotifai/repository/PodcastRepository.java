package com.AngRobert.Zpotifai.repository;

import com.AngRobert.Zpotifai.model.Podcast;
import com.AngRobert.Zpotifai.util.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.ArrayList;

public class PodcastRepository extends BaseRepository<Podcast> implements SearchableRepository<Podcast>{

    @Override
    protected Podcast mapRow(ResultSet rs) throws SQLException {
        Podcast p = new Podcast();
        p.setPodcast_id(rs.getInt("podcast_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setLength(rs.getInt("length"));
        p.setRelease_date(rs.getDate("release_date").toLocalDate());
        p.setStreams(rs.getInt("streams"));

        List<String> hosts = getRelatedNames(
                "SELECT C.name FROM CREATORS C JOIN PODCAST_HOSTS PH ON C.creator_id = PH.creator_id WHERE PH.podcast_id = ?",
                p.getId()
        );
        if (!hosts.isEmpty()) {
            p.setCreatorDisplayName(String.join(", ", hosts));
        }

        return p;
    }

    @Override
    protected String getTableName() {
        return "Podcasts";
    }

    @Override
    protected String getIdColumnName() {
        return "podcast_id";
    }

    @Override
    public List<Podcast> searchByName(String name) {
        return searchByColumnName("name", name);
    }

    @Override
    public String getSearchDetails(int id) {
        String baseDetails = super.getSearchDetails(id, List.of("name", "description", "release_date", "length", "streams"));
        StringBuilder details = new StringBuilder(baseDetails);

        List<String> hosts = getRelatedNames(
                "SELECT C.name FROM CREATORS C JOIN PODCAST_HOSTS PH ON C.creator_id = PH.creator_id WHERE PH.podcast_id = ?",
                id
        );
        details.append("Hosts: ").append(hosts.isEmpty() ? "None" : String.join(", ", hosts)).append("\n");

        return details.toString();
    }

    @Override
    public String getCategoryName() {
        return "Podcasts";
    }

    public int getIdByNameAndHost(String podcastName, int hostId) {
        String sql = "SELECT P.podcast_id FROM PODCASTS P " +
                "JOIN PODCAST_HOSTS PH ON P.podcast_id = PH.podcast_id " +
                "WHERE LOWER(P.name) = LOWER(?) AND PH.creator_id = ?";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setString(1, podcastName);
            stmt.setInt(2, hostId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error finding podcast by name and host: " + e.getMessage());
        }
        return -1;
    }

    public boolean checkDuplicates(String name, int hostId) {
        return getIdByNameAndHost(name, hostId) != -1;
    }

    public List<Integer> getPodcastIdsByHostId(int hostId) {
        String sql = "SELECT podcast_id FROM PODCAST_HOSTS WHERE creator_id = ?";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, hostId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            System.out.println("Error fetching podcast IDs for host: " + e.getMessage());
        }
        return ids;
    }
}
