package com.AngRobert.Zpotifai.service;

import com.AngRobert.Zpotifai.model.Searchable;
import com.AngRobert.Zpotifai.repository.SearchableRepository;
import com.AngRobert.Zpotifai.util.AuditLogger;

import java.util.*;

public class SearchService {
    private final Map<String, SearchableRepository<?>> repositoryMap;

    public SearchService(List<SearchableRepository<?>> repos) {
        // we use a linked hash map so that the categories appear in the same order they were added. important for iterating over them later
        this.repositoryMap = new LinkedHashMap<>();
        for (SearchableRepository<?> repo : repos) {
            this.repositoryMap.put(repo.getCategoryName(), repo);
        }
    }

    // takes the list of repos that appear in the search and calls searchByName for each of them
    /*
        this method is called twice in a usual search. the first (category == 0) is the one the user inputs. the second
        is done after the user inputs the requested category and entry, and loops over the search results(which will be in the same order)
        and returns the item that the user requested
     */
    public Searchable handleSearch(String name, int category, int entry) {
        if (category == 0) {
            AuditLogger.log("Search performed for: " + name);
        }
        int category_no = 1;
        name = name.trim();
        // <?> is a wildcard, it means read-only
        for (SearchableRepository<?> repo : this.repositoryMap.values()) {
            List<?> rawResults = repo.searchByName(name);
            if (!rawResults.isEmpty()) {
                TreeSet<Searchable> sortedResults = new TreeSet<>();
                for (Object obj : rawResults) {
                    sortedResults.add((Searchable) obj);
                }
                
                // converts back to list for index-based access while maintaining the new sort order
                List<Searchable> results = new ArrayList<>(sortedResults);

                if (category == 0) {
                    // standard search listing
                    System.out.println(category_no + ": " + repo.getCategoryName() + ":");
                    for (int i = 0; i < results.size(); i++) {
                        Searchable item = results.get(i);
                        String creator = item.getCreatorDisplayName();
                        String displayName = item.getName();
                        if (!creator.isEmpty()) {
                            displayName += " [by " + creator + "]";
                        }
                        System.out.printf("\t%d: %s\n", i + 1, displayName);
                    }
                    System.out.println();
                } else if (category == category_no) {
                    // detail retrieval
                    if (entry > 0 && entry <= results.size()) {
                        Searchable item = results.get(entry - 1);
                        System.out.println(repo.getSearchDetails(item.getId()));
                        return item;
                    }
                }
                category_no++;
            }
        }
        if (category != 0) {
            System.out.println("Item not found for the given category and number.");
        }
        return null;
    }

    // this is the function called from MenuController on the first search
    public void handleSearch(String name) {
        this.handleSearch(name, 0, 0);
    }
}
