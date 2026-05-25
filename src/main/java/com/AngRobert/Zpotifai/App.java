package com.AngRobert.Zpotifai;

import com.AngRobert.Zpotifai.controller.MenuController;
import com.AngRobert.Zpotifai.repository.*;
import com.AngRobert.Zpotifai.service.DatabaseService;
import com.AngRobert.Zpotifai.service.SearchService;
import com.AngRobert.Zpotifai.util.DBConnection;
import com.AngRobert.Zpotifai.util.SchemaLoader;

import java.util.List;
import java.util.Scanner;

public class App {

    private final List<SearchableRepository<?>> repos;
    private final DatabaseService databaseService;

    public App() {
        // initializes all the database repos so they can be used in DatabaseService
        var albumRepo = new AlbumRepository();
        var albumTrackRepo = new AlbumTrackRepository();
        var artistRepo = new ArtistRepository();
        var hostRepo = new HostRepository();
        var podcastRepo = new PodcastRepository();
        var singleRepo = new SingleRepository();
        var tagRepo = new TagRepository();
        var collaboratorRepo = new CollaboratorRepository();

        this.repos = List.of(albumRepo, albumTrackRepo, artistRepo, hostRepo,
                podcastRepo, singleRepo, collaboratorRepo);

        this.databaseService = new DatabaseService(albumRepo, albumTrackRepo, artistRepo, hostRepo,
                podcastRepo, singleRepo, tagRepo, collaboratorRepo);
    }

    public void run() {
        DBConnection.init();
        SchemaLoader.run();

        SearchService searchService = new SearchService(this.repos);
        Scanner scanner = new Scanner(System.in);

        new MenuController(searchService, this.databaseService, scanner).start();

        DBConnection.close();
    }
}
