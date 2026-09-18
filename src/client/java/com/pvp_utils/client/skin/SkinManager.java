package com.pvp_utils.client.skin;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkinManager {
    public static boolean PERSISTENT_COSMETIC_DISPLAY = false;
    public static final String NETHERITE_SWORD = "netherite_sword";
    public static final String NETHERITE_AXE = "netherite_axe";
    public static final String DIAMOND_SWORD = "diamond_sword";
    public static final String ELYTRA = "elytra";
    public static final String FISHING_ROD = "fishing_rod";
    public static final String SHIELD = "shield";
    public static final String COD = "cod";
    public static final String ARROW = "arrow";
    public static final String PAPER = "paper";
    public static final String SHEARS = "shears";
    public static final String HORSE_ARMOR = "leather_horse_armor";
    public static final String COSMETIC = "cosmetic";

    private static final Map<String, Item> BASE_ITEMS = new LinkedHashMap<>();
    private static final Map<String, List<Skin>> SKINS = new LinkedHashMap<>();
    private static final List<Skin> COSMETICS = new ArrayList<>();

    static {
        add(NETHERITE_SWORD, Items.NETHERITE_SWORD, 1000, "Colossal Sword", "Core Hammer", "Cursed Spear", "Dark Destroyer", "Death Hand", "Enchanted Dagger", "Enchanted Silver Sword", "Engine Blade", "Great Mace", "Great Scythe", "Halberd", "Heavy Greatsword", "Jade Glaive", "Hell Bringer", "Honored Greatsword", "Ktanazul", "Lantern Scythe", "Moonlight", "Poison Dagger", "Prismarine Greataxe", "Silver Halberd", "Storm Axe", "Twisted Greatsword", "Violet Splitter", "Void Sword", "Duality", "Yin", "Yang", "Scissor Dual Blade", "Scissor Light Blade", "Scissor Dark Blade", "Blood Gladius", "Blood Gladius Awakened", "Celestial Sword", "Celestial Morning Blade", "Celestial Sunset Blade", "Celestial Night Blade", "Combustion", "Combustion Awakened", "Conductive Spear", "Conductive Spear Awakened", "Resistance Greatsword", "Resistance Greatsword Awakened", "Royal Greatsword", "Royal Greatsword Awakened", "Singularity Hammer", "Singularity Hammer Awakened");
        add(NETHERITE_SWORD, Items.NETHERITE_SWORD, 4001, "Adventurer Sword", "Butcher Sword", "Crimson Sword", "Crystal Sword", "Dragon Sword", "Fire Sword", "King Sword", "Moonstone Sword", "Poison Sword", "Reaver Sword", "Spike Sword", "Broadsword of Entombed", "Demonic Sword", "Flamberg", "Gem Sword", "Geode Cleaver", "Silver Claymore", "Twisted Sword", "Underground Dwellers Sword", "Vampiric Blade");
        add(NETHERITE_AXE, Items.NETHERITE_AXE, 5001, "Gladiator Axe", "Guardian Axe", "Intruder Axe", "Invader Axe", "Lightbringer Axe", "Peasant Axe", "Royal Axe", "Spike Axe", "Underground Dwellers Axe", "Vampiric Axe", "Ral Axe");
        add(DIAMOND_SWORD, Items.DIAMOND_SWORD, 1018, "Poison Dagger");
        add(ELYTRA, Items.ELYTRA, 5001, "Elytra A", "Elytra B", "Elytra C");
        add(FISHING_ROD, Items.FISHING_ROD, 50001, "Beginner Rod", "Silver Rod", "Golden Rod", "Star Rod", "Bone Rod", "Magical Rod", "Master Rod");
        add(SHIELD, Items.SHIELD, 2000, "Buckler Shield", "Cursed Shield", "Great Shield", "Storm Shield");
        add(ARROW, Items.ARROW, 1, "Arrow 1", "Arrow 2", "Arrow 3", "Arrow 4", "Arrow 5", "Arrow 6", "Arrow 7", "Arrow 8");
        add(COD, Items.COD, 50000, "Fish 1", "Fish 2", "Fish 3", "Fish 4", "Fish 5", "Fish 6", "Fish 7", "Fish 8", "Fish 9", "Fish 10", "Fish 11", "Fish 12", "Fish 13", "Fish 14", "Fish 15", "Fish 16", "Fish 17", "Fish 18", "Fish 19", "Fish 20", "Fish 21", "Fish 22", "Fish 23", "Fish 24", "Fish 25", "Fish 26", "Fish 27", "Fish 28", "Fish 29", "Fish 30", "Fish 31", "Fish 32", "Fish 33", "Fish 34", "Fish 35", "Fish 36", "Fish 37", "Fish 38", "Fish 39", "Fish 40", "Fish 41", "Fish 42", "Fish 43", "Fish 44", "Fish 45", "Fish 46", "Fish 47", "Fish 48", "Fish 49", "Fish 50");
        add(PAPER, Items.PAPER, 49998, "Splash Water", "Splash Lava", "Fish Finder", "Simple Bait", "Magnetic Bait", "Wild Bait");
        add(SHEARS, Items.SHEARS, 50000, "Delicate Hook");
        add(HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR, 1, "Fire 1", "Fire 2");
        addCosmetics("adventurer_pack", "angelic_wings", "animal_ears", "axolotl_hat", "backpack", "basic_kite", "beach_pack", "beenie", "bee_wings", "bunny_balloon", "bunny_beanie", "burger_cap", "cardboard_suit", "clover_staff", "clover_umbrella", "cosmo_cane", "cosmo_hat", "cosmo_robe", "crop_basket", "cyber_blades", "dao_blade", "demon_horns", "demon_wings", "easter_bunny_head", "electric_guitar", "explorers_torch", "fedora", "firefly_lantern", "fish_bowl_helmet", "flame_fist", "flower_basket", "flower_umbrella", "foam_finger", "frankenstein_hat", "froggy_hat", "fruit_crown", "fry_pack", "ghost_cutlass", "ghost_pirate_hat", "gift_sack", "golden_santa_hat", "gothic_lantern", "halo", "hard_hat", "headphones", "honey_wand", "ice_cream_cone", "jade_crown", "jet_pack", "jingle_bell", "junk_drone", "lny_cape", "lny_dragon_hat", "lny_hair_hat", "lny_paper_umbrella", "lny_staff", "lotus_lantern", "lunar_banners", "lunar_crown", "lunar_rabbit_mask", "marshmallow_stick", "meat_cleaver", "medusa_hat", "monster_slayer_gear", "neptunes_trident", "net", "nymph_balloon_whole", "ornate_fan", "ornate_parasol", "panda_pack", "pellet_drum", "pirate_chest", "potion_balloon", "pot_of_gold", "prank_pack", "quiver", "rainbow_ear_muffs", "rainbow_wings", "reaper_scythe", "reindeer_antlers", "samurai_swords", "santa_hat", "saxophone", "snail_shell", "snow_globe", "spell_book", "spirit_dragons", "stasis_hammer", "stellar_hat", "suit_case", "sunhat", "television_helmet", "travel_case", "ufo_balloon_full", "veiled_hat", "visor_helmet", "wisp_cloak", "witch_broom", "witch_cauldron", "x_wing_balloon_full");
    }

    private SkinManager() {
    }

    public static boolean isAvailable() {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData server = minecraft == null ? null : minecraft.getCurrentServer();
        boolean available = PERSISTENT_COSMETIC_DISPLAY || (server != null && normalizeServer(server.ip).equals("mcbi.top"));
        if (!available) {
            disableAll();
        }
        return available;
    }

    public static boolean isDisplayEnabled() {
        return isAvailable() && Config.mcbiSkinDisplay;
    }

    public static boolean isActive(String type) {
        return isDisplayEnabled() && readModes().containsKey(type);
    }

    public static void setActive(String type, boolean active) {
        Map<String, Integer> modes = readModes();
        if (active) modes.putIfAbsent(type, 0);
        else modes.remove(type);
        writeModes(modes);
    }

    public static int selection(String type) {
        List<Skin> skins = skins(type);
        if (skins.isEmpty()) return 0;
        return Math.max(0, Math.min(skins.size() - 1, readModes().getOrDefault(type, 0)));
    }

    public static void setSelection(String type, int index) {
        Map<String, Integer> modes = readModes();
        modes.put(type, Math.max(0, Math.min(skins(type).size() - 1, index)));
        writeModes(modes);
    }

    public static List<String> names(String type) {
        return skins(type).stream().map(Skin::name).toList();
    }

    public static ItemStack replaceItemModel(ItemStack original) {
        if (original == null || original.isEmpty() || !isAvailable() || !Config.mcbiSkinDisplay) return original;
        for (Map.Entry<String, Item> entry : BASE_ITEMS.entrySet()) {
            if (original.is(entry.getValue()) && isActive(entry.getKey())) {
                Skin skin = skins(entry.getKey()).get(selection(entry.getKey()));
                ItemStack copy = original.copy();
                copy.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(original.getItem()));
                copy.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) skin.value()), List.of(), List.of(), List.of()));
                return copy;
            }
        }
        return original;
    }

    public static ItemStack cosmeticStack() {
        if (!isActive(COSMETIC) || COSMETICS.isEmpty()) return ItemStack.EMPTY;
        Skin skin = COSMETICS.get(selection(COSMETIC));
        ItemStack stack = new ItemStack(Items.CARVED_PUMPKIN);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("cosmetics", skin.id()));
        return stack;
    }

    private static void add(String type, Item item, int firstValue, String... names) {
        BASE_ITEMS.put(type, item);
        List<Skin> skins = SKINS.computeIfAbsent(type, ignored -> new ArrayList<>());
        for (int i = 0; i < names.length; i++) skins.add(new Skin(names[i], "", firstValue + i));
    }

    private static void addCosmetics(String... ids) {
        for (String id : ids) COSMETICS.add(new Skin(displayName(id), id, 0));
    }

    private static List<Skin> skins(String type) {
        return COSMETIC.equals(type) ? COSMETICS : SKINS.getOrDefault(type, Collections.emptyList());
    }

    private static Map<String, Integer> readModes() {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (Config.mcbiSkinModes == null || Config.mcbiSkinModes.isBlank()) return result;
        for (String part : Config.mcbiSkinModes.split(";")) {
            String[] split = part.split("=", 2);
            if (split.length != 2) continue;
            try {
                result.put(split[0], Integer.parseInt(split[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static void writeModes(Map<String, Integer> modes) {
        Config.mcbiSkinModes = String.join(";", modes.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        Config.save();
    }

    private static void disableAll() {
        if (!Config.mcbiSkinDisplay && (Config.mcbiSkinModes == null || Config.mcbiSkinModes.isBlank())) return;
        Config.mcbiSkinDisplay = false;
        Config.mcbiSkinModes = "";
        Config.save();
    }

    private static String normalizeServer(String address) {
        if (address == null) return "";
        String value = address.strip().toLowerCase(Locale.ROOT);
        int colon = value.indexOf(':');
        return colon >= 0 ? value.substring(0, colon) : value;
    }

    private static String displayName(String id) {
        String[] words = id.replace('_', ' ').split(" ");
        return Arrays.stream(words).filter(word -> !word.isBlank()).map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1)).reduce((left, right) -> left + " " + right).orElse(id);
    }

    private record Skin(String name, String id, int value) {
    }
}
