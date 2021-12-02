package org.madblock.newgamesapi.util;

import org.madblock.newgamesapi.NewGamesAPI1;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Optional;

public class SkinUtils {

    private SkinUtils() {}

    public static Optional<BufferedImage> getSkinFile(String path) {
        if (path != null) {
            File skinfile = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/"+path);
            try {
                BufferedImage image = ImageIO.read(skinfile);
                return Optional.of(image);
            } catch (Exception err) {
                NewGamesAPI1.getPlgLogger().warning("Error loading custom skin data for NPCHuman skin.");
            }
        }
        File fallback = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/default.png");
        try {
            BufferedImage image = ImageIO.read(fallback);
            return Optional.of(image);
        } catch (Exception err) {
            NewGamesAPI1.getPlgLogger().warning("Error loading fallback skin data for NPCHuman skin.");
        }
        return Optional.empty();
    }

    public static Optional<String> getModelFile(String path) {
        if (path != null) {
            File skinfile = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/"+path);

            try {
                BufferedReader r = new BufferedReader(new FileReader(skinfile));
                StringBuilder b = new StringBuilder();
                Iterator<String> lines = r.lines().iterator();

                while (lines.hasNext()) {
                    b.append(lines.next());
                    b.append("\n");
                }

                return Optional.of(b.toString());

            } catch (Exception err) {
                NewGamesAPI1.getPlgLogger().warning("Error loading custom skin model data for NPCHuman skin.");
            }
        }
        return Optional.empty();
    }

}
