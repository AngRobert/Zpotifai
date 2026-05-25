package com.AngRobert.Zpotifai.repository;

import com.AngRobert.Zpotifai.model.Single;
import com.AngRobert.Zpotifai.util.DBConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class SingleRepository extends BaseRepository<Single> implements SearchableRepository<Single>{

    @Override
    public int add(List<String> columns, List<Object> values) {
        return addWithChild("SINGLES", List.of(), columns, values);
    }

    @Override
    public int update(int id, List<String> columns, List<Object> values) {
        return updateWithChild("SINGLES", List.of(), id, columns, values);
    }

    @Override
    protected Single mapRow(ResultSet rs) throws SQLException {
        Single s = new Single();
        s.setSong_id(rs.getInt("song_id"));
        s.setName(rs.getString("name"));
        s.setLength(rs.getInt("length"));
        s.setRelease_date(rs.getDate("release_date").toLocalDate());
        s.setStreams(rs.getInt("streams"));

        List<String> artists = getRelatedNames(
                "SELECT C.name FROM CREATORS C JOIN SONG_ARTISTS SA ON C.creator_id = SA.creator_id WHERE SA.song_id = ?",
                s.getId()
        );
        if (!artists.isEmpty()) {
            s.setCreatorDisplayName(String.join(", ", artists));
        }

        return s;
    }

    @Override
    protected String getTableName() {
        return "SINGLES JOIN SONGS ON SINGLES.song_id = SONGS.song_id";
    }

    @Override
    protected String getBaseTableName() {
        return "SONGS";
    }

    @Override
    protected String getIdColumnName() {
        return "song_id";
    }

    @Override
    public List<Single> searchByName(String name) {
        return searchByColumnName("name", name);
    }

    @Override
    public String getSearchDetails(int id) {
        String baseDetails = super.getSearchDetails(id, List.of("name", "length", "streams", "release_date"));
        StringBuilder details = new StringBuilder(baseDetails);

        List<String> artists = getRelatedNames(
                "SELECT C.name FROM CREATORS C JOIN SONG_ARTISTS SA ON C.creator_id = SA.creator_id WHERE SA.song_id = ?",
                id
        );
        details.append("Artists: ").append(artists.isEmpty() ? "None" : String.join(", ", artists)).append("\n");

        List<String> collaborators = getRelatedNames(
                "SELECT C.name FROM COLLABORATORS C JOIN SONG_COLLABORATIONS SC ON C.collaborator_id = SC.collaborator_id WHERE SC.song_id = ?",
                id
        );
        details.append("Collaborators: ").append(collaborators.isEmpty() ? "None" : String.join(", ", collaborators)).append("\n");

        return details.toString();
    }

    @Override
    public String getCategoryName() {
        return "Singles";
    }

    public int getIdByNameAndArtist(String songName, int artistId) {
        String sql = "SELECT S.song_id FROM SONGS S " +
                "JOIN SONG_ARTISTS SA ON S.song_id = SA.song_id " +
                "WHERE LOWER(S.name) = LOWER(?) AND SA.creator_id = ?";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setString(1, songName);
            stmt.setInt(2, artistId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error finding single by name and artist: " + e.getMessage());
        }
        return -1;
    }

    public List<Integer> getSongIdsByArtistId(int artistId) {
        String sql = "SELECT S.song_id FROM SONGS S " +
                "JOIN SONG_ARTISTS SA ON S.song_id = SA.song_id " +
                "JOIN SINGLES SI ON S.song_id = SI.song_id " +
                "WHERE SA.creator_id = ?";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, artistId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            System.out.println("Error fetching single IDs for artist: " + e.getMessage());
        }
        return ids;
    }
}
