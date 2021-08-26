package org.madblock.newgamesapi;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.player.PlayerGameModeChangeEvent;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.network.protocol.AdventureSettingsPacket;
import cn.nukkit.network.protocol.SetPlayerGameTypePacket;
import cn.nukkit.utils.*;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// TODO: Move to util package.
public class Utility {

    public static final TextFormat DEFAULT_TEXT_COLOUR = TextFormat.GRAY;
    public static final char[] UNIQUE_TOKEN_CHARACTERS = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '$', '&', '*', '^', '%', '(', ')'};

    public static String generateServerMessage(String topic, TextFormat topicColour, String text) {
        return generateServerMessage(topic, topicColour, text, DEFAULT_TEXT_COLOUR);
    }

    public static String generateServerMessage(String topic, TextFormat topicColour, String text, TextFormat defaultTextColour) {
        return String.format("%s%s%s %s%s>> %s%s%s", topicColour, TextFormat.BOLD, topic.toUpperCase(), TextFormat.DARK_GRAY, TextFormat.BOLD, TextFormat.RESET, defaultTextColour, text);
    }

    public static String generateParagraph(String paragraphs[], TextFormat lineColour, TextFormat defaultTextColour, int charactersPerLine) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TextFormat.DARK_GRAY).append(TextFormat.BOLD).append(">>").append(TextFormat.RESET).append(lineColour);
        for (int i = 0; i < charactersPerLine; i++) {
            stringBuilder.append("-");
        }
        stringBuilder.append(TextFormat.DARK_GRAY).append(TextFormat.BOLD).append("<<");
        String line = stringBuilder.toString();

        String finalString = "\n\n" + line + "\n\n";

        for (String para : paragraphs) {
            finalString = finalString.concat("" + TextFormat.RESET + defaultTextColour + para.replaceAll(String.format("(\\S(.{0,%s}\\w*))", charactersPerLine), "$1\n"));
        }

        return finalString.concat("\n" + line + "\n");
    }

    public static String generateUnlimitedParagraph(String paragraphs[], TextFormat lineColour, TextFormat defaultTextColour, int lineLength) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TextFormat.DARK_GRAY).append(TextFormat.BOLD).append(">>").append(TextFormat.RESET).append(lineColour);
        for (int i = 0; i < lineLength; i++) {
            stringBuilder.append("-");
        }
        stringBuilder.append(TextFormat.DARK_GRAY).append(TextFormat.BOLD).append("<<");
        String line = stringBuilder.toString();

        String finalString = "\n\n" + line + "\n\n";

        for (String para : paragraphs) {
            finalString = finalString.concat("" + TextFormat.RESET + defaultTextColour + para + "\n");
        }

        return finalString.concat("\n" + line + "\n");
    }

    public static String generateUniqueToken(int minlength, int variation) {
        int length = minlength + (variation > 0 ? new Random().nextInt(variation) : 0);
        String fstr = "";
        for (int i = 0; i < length; i++) {
            Random r = new Random();
            fstr = fstr.concat(String.valueOf(UNIQUE_TOKEN_CHARACTERS[r.nextInt(UNIQUE_TOKEN_CHARACTERS.length)]));
        }
        return fstr;
    }

    public static void executeExperimentalAdventureSettingsUpdate(Player player) {
        AdventureSettingsPacket pk = new AdventureSettingsPacket();
        for (AdventureSettings.Type t : AdventureSettings.Type.values()) {
            pk.setFlag(t.getId(), player.getAdventureSettings().get(t));
        }

        pk.commandPermission = (player.isOp() ? AdventureSettingsPacket.PERMISSION_OPERATOR : AdventureSettingsPacket.PERMISSION_NORMAL);
        pk.playerPermission = (player.isOp() ? Player.PERMISSION_OPERATOR : Player.PERMISSION_MEMBER);
        pk.entityUniqueId = player.getId();

        //Server.broadcastPacket(player.getViewers().values(), pk);
        player.dataPacket(pk);

        player.resetInAirTicks();
    }

    public static String getLevelText(int level) {
        if (level < 20) {
            return "" + TextFormat.GRAY + level + TextFormat.RESET;
        } else if (level < 40) {
            return "" + TextFormat.BLUE + level + TextFormat.RESET;
        } else if (level < 60) {
            return "" + TextFormat.GREEN + level + TextFormat.RESET;
        } else if (level < 80) {
            return "" + TextFormat.YELLOW + level + TextFormat.RESET;
        } else {
            return "" + TextFormat.RED + level + TextFormat.RESET;
        }

    }

    public static boolean setGamemodeWorkaround(Player player, int gamemode, boolean clientSide, AdventureSettings newSettings) {
        if (gamemode < 0 || gamemode > 3 || player.getGamemode() == gamemode) {
            return false;
        }

        if (newSettings == null) {
            newSettings = player.getAdventureSettings().clone(player);
            newSettings.set(AdventureSettings.Type.WORLD_IMMUTABLE, (gamemode & 0x02) > 0);
            newSettings.set(AdventureSettings.Type.BUILD_AND_MINE, (gamemode & 0x02) <= 0);
            newSettings.set(AdventureSettings.Type.WORLD_BUILDER, (gamemode & 0x02) <= 0);
            newSettings.set(AdventureSettings.Type.ALLOW_FLIGHT, (gamemode & 0x01) > 0);
            newSettings.set(AdventureSettings.Type.NO_CLIP, gamemode == 0x03);
            newSettings.set(AdventureSettings.Type.FLYING, gamemode == 0x03);
        }

        PlayerGameModeChangeEvent ev;
        player.getServer().getPluginManager().callEvent(ev = new PlayerGameModeChangeEvent(player, gamemode, newSettings));

        if (ev.isCancelled()) {
            return false;
        }

        player.gamemode = gamemode;

        player.namedTag.putInt("playerGameType", player.getGamemode());

        if (!clientSide) {
            SetPlayerGameTypePacket pk = new SetPlayerGameTypePacket();
            int gm = gamemode;
            gm &= 0x03;
            if (gm == Player.SPECTATOR) {
                pk.gamemode = Player.CREATIVE;
            } else {
                pk.gamemode = gm;
            }
            player.dataPacket(pk);
        }

        player.setAdventureSettings(ev.getNewAdventureSettings());

        player.resetFallDistance();

        player.getInventory().sendContents(player);
        //player.getInventory().sendContents(player.getViewers().values());
        //player.getInventory().sendHeldItem(player.hasSpawned.values());
        player.getOffhandInventory().sendContents(player);
        player.getOffhandInventory().sendContents(player.getViewers().values());

        player.getInventory().sendCreativeContents();
        return true;
    }


    public static Skin parseSkinFromNBT(CompoundTag tag) {
        if (tag == null) throw new IllegalArgumentException("NBT cannot be null");
        Skin skin = new Skin();

        skin.setSkinId(tag.getString("ModelId"));
        skin.setSkinResourcePatch(new String(tag.getByteArray("SkinResourcePatch"), StandardCharsets.UTF_8));
        skin.setSkinData(new SerializedImage(tag.getInt("SkinImageWidth"), tag.getInt("SkinImageHeight"), tag.getByteArray("Data")));
        skin.setCapeData(tag.getByteArray("CapeData"));
        skin.setGeometryData(new String(tag.getByteArray("GeometryData"), StandardCharsets.UTF_8));
        skin.setAnimationData(new String(tag.getByteArray("AnimationData"), StandardCharsets.UTF_8));
        skin.setPremium(tag.getBoolean("PremiumSkin"));
        skin.setPersona(tag.getBoolean("PersonaSkin"));
        skin.setCapeOnClassic(tag.getBoolean("CapeOnClassicSkin"));
        skin.setCapeId(tag.getString("CapeId"));
        skin.setSkinColor(tag.getString("SkinColour"));
        skin.setArmSize(tag.getString("ArmSize"));

        ListTag<CompoundTag> animationListTag = tag.getList("AnimatedImageData", CompoundTag.class);
        for (int i = 0; i < animationListTag.size(); i++) {
            CompoundTag animationData = animationListTag.get(i);
            skin.getAnimations().add(
                    new SkinAnimation(
                            new BinaryStream(animationData.getByteArray("image")).getImage(),
                            animationData.getInt("type"),
                            animationData.getFloat("frames"),
                            animationData.getInt("expression")
                    )
            );
        }

        ListTag<CompoundTag> personaPiecesListTag = tag.getList("PersonaPieces", CompoundTag.class);
        for (int i = 0; i < personaPiecesListTag.size(); i++) {
            CompoundTag pieceData = personaPiecesListTag.get(i);
            skin.getPersonaPieces().add(
                    new PersonaPiece(
                            pieceData.getString("id"),
                            pieceData.getString("type"),
                            pieceData.getString("packId"),
                            pieceData.getBoolean("isDefault"),
                            pieceData.getString("productId")
                    )
            );
        }

        ListTag<CompoundTag> pieceTintColors = tag.getList("PieceTintColors", CompoundTag.class);
        for (int i = 0; i < pieceTintColors.size(); i++) {
            CompoundTag tintData = pieceTintColors.get(i);

            List<String> colors = new ArrayList<>();
            ListTag<StringTag> colorsTag = tintData.getList("colors", StringTag.class);
            for (int colorI = 0; colorI < colorsTag.size(); colorI++) {
                colors.add(colorsTag.get(colorI).parseValue());
            }

            skin.getTintColors().add(
                    new PersonaPieceTint(tintData.getString("pieceType"), colors)
            );
        }

        return skin;
    }

    public static CompoundTag parseNBTFromSkin(Skin skin) {
        if (skin == null) throw new IllegalArgumentException("Skin cannot be null");

        ListTag<CompoundTag> pieceTintColors = new ListTag<>("PieceTintColors");
        for (PersonaPieceTint tint : skin.getTintColors()) {

            ListTag<StringTag> colors = new ListTag<>("colors");
            for (String color : tint.colors) {
                colors.add(new StringTag("", color));
            }

            pieceTintColors.add(
                    new CompoundTag()
                            .putString("pieceType", tint.pieceType)
                            .putList(colors)
            );
        }

        ListTag<CompoundTag> personaPieces = new ListTag<>("PersonaPieces");
        for (PersonaPiece piece : skin.getPersonaPieces()) {
            personaPieces.add(
                    new CompoundTag()
                            .putString("id", piece.id)
                            .putString("type", piece.type)
                            .putString("packId", piece.packId)
                            .putBoolean("isDefault", piece.isDefault)
                            .putString("productId", piece.productId)
            );
        }

        ListTag<CompoundTag> animationData = new ListTag<>("AnimatedImageData");
        for (SkinAnimation animation : skin.getAnimations()) {
            BinaryStream image = new BinaryStream();
            image.putImage(animation.image);
            animationData.add(
                    new CompoundTag()
                            .putByteArray("image", image.get())
                            .putInt("type", animation.type)
                            .putFloat("frames", animation.frames)
                            .putInt("expression", animation.expression)
            );
        }

        CompoundTag skinDataTag = new CompoundTag("skin")
                .putString("ModelId", skin.getSkinId())
                .putByteArray("SkinResourcePatch", skin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                .putByteArray("Data", skin.getSkinData().data)
                .putInt("SkinImageWidth", skin.getSkinData().width)
                .putInt("SkinImageHeight", skin.getSkinData().height)
                .putList(animationData)
                .putList(personaPieces)
                .putList(pieceTintColors)
                .putByteArray("CapeData", skin.getCapeData().data)
                .putInt("CapeImageWidth", skin.getCapeData().width)
                .putInt("CapeImageHeight", skin.getCapeData().height)
                .putByteArray("GeometryData", skin.getGeometryData().getBytes(StandardCharsets.UTF_8))
                .putByteArray("AnimationData", skin.getAnimationData().getBytes(StandardCharsets.UTF_8))
                .putBoolean("PremiumSkin", skin.isPremium())
                .putBoolean("PersonaSkin", skin.isPersona())
                .putBoolean("CapeOnClassicSkin", skin.isCapeOnClassic())
                .putString("CapeId", skin.getCapeId())
                .putString("SkinColour", skin.getSkinColor())
                .putString("ArmSize", skin.getArmSize());
        return skinDataTag;
    }



    public static class ResourcePackCharacters {

        public static final String TAG_SUPER = "\uE130";
        public static final String TAG_TOURNEY = "\uE131";

        public static final String NO_SIGN_RED = "\uE180";
        public static final String NO_SIGN_BLUE = "\uE181";
        public static final String NO_SIGN_YELLOW = "\uE182";
        public static final String NO_SIGN_GREEN = "\uE183";
        public static final String NO_SIGN_LIGHT_PURPLE = "\uE184";
        public static final String NO_SIGN_AQUA = "\uE185";
        public static final String NO_SIGN_GOLD = "\uE186";
        public static final String NO_SIGN_WHITE = "\uE187";
        public static final String NO_SIGN_DARK_PURPLE = "\uE188";
        public static final String NO_SIGN_DARK_GREEN = "\uE189";
        public static final String NO_SIGN_GRAY = "\uE18A";
        public static final String NO_SIGN_DARK_AQUA = "\uE18B";

        public static final String LARGE_SQUARE_RED = "\uE190";
        public static final String LARGE_SQUARE_BLUE = "\uE191";
        public static final String LARGE_SQUARE_YELLOW = "\uE192";
        public static final String LARGE_SQUARE_GREEN = "\uE193";
        public static final String LARGE_SQUARE_LIGHT_PURPLE = "\uE194";
        public static final String LARGE_SQUARE_AQUA = "\uE195";
        public static final String LARGE_SQUARE_GOLD = "\uE196";
        public static final String LARGE_SQUARE_WHITE = "\uE197";
        public static final String LARGE_SQUARE_DARK_PURPLE = "\uE198";
        public static final String LARGE_SQUARE_DARK_GREEN = "\uE199";
        public static final String LARGE_SQUARE_GRAY = "\uE19A";
        public static final String LARGE_SQUARE_DARK_AQUA = "\uE19B";

        public static final String SMALL_SQUARE_RED = "\uE1A0";
        public static final String SMALL_SQUARE_BLUE = "\uE1A1";
        public static final String SMALL_SQUARE_YELLOW = "\uE1A2";
        public static final String SMALL_SQUARE_GREEN = "\uE1A3";
        public static final String SMALL_SQUARE_LIGHT_PURPLE = "\uE1A4";
        public static final String SMALL_SQUARE_AQUA = "\uE1A5";
        public static final String SMALL_SQUARE_GOLD = "\uE1A6";
        public static final String SMALL_SQUARE_WHITE = "\uE1A7";
        public static final String SMALL_SQUARE_DARK_PURPLE = "\uE1A8";
        public static final String SMALL_SQUARE_DARK_GREEN = "\uE1A9";
        public static final String SMALL_SQUARE_GRAY = "\uE1AA";
        public static final String SMALL_SQUARE_DARK_AQUA = "\uE1AB";

        public static final String TIME = "\uE1B0";

        public static final String ARMOUR_EMPTY = "\uE1B1";
        public static final String ARMOUR_HALF = "\uE1B2";
        public static final String ARMOUR_FULL1 = "\uE1B3"; // Oops about the dupe
        public static final String ARMOUR_FULL2 = "\uE1B4";

        public static final String HEART_EMPTY = "\uE1B5";
        public static final String HEART_FULL = "\uE1B6";
        public static final String HEART_HALF = "\uE1B7";
        public static final String HEART_ABSORB_FULL = "\uE1B8";
        public static final String HEART_ABSORB_HALF = "\uE1B9";

        public static final String FOOD_EMPTY = "\uE1BA";
        public static final String FOOD_FULL = "\uE1BB";
        public static final String FOOD_HALF = "\uE1BC";

        public static final String IRON_INGOT = "\uE1BD";
        public static final String GOLD_INGOT = "\uE1BE";
        public static final String DIAMOND = "\uE1BF";

        public static final String PING_BEST = "\uE1C0";
        public static final String PING_BETTER = "\uE1C1";
        public static final String PING_GOOD = "\uE1C2";
        public static final String PING_BAD = "\uE1C3";
        public static final String PING_YIKES = "\uE1C4";
        public static final String PING_OOF = "\uE1C5";

        public static final String COIN = "\uE1C6";
        public static final String TROPHY = "\uE1C7";

        public static final String LONELY_PERSON = "\uE1C8";
        public static final String MORE_PEOPLE = "\uE1C9";

        public static final String SKULL = "\uE1CA";

        public static final String TACO = "\uE1CB";
        public static final String DISCORD = "\uE1CC";


        private String character;

        ResourcePackCharacters(String character) {
            this.character = character;
        }

        @Override
        public String toString() {
            return character;
        }

        public static String getNoSignCharacter(TextFormat color) {

            switch (color) {
                case RED:
                    return NO_SIGN_RED;
                case BLUE:
                    return NO_SIGN_BLUE;
                case YELLOW:
                    return NO_SIGN_YELLOW;
                case GREEN:
                    return NO_SIGN_GREEN;
                case LIGHT_PURPLE:
                    return NO_SIGN_LIGHT_PURPLE;
                case AQUA:
                    return NO_SIGN_AQUA;
                case GOLD:
                    return NO_SIGN_GOLD;
                case WHITE:
                    return NO_SIGN_WHITE;
                case DARK_PURPLE:
                    return NO_SIGN_DARK_PURPLE;
                case DARK_GREEN:
                    return NO_SIGN_DARK_GREEN;
                case GRAY:
                    return NO_SIGN_GRAY;
                case DARK_AQUA:
                    return NO_SIGN_DARK_AQUA;
                default:
                    return null;
            }

        }

        public static String getLargeSquareCharacter(TextFormat color) {

            switch (color) {
                case RED:
                    return LARGE_SQUARE_RED;
                case BLUE:
                    return LARGE_SQUARE_BLUE;
                case YELLOW:
                    return LARGE_SQUARE_YELLOW;
                case GREEN:
                    return LARGE_SQUARE_GREEN;
                case LIGHT_PURPLE:
                    return LARGE_SQUARE_LIGHT_PURPLE;
                case AQUA:
                    return LARGE_SQUARE_AQUA;
                case GOLD:
                    return LARGE_SQUARE_GOLD;
                case WHITE:
                    return LARGE_SQUARE_WHITE;
                case DARK_PURPLE:
                    return LARGE_SQUARE_DARK_PURPLE;
                case DARK_GREEN:
                    return LARGE_SQUARE_DARK_GREEN;
                case GRAY:
                    return LARGE_SQUARE_GRAY;
                case DARK_AQUA:
                    return LARGE_SQUARE_DARK_AQUA;
                default:
                    return null;
            }

        }

        public static String getSmallSquareCharacter(TextFormat color) {

            switch (color) {
                case RED:
                    return SMALL_SQUARE_RED;
                case BLUE:
                    return SMALL_SQUARE_BLUE;
                case YELLOW:
                    return SMALL_SQUARE_YELLOW;
                case GREEN:
                    return SMALL_SQUARE_GREEN;
                case LIGHT_PURPLE:
                    return SMALL_SQUARE_LIGHT_PURPLE;
                case AQUA:
                    return SMALL_SQUARE_AQUA;
                case GOLD:
                    return SMALL_SQUARE_GOLD;
                case WHITE:
                    return SMALL_SQUARE_WHITE;
                case DARK_PURPLE:
                    return SMALL_SQUARE_DARK_PURPLE;
                case DARK_GREEN:
                    return SMALL_SQUARE_DARK_GREEN;
                case GRAY:
                    return SMALL_SQUARE_GRAY;
                case DARK_AQUA:
                    return SMALL_SQUARE_DARK_AQUA;
                default:
                    return null;
            }

        }


    }

}
