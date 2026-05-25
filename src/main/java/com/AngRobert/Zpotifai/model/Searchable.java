package com.AngRobert.Zpotifai.model;

// interface helps because i can have a List<Searchable> in the search method.
public interface Searchable extends Comparable<Searchable> {
    // method that ensures the subclasses are all searched by name
    public String getName();

    // for details
    public int getId();

    // shows who created an item (song, album, podcast) in the search display.
    // for each of the models, this information is stored in a field that gets automatically filled during the mapRow()
    // operation (when adding).
    default String getCreatorDisplayName() {
        return "";
    }

    @Override
    default int compareTo(Searchable other) {
        int nameCompare = this.getName().compareToIgnoreCase(other.getName());
        if (nameCompare != 0) {
            return nameCompare;
        }
        // if names are the same, use ID to distinguish them
        return Integer.compare(this.getId(), other.getId());
    }
}
