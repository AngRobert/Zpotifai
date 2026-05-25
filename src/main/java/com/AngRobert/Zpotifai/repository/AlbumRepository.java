package com.AngRobert.Zpotifai.repository;

import com.AngRobert.Zpotifai.model.Album;
import com.AngRobert.Zpotifai.util.DBConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class AlbumRepository extends BaseRepository<Album> implements SearchableRepository<Album> {

    @Override
    protected Album mapRow(ResultSet rs) throws SQLException {
        Album a = new Album();
        a.setAlbum_id(rs.getInt("album_id"));
        a.setName(rs.getString("name"));
        a.setRelease_date(rs.getDate("release_date").toLocalDate());

        // get primary artist for display purposes
        List<String> artists = getRelatedNames(
                "SELECT C.name FROM CREATORS C JOIN ALBUM_ARTISTS AA ON C.creator_id = AA.creator_id " +
                        "WHERE AA.album_id = ?", a.getId());
        if (!artists.isEmpty()) {
            a.setCreatorDisplayName(String.join(", ", artists));
        }

        return a;
    }

    @Override
    protected String getTableName() {
        return "Albums";
    }

    @Override
    public String getCategoryName() {
        return "Albums";
    }

    @Override
    protected String getIdColumnName() {
        return "album_id";
    }

    @Override
    public List<Album> searchByName(String name) {
        return this.searchByColumnName("name", name);
    }

    @Override
    public String getSearchDetails(int id) {
        String baseDetails = super.getSearchDetails(id, List.of("name", "release_date"));
        StringBuilder details = new StringBuilder(baseDetails);

        List<String> artists = getRelatedNames(
                "SELECT C.name FROM CREATORS C JOIN ALBUM_ARTISTS AA ON C.creator_id = AA.creator_id " +
                        "WHERE AA.album_id = ?", id);
        details.append("Artists: ").append(String.join(", ", artists)).append("\n");

        List<String> tracks = getRelatedNames(
                "SELECT S.name || ' (Track ' || AT.track_number || ', ' || " +
                        "CAST(S.length / 60 AS TEXT) || ':' || " +
                        "CASE WHEN S.length % 60 < 10 THEN '0' || CAST(S.length % 60 AS TEXT) " +
                        "ELSE CAST(S.length % 60 AS TEXT) END || ')' " +
                        "FROM SONGS S JOIN ALBUM_TRACKS AT ON S.song_id = AT.song_id " +
                        "WHERE AT.album_id = ? ORDER BY AT.track_number",
                id
        );

        details.append("Tracks:\n\t").append(String.join("\n\t", tracks)).append("\n");

        return details.toString();
    }

    public int getIdByNameAndArtist(String albumName, int artistId) {
        String sql = "SELECT A.album_id FROM ALBUMS A " +
                "JOIN ALBUM_ARTISTS AA ON A.album_id = AA.album_id " +
                "WHERE LOWER(A.name) = LOWER(?) AND AA.creator_id = ?";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setString(1, albumName);
            stmt.setInt(2, artistId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error finding album by name and artist: " + e.getMessage());
        }
        return -1;
    }

    public List<Integer> getAlbumIdsByArtistId(int artistId) {
        String sql = "SELECT album_id FROM ALBUM_ARTISTS WHERE creator_id = ?";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, artistId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            System.out.println("Error fetching album IDs for artist: " + e.getMessage());
        }
        return ids;
    }

    public List<com.AngRobert.Zpotifai.model.AlbumTrack> getTracksForAlbum(int albumId) {
        String sql = "SELECT S.*, AT.track_number FROM SONGS S " +
                "JOIN ALBUM_TRACKS AT ON S.song_id = AT.song_id " +
                "WHERE AT.album_id = ? ORDER BY AT.track_number";
        List<com.AngRobert.Zpotifai.model.AlbumTrack> tracks = new ArrayList<>();
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, albumId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                com.AngRobert.Zpotifai.model.AlbumTrack t = new com.AngRobert.Zpotifai.model.AlbumTrack();
                t.setSong_id(rs.getInt("song_id"));
                t.setName(rs.getString("name"));
                t.setLength(rs.getInt("length"));
                t.setStreams(rs.getInt("streams"));
                t.setRelease_date(rs.getDate("release_date").toLocalDate());
                t.setTrack_number(rs.getInt("track_number"));
                tracks.add(t);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching tracks for album: " + e.getMessage());
        }
        return tracks;
    }
}
