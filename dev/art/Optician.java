import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Optician's Bench, two blocks wide like the game's big benches and built from the game's own
 * block textures. Every tier is a model of its own and shows what its upgrade brought:
 * <ol>
 *   <li>a makeshift trestle table: a stone grinding wheel with a crank, an eye chart on an easel, a
 *   magnifying glass, a candle, a sack of sand and a crate of glass;</li>
 *   <li>iron, quartz and leather: a proper desk with drawers, a back board with the chart and a shelf of
 *   tinted lenses, a felt polishing wheel and a water trough at the grindstone, a leather tool roll;</li>
 *   <li>gold and crystal: brass fittings, a telescope on a tripod, a lamp with a glowing crystal;</li>
 *   <li>adamantite and void: a dark desk with a purple runner, void crystals growing from the board, a
 *   voidheart in a jar, violet light in the lenses;</li>
 *   <li>lumenite: gold fittings and a lumenite crystal floating in two rings over the grindstone.</li>
 * </ol>
 * While something is being crafted the grindstone, its crank and the polishing wheel turn; on tier 5 the
 * lumenite crystal always floats and turns in its rings, faster while crafting.
 *
 * <p>Model space: x -32..32 (two blocks; the root sits half a block to the side like the game's benches),
 * y up from the floor, z -16..16 with the front at +z. The table top is at y 26.
 */
final class Optician {
    static final String DIR = "Blocks/FlexiisGoggles/";

    static final Art.Mat SOFTWOOD = tex("Wood_Softwood_Planks_02");
    static final Art.Mat DRYWOOD = tex("Wood_Drywood_Planks");
    static final Art.Mat HARDWOOD = tex("Wood_Hardwood_Planks");
    static final Art.Mat DARKWOOD = tex("Wood_Darkwood_Planks");
    static final Art.Mat BLACKWOOD = tex("Wood_Blackwood_Planks");
    static final Art.Mat COPPER = tex("Metal_Copper_Smooth");
    static final Art.Mat IRON = tex("Metal_Iron_Smooth");
    static final Art.Mat BRASS = tex("Metal_Bronze_Smooth");
    static final Art.Mat GOLD = tex("Rock_Gold_Smooth_Top");
    static final Art.Mat GRINDSTONE = tex("Calcite");
    static final Art.Mat FELT = tex("Cloth_Red");
    static final Art.Mat RUNNER = tex("Cloth_Purple");
    static final Art.Mat ADAMANTITE = Art.Mat.of("#8e2f36", Art.Style.METAL);
    static final Art.Mat ROPE = Art.Mat.of("#c9a66b", Art.Style.LEATHER);
    static final Art.Mat BURLAP = Art.Mat.of("#a88e62", Art.Style.LEATHER);
    static final Art.Mat LEATHER = Art.Mat.of("#6e4127", Art.Style.LEATHER);
    static final Art.Mat WAX = Art.Mat.of("#efe7d2", Art.Style.PLAIN);
    static final Art.Mat FLAME = Art.Mat.of("#ffc24a", Art.Style.GLOW);
    static final Art.Mat CHART = new Art.Mat(0, Art.Style.CHART);
    static final Art.Mat WATER = Art.Mat.of("#3d7db5", Art.Style.GLASS);
    static final Art.Mat CLEAR = Art.Mat.of("#d4eef6", Art.Style.GLASS);
    static final Art.Mat BLUE = Art.Mat.of("#7db6ff", Art.Style.GLASS);
    static final Art.Mat AMBER = Art.Mat.of("#ffcd5e", Art.Style.GLASS);
    static final Art.Mat GREEN = Art.Mat.of("#8fe08a", Art.Style.GLASS);
    static final Art.Mat JAR = Art.Mat.of("#cfe6ee", Art.Style.PANE);
    static final Art.Mat LAMP = Art.Mat.of("#ffd36b", Art.Style.GLOW);
    static final Art.Mat VOID = Art.Mat.of("#b07cff", Art.Style.GLOW);
    static final Art.Mat LUMENITE = Art.Mat.of("#3fe6d6", Art.Style.GLOW);
    static final Art.Mat[] BOOKS = {Art.Mat.of("#8a2f2a", Art.Style.LEATHER), Art.Mat.of("#2f4f7a", Art.Style.LEATHER), Art.Mat.of("#3f6b35", Art.Style.LEATHER)};

    private Optician() {
    }

    static Art.Mat tex(String name) {
        return Art.Mat.tex("BlockTextures/" + name + ".png");
    }

    static void all() throws IOException {
        for (int tier = 1; tier <= 5; tier++) {
            String name = DIR + "Optician_Bench_Tier" + tier;
            Art.Model model = bench(name, tier);
            model.write();
            BufferedImage texture = model.texture(97L + tier);
            Art.png(texture, name + "_Texture.png");
            // crafting: the wheels turn twice and four times a second (60 frames), the crystal once
            List<Motion> crafting = new ArrayList<>(List.of(new Motion("Grindstone", Z, 2, 0), new Motion("Crank", Z, 2, 0)));
            if (tier >= 2) {
                crafting.add(new Motion("Polisher", Z, 4, 0));
            }
            if (tier >= 5) {
                crafting.addAll(lumeniteMotions());
                animation(name + "_Idle.blockyanim", 240, lumeniteMotions());
            }
            animation(name + "_Crafting.blockyanim", 60, crafting);
            if (tier == 1) {
                Art.png(Art.icon(model, texture, -40, 32, 2, 1.15, 2), "Icons/ItemsGenerated/Flexii_Bench_Optician.png");
            }
        }
        Files.createDirectories(Art.COMMON.resolve("Icons/CraftingCategories/FlexiisGoggles"));
        copyIcon("Flexii_Lens_Polished", "Materials");
        copyIcon("Flexii_Goggles_Zoom_2", "Zoom");
        copyIcon("Flexii_Goggles_NightVision_2", "NightVision");
        copyIcon("Flexii_Goggles_Ultra", "Ultra");
    }

    static void copyIcon(String item, String category) throws IOException {
        Files.copy(Art.COMMON.resolve("Icons/ItemsGenerated/" + item + ".png"),
                Art.COMMON.resolve("Icons/CraftingCategories/FlexiisGoggles/" + category + ".png"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    /** Fittings: rope, iron, brass, adamantite, gold. */
    static Art.Mat fitting(int tier) {
        return switch (tier) {
            case 1 -> ROPE;
            case 2 -> IRON;
            case 3 -> BRASS;
            case 4 -> ADAMANTITE;
            default -> GOLD;
        };
    }

    /** Instruments: the lathe's metal, the spectacles, the telescope's bands. Copper first, as in the recipe. */
    static Art.Mat instrument(int tier) {
        return switch (tier) {
            case 1 -> COPPER;
            case 2 -> IRON;
            case 3, 4 -> BRASS;
            default -> GOLD;
        };
    }

    static Art.Model bench(String path, int tier) {
        Art.Model m = new Art.Model(path, false);
        m.rootName = "R-Attachment";
        m.rootPosition = new double[]{-16, 0, 0};
        if (tier == 1) {
            trestle(m);
            easel(m);
        } else {
            desk(m, tier);
            board(m, tier);
        }
        lathe(m, tier);
        props(m, tier);
        if (tier >= 3) {
            telescope(m, tier);
        }
        if (tier == 3 || tier == 4) {
            lamp(m, tier);
        }
        if (tier >= 4) {
            voidTouched(m, tier);
        }
        if (tier >= 5) {
            lumenite(m);
        }
        return m;
    }

    // ---------------------------------------------------------------- the table

    /** Tier 1: planks on two lashed trestles, a sack of sand and a crate of glass beneath. */
    static void trestle(Art.Model m) {
        m.add(new Art.Box("Top", 0, 24, 0, 64, 4, 30, SOFTWOOD));
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "_L" : "_R";
            double x = 24 * side;
            m.add(new Art.Box("Leg_A" + s, x, 12, 0, 3, 26, 3, DRYWOOD).rot(30, 0, 0));
            m.add(new Art.Box("Leg_B" + s, x, 12, 0, 3, 26, 3, DRYWOOD).rot(-30, 0, 0));
            m.add(new Art.Box("Bearer" + s, x, 20.5, 0, 5, 3, 26, DRYWOOD));
            m.add(new Art.Box("Lashing" + s, x, 12, 0, 5, 4, 5, ROPE));
        }
        m.add(new Art.Box("Stretcher", 0, 12, 0, 50, 3, 3, DRYWOOD));
        m.add(new Art.Box("Sack", -12, 4.5, 7, 9, 9, 7, BURLAP));
        m.add(new Art.Box("Sack_Neck", -12, 10, 7, 4, 2, 4, BURLAP));
        m.add(new Art.Box("Sack_Tie", -12, 9.75, 7, 5, 1, 5, ROPE));
        m.add(new Art.Box("Crate", 13, 4, 6, 11, 8, 9, DRYWOOD));
        m.add(new Art.Box("Crate_Glass_1", 11, 8, 3.5, 6, 6, 1, CLEAR).rot(0, 0, -6));
        m.add(new Art.Box("Crate_Glass_2", 14, 8.5, 6, 6, 7, 1, BLUE).rot(0, 0, 5));
        m.add(new Art.Box("Crate_Glass_3", 13, 7.5, 8.5, 6, 6, 1, CLEAR).rot(0, 0, -3));
    }

    /** Tiers 2-5: a desk with legs and a shelf on the left, a cabinet with two drawers on the right. */
    static void desk(Art.Model m, int tier) {
        Art.Mat top = tier >= 5 ? BLACKWOOD : tier == 4 ? DARKWOOD : HARDWOOD;
        Art.Mat frame = tier >= 4 ? BLACKWOOD : DARKWOOD;
        Art.Mat fronts = tier >= 4 ? DARKWOOD : HARDWOOD;
        Art.Mat fitting = fitting(tier);
        m.add(new Art.Box("Top", 0, 24, 0, 64, 4, 30, top));
        for (int z = -12; z <= 12; z += 24) {
            String s = z < 0 ? "_Back" : "_Front";
            m.add(new Art.Box("Leg" + s, -28, 12, z, 4, 20, 4, frame));
            m.add(new Art.Box("Foot" + s, -28, 1.5, z, 6, 3, 6, fitting));
        }
        m.add(new Art.Box("Apron_Front", -11, 20, 13, 30, 4, 2, frame));
        m.add(new Art.Box("Apron_Back", -11, 20, -13, 30, 4, 2, frame));
        m.add(new Art.Box("Apron_Side", -28, 20, 0, 2, 4, 20, frame));
        m.add(new Art.Box("Shelf", -11, 7, 0, 30, 2, 24, top));
        m.add(new Art.Box("Cabinet", 18, 12, 0, 24, 20, 28, frame));
        m.add(new Art.Box("Plinth", 18, 1.5, 0, 26, 3, 30, frame));
        for (int x = 7; x <= 29; x += 22) {
            for (int z = -13; z <= 13; z += 26) {
                m.add(new Art.Box("Cabinet_Foot_" + x + "_" + z, x, 2, z, 5, 4, 5, fitting));
            }
        }
        for (int i = 0; i < 2; i++) {
            double y = i == 0 ? 16.5 : 8.5;
            m.add(new Art.Box("Drawer_" + i, 18, y, 14.5, 20, 7, 1, fronts));
            m.add(new Art.Box("Pull_" + i, 18, y, 15.5, 5, 1, 1, fitting));
        }
        for (int x = -30; x <= 30; x += 60) {
            for (int z = -13; z <= 13; z += 26) {
                m.add(new Art.Box("Corner_" + x + "_" + z, x, 24, z, 5, 5, 5, fitting));
            }
        }
        if (tier >= 3) {
            m.add(new Art.Box("Trim_Front", 0, 23.5, 15.5, 54, 1, 1, fitting));
        }
        // on the shelf: books, and a sack of sand until the voidheart's jar takes its place
        m.add(new Art.Box("Book_1", -5, 9, -1, 8, 2, 6, BOOKS[0]));
        m.add(new Art.Box("Book_2", -5, 11, -1, 7, 2, 6, BOOKS[1]).rot(0, 8, 0));
        m.add(new Art.Box("Book_3", -5, 13, -1, 6, 2, 5, BOOKS[2]).rot(0, -6, 0));
        if (tier <= 3) {
            m.add(new Art.Box("Sack", -18, 11.5, 3, 8, 7, 7, BURLAP));
            m.add(new Art.Box("Sack_Neck", -18, 15.5, 3, 4, 1, 4, BURLAP));
            m.add(new Art.Box("Sack_Tie", -18, 16.5, 3, 5, 1, 5, ROPE));
        }
    }

    /** Tiers 2-5: posts, a board with the eye chart and a shelf of lenses, a cap on top. */
    static void board(Art.Model m, int tier) {
        Art.Mat frame = tier >= 4 ? BLACKWOOD : DARKWOOD;
        Art.Mat panel = tier >= 4 ? DARKWOOD : HARDWOOD;
        m.add(new Art.Box("Post_L", -29.5, 40, -13.5, 3, 28, 3, frame));
        m.add(new Art.Box("Post_R", 29.5, 40, -13.5, 3, 28, 3, frame));
        m.add(new Art.Box("Board", 0, 39, -14, 56, 26, 1, panel));
        m.add(new Art.Box("Cap", 0, 55.5, -13.5, 64, 3, 5, tier >= 3 ? fitting(tier) : frame));
        m.add(new Art.Box("Chart", -10, 40, -13, 18, 24, 1, frame).face(0, CHART));
        m.add(new Art.Box("Lens_Shelf", 16, 43.5, -11.5, 24, 1, 4, frame));
        Art.Mat[] lenses = switch (tier) {
            case 2 -> new Art.Mat[]{CLEAR, BLUE, AMBER, GREEN};
            case 3 -> new Art.Mat[]{CLEAR, BLUE, LAMP, AMBER};
            case 4 -> new Art.Mat[]{VOID, BLUE, VOID, AMBER};
            default -> new Art.Mat[]{LUMENITE, VOID, LUMENITE, AMBER};
        };
        for (int i = 0; i < 4; i++) {
            double[] at = {8 + i * 6, 47, -11.5};
            Shapes.disc(m, "Shelf_Rim_" + i, at, 6, 4, 1, Art.quaternion(0, 0, 0), instrument(tier), false);
            Shapes.disc(m, "Shelf_Lens_" + i, at, 4, 0, 1, Art.quaternion(0, 0, 0), lenses[i], lenses[i].style() == Art.Style.GLOW);
        }
    }

    /** Tier 1: the chart on a little easel. */
    static void easel(Art.Model m) {
        m.add(new Art.Box("Easel_L", -19, 40, -11, 2, 28, 2, DRYWOOD));
        m.add(new Art.Box("Easel_R", -5, 40, -11, 2, 28, 2, DRYWOOD));
        m.add(new Art.Box("Easel_Top", -12, 54, -11, 17, 2, 3, DRYWOOD));
        m.add(new Art.Box("Easel_Ledge", -12, 34.5, -10, 17, 1, 3, DRYWOOD));
        m.add(new Art.Box("Chart", -12, 44, -10.25, 14, 18, 1, DRYWOOD).face(0, CHART));
    }

    // ---------------------------------------------------------------- the lens-grinding lathe

    /**
     * A grindstone facing the front, turned by a crank, and from tier 2 a felt polishing wheel beside it
     * and a water trough under the stone.
     */
    static void lathe(Art.Model m, int tier) {
        Art.Mat wood = tier == 1 ? DRYWOOD : tier >= 4 ? BLACKWOOD : DARKWOOD;
        Art.Mat metal = instrument(tier);
        double[] front = Art.quaternion(0, 0, 0);
        double[] stone = {14, 37, 0};
        m.add(new Art.Box("Lathe_Bed", 14, 27, 0, 12, 2, 16, wood));
        m.add(new Art.Box("Lathe_Upright_Back", 14, 34, -6, 3, 12, 2, wood));
        m.add(new Art.Box("Lathe_Upright_Front", 14, 34, 6, 3, 12, 2, wood));
        m.add(new Art.Box("Lathe_Axle", 14, 37, 0, 2, 2, 13, metal));
        int mark = m.boxes.size();
        Shapes.disc(m, "Grindstone_Stone", stone, 14, 0, 4, front, GRINDSTONE, false);
        Shapes.disc(m, "Grindstone_Hub", stone, 7, 0, 6, front, metal, false);
        m.group("Grindstone", mark, stone[0], stone[1], stone[2]);
        mark = m.boxes.size();
        m.add(new Art.Box("Crank_Arm", 14, 34, 7.5, 2, 8, 1, metal));
        m.add(new Art.Box("Crank_Grip", 14, 31, 9.5, 2, 2, 3, wood));
        m.group("Crank", mark, stone[0], stone[1], stone[2]);
        if (tier >= 2) {
            m.add(new Art.Box("Trough", 14, 28.25, 0, 16, 5, 7, metal).face(4, WATER));
            double[] felt = {27, 35, 0};
            m.add(new Art.Box("Polisher_Bed", 27, 27, 0, 8, 2, 10, wood));
            m.add(new Art.Box("Polisher_Post_Back", 27, 32.5, -3.5, 3, 9, 2, wood));
            m.add(new Art.Box("Polisher_Post_Front", 27, 32.5, 3.5, 3, 9, 2, wood));
            m.add(new Art.Box("Polisher_Axle", 27, 35, 0, 2, 2, 8, metal));
            mark = m.boxes.size();
            Shapes.disc(m, "Polisher_Felt", felt, 10, 0, 3, front, FELT, false);
            Shapes.disc(m, "Polisher_Hub", felt, 3, 0, 5, front, metal, false);
            m.group("Polisher", mark, felt[0], felt[1], felt[2]);
        }
    }

    // ---------------------------------------------------------------- on the table

    static void props(Art.Model m, int tier) {
        Art.Mat metal = instrument(tier);
        double table = 26;
        // spectacles, open, facing the front
        Art.Mat specLens = tier >= 5 ? LUMENITE : tier == 4 ? VOID : BLUE;
        spectacles(m, 1, tier >= 4 ? table + 1 : table, 10, tier == 1 ? DRYWOOD : metal, specLens, specLens.style() == Art.Style.GLOW);
        double[] flat = Art.quaternion(-90, 0, 0);
        if (tier <= 2) {
            // a magnifier on a stand, tipped back towards whoever works here
            double[] tipped = Art.quaternion(-20, 0, 0);
            double[] lens = {-23, table + 13, 8};
            m.add(new Art.Box("Magnifier_Foot", -23, table + 1, 8, 6, 2, 6, tier == 1 ? DRYWOOD : metal));
            m.add(new Art.Box("Magnifier_Stem", -23, table + 5, 8, 2, 6, 2, metal));
            Shapes.disc(m, "Magnifier_Ring", lens, 10, 8, 1, tipped, metal, false);
            Shapes.disc(m, "Magnifier_Glass", lens, 8, 0, 1, tipped, CLEAR, false);
        } else {
            // a magnifying glass lying in front of the telescope
            double[] glass = {-22, table + 0.5, 9};
            Shapes.disc(m, "Magnifier_Ring", glass, 8, 6, 1, flat, metal, false);
            Shapes.disc(m, "Magnifier_Glass", glass, 6, 0, 1, flat, CLEAR, false);
            Shapes.beam(m, "Magnifier_Handle", new double[]{-18.5, table + 0.5, 11}, new double[]{-13, table + 0.5, 14}, 2, LEATHER);
        }
        if (tier == 1) {
            // a candle at the back and three glass blanks
            m.add(new Art.Box("Candle_Dish", -27, table + 0.5, -8, 5, 1, 5, IRON));
            m.add(new Art.Box("Candle", -27, table + 3.5, -8, 2, 5, 2, WAX));
            m.add(new Art.Box("Candle_Flame", -27, table + 7, -8, 1, 2, 1, FLAME)).glow();
            for (int i = 0; i < 3; i++) {
                double[] at = {20 + i * 5, table + 0.5 + (i == 1 ? 1 : 0), 12 + (i == 1 ? -0.5 : 0)};
                Shapes.disc(m, "Blank_" + i, at, 4, 0, 1, flat, i == 1 ? BLUE : CLEAR, false);
            }
            return;
        }
        // a tray of lenses in front of the lathe
        m.add(new Art.Box("Tray", 22, table + 0.5, 12.5, 17, 1, 5, tier >= 4 ? BLACKWOOD : DARKWOOD));
        Art.Mat[] tray = tier >= 5 ? new Art.Mat[]{AMBER, LUMENITE, BLUE} : tier == 4 ? new Art.Mat[]{AMBER, VOID, BLUE} : new Art.Mat[]{AMBER, CLEAR, BLUE};
        for (int i = 0; i < 3; i++) {
            Shapes.disc(m, "Tray_Lens_" + i, new double[]{16.5 + i * 5.5, table + 1.5, 12.5}, 4, 0, 1, flat, tray[i], tray[i].style() == Art.Style.GLOW);
        }
        // a leather tool roll
        double roll = tier >= 4 ? table + 1 : table;
        m.add(new Art.Box("Tool_Roll", -10, roll + 0.5, 10, 11, 1, 6, LEATHER));
        m.add(new Art.Box("Tool_Roll_Rolled", -16, roll + 1.25, 10, 2, 2, 7, LEATHER));
        m.add(new Art.Box("Tool_File", -11.5, roll + 1.5, 10, 1, 1, 5, IRON).rot(0, 8, 0));
        m.add(new Art.Box("Tool_Pick", -9, roll + 1.5, 10, 1, 1, 5, metal).rot(0, -5, 0));
        m.add(new Art.Box("Tool_Handle", -6.5, roll + 1.5, 9, 2, 1, 3, HARDWOOD));
        // quartz, the first upgrade's crystal, in the back corner
        m.crystal("Quartz_1", -26, table, -9.5, 4, 9, 0, 0, 8, Art.Materials.QUARTZ, false);
        m.crystal("Quartz_2", -23.5, table, -8.5, 3, 6, 10, 0, -22, Art.Materials.QUARTZ_PINK, false);
        m.crystal("Quartz_3", -27.5, table, -6.5, 3, 5, -12, 0, 20, Art.Materials.QUARTZ, false);
    }

    static void spectacles(Art.Model m, double x, double table, double z, Art.Mat frame, Art.Mat lens, boolean glow) {
        double[] facing = Art.quaternion(0, 0, 0);
        for (int side = -1; side <= 1; side += 2) {
            double[] at = {x + 3 * side, table + 2.5, z};
            String s = side < 0 ? "_L" : "_R";
            Shapes.disc(m, "Spectacles_Rim" + s, at, 5, 3, 1, facing, frame, false);
            Shapes.disc(m, "Spectacles_Lens" + s, at, 3, 0, 1, facing, lens, glow);
            Shapes.beam(m, "Spectacles_Temple" + s, new double[]{x + 5.5 * side, table + 3.5, z - 0.5}, new double[]{x + 5.5 * side, table + 0.5, z - 6.5}, 1, frame);
        }
        m.add(new Art.Box("Spectacles_Bridge", x, table + 3.5, z, 1, 1, 1, frame));
    }

    // ---------------------------------------------------------------- tier 3 and up

    /** A telescope on a tripod at the left end, pointing up and out. */
    static void telescope(Art.Model m, int tier) {
        Art.Mat tube = tier == 4 ? ADAMANTITE : tier >= 5 ? DARKWOOD : LEATHER;
        Art.Mat band = instrument(tier);
        Art.Mat lens = tier >= 5 ? LUMENITE : tier == 4 ? VOID : BLUE;
        double[] head = {-19, 38.5, -2};
        Shapes.beam(m, "Tripod_1", head, new double[]{-25, 26, 2}, 2, band);
        Shapes.beam(m, "Tripod_2", head, new double[]{-13, 26, 1}, 2, band);
        Shapes.beam(m, "Tripod_3", head, new double[]{-20, 26, -9}, 2, band);
        m.add(new Art.Box("Tripod_Head", head[0], head[1], head[2], 4, 3, 4, band));
        double[] pivot = {-19, 41, -2};
        double[] q = Shapes.towards(new double[]{0, 1, 0}, Art.norm(new double[]{-0.74, 0.6, 0.3}));
        Shapes.segment(m, "Scope_Eyepiece", pivot, q, -12, -9, 3, band);
        Shapes.segment(m, "Scope_Drawtube", pivot, q, -9, -2, 4, band);
        Shapes.segment(m, "Scope_Tube", pivot, q, -2, 10, 6, tube);
        Shapes.segment(m, "Scope_Band_1", pivot, q, 0, 1, 7, band);
        Shapes.segment(m, "Scope_Band_2", pivot, q, 7, 8, 7, band);
        Shapes.segment(m, "Scope_Hood", pivot, q, 10, 13, 8, band);
        double[] lensAt = Blocky.add(pivot, Art.rotate(new double[]{0, 13, 0}, q));
        Shapes.disc(m, "Scope_Lens", lensAt, 6, 0, 1, Blocky.mul(q, Art.quaternion(-90, 0, 0)), lens, lens.style() == Art.Style.GLOW);
    }

    /** Tiers 3 and 4: a lamp on an arm from the right post, a crystal glowing over the grindstone. */
    static void lamp(Art.Model m, int tier) {
        Art.Mat band = instrument(tier);
        Shapes.beam(m, "Lamp_Arm", new double[]{29, 53, -12}, new double[]{14, 53, 1}, 2, band);
        m.add(new Art.Box("Lamp_Hood", 14, 52, 1, 7, 3, 7, band));
        m.add(new Art.Box("Lamp_Crystal", 14, 48.5, 1, 3, 4, 3, tier >= 4 ? VOID : LAMP).rot(0, 45, 0)).glow();
    }

    /** Tiers 4 and 5: a purple runner, void crystals on the board, a voidheart in a jar on the shelf. */
    static void voidTouched(Art.Model m, int tier) {
        m.add(new Art.Box("Runner", -10, 26.5, 1, 14, 1, 28, RUNNER));
        m.add(new Art.Box("Runner_Drape", -10, 21, 16.25, 14, 12, 2, RUNNER));
        m.add(new Art.Box("Runner_Hem", -10, 15.25, 16, 15, 1, 3, fitting(tier)));
        m.crystal("Void_1", -23, 57, -13.5, 4, 5, 0, 0, 10, VOID, true);
        m.crystal("Void_2", -18.5, 57, -13, 3, 3, 8, 0, -22, VOID, true);
        m.crystal("Void_3", -27, 57, -14, 3, 3, -6, 0, 30, VOID, true);
        m.add(new Art.Box("Jar", -18, 12.5, 3, 6, 9, 6, JAR));
        m.add(new Art.Box("Jar_Lid", -18, 17.5, 3, 7, 1, 7, fitting(tier)));
        m.add(new Art.Box("Voidheart", -18, 12.5, 3, 3, 3, 3, VOID).rot(45, 0, 45)).glow();
    }

    /** Tier 5: a lumenite crystal floating in two gold rings over the grindstone, lumenite on the board. */
    static void lumenite(Art.Model m) {
        double[] c = {14, 53, 1};
        int mark = m.boxes.size();
        Shapes.disc(m, "Orbit_1_Ring", c, 14, 12, 1, Art.quaternion(20, 0, 0), GOLD, false);
        m.group("Orbit_1", mark, c[0], c[1], c[2]);
        mark = m.boxes.size();
        Shapes.disc(m, "Orbit_2_Ring", c, 14, 12, 1, Art.quaternion(0, 70, 0), GOLD, false);
        m.group("Orbit_2", mark, c[0], c[1], c[2]);
        mark = m.boxes.size();
        m.add(new Art.Box("Lumenite_Core", c[0], c[1], c[2], 4, 7, 4, LUMENITE).rot(0, 45, 0)).glow();
        m.add(new Art.Box("Lumenite_Tip_Top", c[0], c[1] + 4.5, c[2], 2, 2, 2, LUMENITE).rot(0, 45, 0)).glow();
        m.add(new Art.Box("Lumenite_Tip_Bottom", c[0], c[1] - 4.5, c[2], 2, 2, 2, LUMENITE).rot(0, 45, 0)).glow();
        m.group("Lumenite", mark, c[0], c[1], c[2]);
        m.crystal("Lumenite_1", 23, 57, -13.5, 4, 5, 0, 0, -10, LUMENITE, true);
        m.crystal("Lumenite_2", 18.5, 57, -13, 3, 3, 8, 0, 22, LUMENITE, true);
        m.crystal("Lumenite_3", 27, 57, -14, 3, 3, -6, 0, -30, LUMENITE, true);
    }

    // ---------------------------------------------------------------- animations

    static final double[] X = {1, 0, 0};
    static final double[] Y = {0, 1, 0};
    static final double[] Z = {0, 0, 1};

    /** A node turning {@code turns} times per loop about an axis, and rising by {@code rise} halfway through. */
    record Motion(String node, double[] axis, int turns, double rise) {
    }

    /** The crystal turns and floats, the rings turn about two other axes. */
    static List<Motion> lumeniteMotions() {
        return List.of(new Motion("Lumenite", Y, 1, 1.5), new Motion("Orbit_1", Y, 1, 0), new Motion("Orbit_2", X, 1, 0));
    }

    /** Writes a looping {@code .blockyanim}: a third of a turn per keyframe, like the game's campfire spit. */
    static void animation(String path, int duration, List<Motion> motions) throws IOException {
        Map<String, Object> nodes = new LinkedHashMap<>();
        for (Motion motion : motions) {
            List<Object> orientation = new ArrayList<>();
            int steps = 3 * motion.turns();
            for (int k = 0; k < steps; k++) {
                double half = Math.toRadians(120.0 * k) / 2, sin = Math.sin(half);
                double[] a = motion.axis();
                orientation.add(keyframe(duration * k / steps, ordered("x", round(a[0] * sin), "y", round(a[1] * sin), "z", round(a[2] * sin),
                        "w", round(Math.cos(half))), "linear"));
            }
            List<Object> position = new ArrayList<>();
            if (motion.rise() != 0) {
                position.add(keyframe(0, ordered("x", 0.0, "y", 0.0, "z", 0.0), "smooth"));
                position.add(keyframe(duration / 2, ordered("x", 0.0, "y", motion.rise(), "z", 0.0), "smooth"));
            }
            nodes.put(motion.node(), ordered("position", position, "orientation", orientation,
                    "shapeStretch", List.of(), "shapeVisible", List.of(), "shapeUvOffset", List.of()));
        }
        Map<String, Object> animation = ordered("formatVersion", 1, "duration", duration, "holdLastKeyframe", false, "nodeAnimations", nodes);
        Files.writeString(Art.COMMON.resolve(path), Json.write(animation), StandardCharsets.UTF_8);
    }

    private static double round(double v) {
        return Math.round(v * 1e6) / 1e6;
    }

    private static Map<String, Object> keyframe(int time, Map<String, Object> delta, String interpolation) {
        return ordered("time", time, "delta", delta, "interpolationType", interpolation);
    }

    private static Map<String, Object> ordered(Object... keysAndValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }
}
