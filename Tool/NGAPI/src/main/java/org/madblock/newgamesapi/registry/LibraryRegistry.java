package org.madblock.newgamesapi.registry;

import cn.nukkit.utils.TextFormat;
import com.google.gson.*;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.book.BookConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * A registry for storing all the books loaded on the server.
 * Registers BookConfigurations against String keys.
 */
public class LibraryRegistry {

    public static final String BOOK_CONFIG_LOCATION = "./plugins/NGAPI/books/";
    private static LibraryRegistry registryInstance;

    private HashMap<String, BookConfiguration> books;

    public LibraryRegistry() {
        this.books = new HashMap<>();
    }

    /**
     * Registers a Game via it's GameID. Does not accept duplicates. Case-
     * insensitive.
     * @return self for chaining.
     */
    public LibraryRegistry registerBook(BookConfiguration book) {
        String id = book.getBookID();
        if(!books.containsKey(id)){
            books.put(id, book);
        }
        return this;
    }

    /**
     * Makes the registry the result provided from GameRegistry#get() and
     * finalizes the instance to an extent.
     */
    public void setAsPrimaryRegistry(){
        if(registryInstance == null) registryInstance = this;
    }


    public void loadAllBooks(){
        NewGamesAPI1.getPlgLogger().info("== Loading all present books. ==");
        File bookDir = new File(NewGamesAPI1.get().getServer().getDataPath()+BOOK_CONFIG_LOCATION);

        if(bookDir.exists() && bookDir.isDirectory()){
            NewGamesAPI1.getPlgLogger().info(": > FOUND - Configuration directory exists.");
            File[] children = bookDir.listFiles();

            if(children != null) {

                for (File file : children) {

                    try {

                        if (file.getName().toLowerCase().endsWith(".json")) {
                            loadSpecificBook(file);
                        }

                    } catch (Exception err) {
                        err.printStackTrace();
                        NewGamesAPI1.getPlgLogger().info(": > FAIL - Check above for stacktrace.");
                    }
                }
            }

        } else {
            NewGamesAPI1.getPlgLogger().info(": > MISSING - Attempting to create config.");

            try {
                File location = new File(NewGamesAPI1.get().getServer().getDataPath()+BOOK_CONFIG_LOCATION);
                location.mkdirs();
                NewGamesAPI1.getPlgLogger().info(": > PASS - Created book configuration directory.");

            } catch (Exception err){
                err.printStackTrace();
                NewGamesAPI1.getPlgLogger().info(": > FAIL - Check above for stacktrace.");
                return;
            }
        }
        NewGamesAPI1.getPlgLogger().info("== Finished book loading. ==");
    }

    public Optional<String> loadSpecificBook(File book) {

        if(book.exists() && book.isFile()) {
            NewGamesAPI1.getPlgLogger().info(": > FOUND - Loading book @ "+book.getName());

            try {
                FileReader r = new FileReader(book);
                BufferedReader reader = new BufferedReader(r);
                String str = "";
                Iterator<String> i = reader.lines().iterator();

                while (i.hasNext()){
                    str = str.concat(i.next());
                }

                Optional<String> bookID = parseBookFromJson(str);
                if(bookID.isPresent()) NewGamesAPI1.getPlgLogger().info(": > READ - Book was read successfully.");
                return bookID;

            } catch (Exception err){
                err.printStackTrace();
                NewGamesAPI1.getPlgLogger().info(": > FAIL - Check above for stacktrace.");
            }
        }
        return Optional.empty();
    }

    public Optional<String> parseBookFromJson(String jsonText) {
        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(jsonText);

        if(rootElement instanceof JsonObject){
            JsonObject root = (JsonObject) rootElement;
            JsonElement idElement = root.get("identifier");
            JsonElement displayNameElement = root.get("display_name");
            JsonElement authorElement = root.get("author");
            JsonElement pagesElement = root.get("pages");

            BookConfiguration.Builder builder = BookConfiguration.builder();

            if(idElement instanceof JsonPrimitive) {
                JsonPrimitive idPrimitive = (JsonPrimitive) idElement;
                builder.setBookIdentifer(idPrimitive.getAsString());
            }

            if(displayNameElement instanceof JsonPrimitive) {
                JsonPrimitive namePrimitive = (JsonPrimitive) displayNameElement;
                builder.setDisplayName(TextFormat.colorize('&', namePrimitive.getAsString()));

            }

            if(authorElement instanceof JsonPrimitive) {
                JsonPrimitive authorPrimitive = (JsonPrimitive) authorElement;
                builder.setAuthor(TextFormat.colorize('&', authorPrimitive.getAsString()));
            }

            if(pagesElement instanceof JsonArray) {
                JsonArray pagesArray = (JsonArray) pagesElement;

                for (JsonElement page: pagesArray){
                    String parsedPage;

                    if(page instanceof JsonPrimitive) {
                        JsonPrimitive pagePrimitiveForm = (JsonPrimitive) page;
                        parsedPage = TextFormat.colorize('&', pagePrimitiveForm.getAsString());

                    } else if (page instanceof JsonArray) {
                        JsonArray pageArrayForm = (JsonArray) page;
                        parsedPage = parseMultiLinePage(pageArrayForm);

                    } else {
                        continue;
                    }
                    builder.addPage(parsedPage);
                }
            }
            BookConfiguration book = builder.build();
            registerBook(book);
            return Optional.of(book.getBookID());
        }
        return Optional.empty();
    }

    protected static String parseMultiLinePage(JsonArray page){
        StringBuilder build = new StringBuilder();

        for(int i = 0; i < page.size(); i++){
            JsonElement line = page.get(i);

            if(line instanceof JsonPrimitive) {
                JsonPrimitive linePrimitive = (JsonPrimitive) line;
                build.append(TextFormat.colorize('&', "&r"+linePrimitive.getAsString()));

                if(i != (page.size() - 1)){
                    build.append("\n");
                }
            }
        }
        String finalStr = build.toString();
        return finalStr.equals("") ? " " : finalStr;
    }



    /** @return the primary instance of the Registry. */
    public static LibraryRegistry get(){
        return registryInstance;
    }

    public Optional<BookConfiguration> getBook(String bookID){ return Optional.ofNullable(books.get(bookID.toLowerCase())); }
    public Set<String> getBookIDs() { return books.keySet(); }

}
