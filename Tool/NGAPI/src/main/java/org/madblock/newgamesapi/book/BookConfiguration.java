package org.madblock.newgamesapi.book;

import cn.nukkit.item.ItemBookWritten;
import org.madblock.newgamesapi.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A data object used to store Minecraft book data
 * in a json object. Used within NGAPI as a config.
 */
public abstract class BookConfiguration {

    protected String bookIdentifier;
    protected String displayName;
    protected String author;
    protected List<String> pages;

    protected BookConfiguration (boolean unmodifiable, String bookIdentifier, String displayName, String author, List<String> pages) {
        this.bookIdentifier = bookIdentifier == null ? "book#"+Utility.generateUniqueToken(5, 5) : bookIdentifier;
        this.displayName = displayName == null ? "A Book" : displayName;
        this.author = author == null ? "Anonymous" : author;
        this.pages = uList(pages == null ? new ArrayList<>() : pages, unmodifiable);
    }

    /*

    An example of the format:

    {
        "identifier": "book123",
        "display_name": "I AM A BOOK!",
        "author": "Chris Pratt",
        "pages": [
            "I AM A PAGE!",
            " ",
            "That was just an empty page???",
            [
                "I'm a page but",
                "I have newlines",
                " ",
                "within me!"
            ],
            "Back to our regularly scheduled single pages.",
            "- The end."
        ]
    }

    Array-based pages just have \n added at the end of each string and are concatenated together.
    They're only there to make reading the config easier.

     */

    /**
     * Sets the name, author, and contents of the specified book. Does not
     * create a new item (Overrides the provided one's data)
     * @param target The book to be edited.
     * @return Target for chaining.
     */
    public ItemBookWritten applyBookBlueprint(ItemBookWritten target) {
        target.setTitle(this.displayName);
        target.setAuthor(this.author);

        for(int i = 0; i < pages.size(); i++){
            target.setPageText(i, pages.get(i));
        }
        return target;
    }

    public static <V> List<V> uList(List<V> obj, boolean isUnmodifiable){
        return isUnmodifiable ? Collections.unmodifiableList(obj) : obj;
    }


    public String getBookID() { return bookIdentifier; }
    public String getDisplayName() { return displayName; }
    public String getAuthor() { return author; }
    public List<String> getPages() { return pages; }

    public static Builder builder() { return new Builder(); }



    public static class AssembledBookConfiguration extends BookConfiguration {

        protected AssembledBookConfiguration(String bookIdentifer, String displayName, String author, List<String> pages) {
            super(true, bookIdentifer, displayName, author, pages);
        }

    }



    public static class Builder extends BookConfiguration {

        public Builder() {
            super(false, null, null, null, null);
        }


        @Override
        public ItemBookWritten applyBookBlueprint(ItemBookWritten target) {
            throw new IllegalStateException("Books cannot be edited by a Builder");
        }

        public BookConfiguration build(){
            return new AssembledBookConfiguration(this.bookIdentifier, this.displayName, this.author, this.pages);
        }

        public Builder addPage(String page){
            return insertPage(-1, page);
        }

        public Builder insertPage(int index, String page){
            // If page is outside bounds, just add it to the end.
            String p = page == null ? " " : page;

            if((index < 0) || (index >= pages.size())){
                pages.add(p);

            } else {
                pages.add(index, page);
            }
            return this;
        }


        public Builder setBookIdentifer(String bookIdentifer){
            this.bookIdentifier = bookIdentifer;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder setAuthor(String author) {
            this.author = author;
            return this;
        }

        public Builder setPages(ArrayList<String> pages) {
            this.pages = pages == null ? new ArrayList<>() : new ArrayList<>(pages);
            // Pages array cannot be null else addPage methods will just break.
            return this;
        }
    }

}
