import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Predicate;

/**
 * The goggles. Tier I of each kind stays plain on purpose; every later tier adds parts and better
 * materials, and the UltraGoggles get everything:
 * <ul>
 *   <li>Zoom II: round iron rims, barrels with a focus ring, a flip-up loupe;</li>
 *   <li>Zoom III: thorium, telescoping barrels with gold bands, a forehead plate with a crystal, a
 *   rangefinder at the temple;</li>
 *   <li>Zoom IV: adamantite, a long three-stage telescope over one eye and a short one over the other,
 *   void lenses, gears at the temples, a void crystal crest;</li>
 *   <li>Night Vision II: rubber eyecups in iron rims, an infrared lamp on the forehead;</li>
 *   <li>Night Vision III: binocular tubes hanging from a forehead mount, a battery at the back;</li>
 *   <li>Night Vision IV: four tubes, the outer ones turned outwards, a crest with a void crystal;</li>
 *   <li>UltraGoggles: a gold visor glowing like lumenite, a scope, night vision tubes flipped up, a
 *   lumenite crest.</li>
 * </ul>
 * The same builders put the goggles on the helms, there without the strap and in front of the helmet.
 *
 * <p>Head-centre coordinates: on a bare head the face is at z = +14 and the eyes at (+-7.5, -1.8).
 */
final class Gear {
    enum Kind { ZOOM, NIGHT, ULTRA }

    static final double LENS_Y = -2;

    static final Art.Mat COPPER = Art.Mat.of("#c06a35", Art.Style.METAL);
    static final Art.Mat IRON = Art.Mat.tex("BlockTextures/Metal_Iron_Smooth.png");
    static final Art.Mat THORIUM = Art.Mat.tint("BlockTextures/Metal_Iron_Smooth.png", "#7fae68");
    static final Art.Mat ADAMANTITE = Art.Mat.tint("BlockTextures/Metal_Iron_Smooth.png", "#c8484a");
    static final Art.Mat STEEL = Art.Mat.tint("BlockTextures/Metal_Iron_Smooth.png", "#5d6570");
    static final Art.Mat GOLD = Art.Mat.tex("BlockTextures/Rock_Gold_Smooth_Top.png");
    static final Art.Mat RUBBER = Art.Mat.of("#2f3337", Art.Style.LEATHER);
    static final Art.Mat BROWN = Art.Mat.of("#5a3a22", Art.Style.LEATHER);
    static final Art.Mat DARK_LEATHER = Art.Mat.of("#3a2a20", Art.Style.LEATHER);
    static final Art.Mat BLACK_LEATHER = Art.Mat.of("#25211f", Art.Style.LEATHER);
    static final Art.Mat VELVET = Art.Mat.of("#3a1f52", Art.Style.LEATHER);
    static final Art.Mat CLEAR = Art.Mat.of("#cfe3ea", Art.Style.GLASS);
    static final Art.Mat BLUE = Art.Mat.of("#9fd3ff", Art.Style.GLASS);
    static final Art.Mat AMBER = Art.Mat.of("#ffcf5e", Art.Style.GLASS);
    static final Art.Mat VOID = Art.Mat.of("#b98cff", Art.Style.GLOW);
    static final Art.Mat NIGHT = Art.Mat.of("#5cff6a", Art.Style.GLOW);
    static final Art.Mat LAMP = Art.Mat.of("#ffb347", Art.Style.GLOW);
    static final Art.Mat CRYSTAL = Art.Mat.of("#ffe27a", Art.Style.GLOW);
    static final Art.Mat LUMENITE = Art.Mat.of("#57f0ff", Art.Style.GLOW);

    private Gear() {
    }

    static void all() throws IOException {
        String dir = "Items/FlexiisGoggles/";
        // the icons show the goggles from the front, without what runs round the head
        Predicate<Art.Box> front = box -> !box.name.startsWith("Strap_") && !box.name.startsWith("Back_") && !box.name.startsWith("Temple_");
        for (int tier = 1; tier <= 4; tier++) {
            write(dir + "Goggles_Zoom_" + tier, "Flexii_Goggles_Zoom_" + tier, build(Kind.ZOOM, tier, bare(), 7.5, true), front);
            write(dir + "Goggles_NightVision_" + tier, "Flexii_Goggles_NightVision_" + tier, build(Kind.NIGHT, tier, bare(), 7.5, true), front);
        }
        write(dir + "Goggles_Ultra", "Flexii_Goggles_Ultra", build(Kind.ULTRA, 5, bare(), 7.5, true), front);
        Helms.all();
    }

    static void write(String path, String iconId, Art.Model model, Predicate<Art.Box> inIcon) throws IOException {
        Art.Model m = new Art.Model(path, true);
        m.boxes.addAll(model.boxes);
        m.write();
        BufferedImage texture = m.texture(path.hashCode());
        Art.png(texture, path + "_Texture.png");
        Art.Model shown = new Art.Model(path, true);
        m.boxes.stream().filter(inIcon).forEach(shown.boxes::add);
        Art.png(Art.icon(shown, texture, 28, 14), "Icons/ItemsGenerated/" + iconId + ".png");
    }

    /** A bare head: the face is flat at z = 14. */
    static DoubleBinaryOperator bare() {
        return (x, y) -> 14;
    }

    /**
     * Goggles of a kind and tier with their lenses {@code lensX} to each side, in front of a face that
     * reaches {@code front(x, y)} forward; with the strap round the head or without (on a helm).
     */
    static Art.Model build(Kind kind, int tier, DoubleBinaryOperator front, double lensX, boolean strap) {
        Art.Model m = new Art.Model("unused", true);
        switch (kind) {
            case ZOOM -> {
                switch (tier) {
                    case 1 -> zoom1(m, front, lensX);
                    case 2 -> zoom2(m, front, lensX);
                    case 3 -> zoom3(m, front, lensX);
                    default -> zoom4(m, front, lensX);
                }
            }
            case NIGHT -> {
                switch (tier) {
                    case 1 -> night1(m, front, lensX);
                    case 2 -> night2(m, front, lensX);
                    case 3 -> night3(m, front, lensX);
                    default -> night4(m, front, lensX);
                }
            }
            default -> ultra(m, front, lensX);
        }
        if (strap) {
            strap(m, kind, tier);
        }
        return m;
    }

    // ---------------------------------------------------------------- zoom

    /** Tier I: square copper frames with short barrels, as plain as goggles get. */
    static void zoom1(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        m.pair("Frame", lx, LENS_Y, f + 1.5, 10, 9, 2, COPPER);
        m.add(new Art.Box("Bridge", 0, -1, f + 1.5, 5, 2, 2, COPPER));
        m.pair("Tube", lx, LENS_Y, f + 5, 7, 7, 5, COPPER);
        m.pair("Ring", lx, LENS_Y, f + 7.75, 8, 8, 1, new Art.Mat(Art.shade(COPPER.rgb(), 1.12), Art.Style.METAL));
        m.pair("Lens", lx, LENS_Y, f + 8.5, 5, 5, 1, CLEAR);
    }

    /** Tier II: round iron rims, barrels with a dark focus ring, a loupe flipped up over the right eye. */
    static void zoom2(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        for (int side = -1; side <= 1; side += 2) {
            String s = side(side);
            double x = lx * side;
            ring(m, "Rim" + s, x, LENS_Y, f + 1.5, 12, 8, 2, IRON);
            disc(m, "Barrel" + s, x, LENS_Y, f + 4.5, 9, 4, IRON);
            ring(m, "Focus" + s, x, LENS_Y, f + 4.5, 10, 8, 1, STEEL);
            ring(m, "Bezel" + s, x, LENS_Y, f + 7, 10, 6, 1, IRON);
            lens(m, "Lens" + s, x, LENS_Y, f + 6.75, 6, BLUE);
            rivets(m, "Rivet" + s, x, LENS_Y, f + 2.75, 5.2, STEEL);
        }
        m.add(new Art.Box("Bridge", 0, LENS_Y + 1.25, f + 1.5, 4, 2, 1, IRON));
        // the loupe, flipped up on a hinge above the right rim
        double hx = lx + 2, hy = LENS_Y + 6, hz = f + 2;
        m.add(new Art.Box("Loupe_Hinge", hx, hy + 0.25, hz, 3, 2, 2, STEEL));
        double[] tipped = Art.quaternion(-62, 0, 0);
        double[] at = {hx, hy + 4.5, hz + 2.5};
        Shapes.beam(m, "Loupe_Arm", new double[]{hx, hy + 0.5, hz}, at, 1, IRON);
        Shapes.disc(m, "Loupe_Rim", at, 7, 5, 1, tipped, IRON, false);
        Shapes.disc(m, "Loupe_Lens", at, 5, 0, 1, tipped, BLUE, false);
    }

    /** Tier III: thorium with gold bands, telescoping barrels, a crystal on the forehead, a rangefinder. */
    static void zoom3(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        for (int side = -1; side <= 1; side += 2) {
            String s = side(side);
            double x = lx * side;
            ring(m, "Rim" + s, x, LENS_Y, f + 1.5, 13, 9, 2, THORIUM);
            rivets(m, "Rivet" + s, x, LENS_Y, f + 2.75, 5.8, GOLD);
            disc(m, "Barrel" + s, x, LENS_Y, f + 4.5, 10, 4, THORIUM);
            ring(m, "Band" + s, x, LENS_Y, f + 5, 11, 9, 1, GOLD);
            disc(m, "Draw" + s, x, LENS_Y, f + 8.5, 8, 4, STEEL);
            ring(m, "Bezel" + s, x, LENS_Y, f + 11, 9, 7, 1, GOLD);
            lens(m, "Lens" + s, x, LENS_Y, f + 10.75, 7, AMBER);
        }
        m.add(new Art.Box("Bridge", 0, LENS_Y + 1.25, f + 1.5, 4, 2, 1, GOLD));
        // a plate on the forehead with a glowing crystal in a gold setting
        double py = LENS_Y + 8.5, pf = front.applyAsDouble(0, py);
        m.add(new Art.Box("Brow_Plate", 0, py, pf + 0.5, 14, 4, 2, THORIUM));
        m.pair("Brow_Fin", 8.5, py + 0.75, pf + 0.75, 4, 2, 2, GOLD);
        ring(m, "Brow_Setting", 0, py, pf + 2, 8, 4, 1, GOLD);
        lens(m, "Brow_Crystal", 0, py, pf + 2, 4, CRYSTAL);
        // a rangefinder along the right temple
        double rx = 15.5, ry = LENS_Y + 3;
        m.add(new Art.Box("Temple_Rangefinder", rx, ry, f - 3, 4, 4, 12, THORIUM));
        ring(m, "Temple_Rangefinder_Bezel", rx, ry, f + 3.25, 5, 3, 1, GOLD);
        lens(m, "Temple_Rangefinder_Lens", rx, ry, f + 3.25, 3, CRYSTAL);
    }

    /** Tier IV: adamantite, a long telescope over the right eye, a short one over the left, gears, a crest. */
    static void zoom4(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        for (int side = -1; side <= 1; side += 2) {
            String s = side(side);
            double x = lx * side;
            ring(m, "Rim" + s, x, LENS_Y, f + 1.5, 13, 9, 2, ADAMANTITE);
            rivets(m, "Rivet" + s, x, LENS_Y, f + 2.75, 6.2, GOLD);
            disc(m, "Barrel" + s, x, LENS_Y, f + 4.5, 11, 4, ADAMANTITE);
            ring(m, "Band" + s, x, LENS_Y, f + 4.5, 12, 10, 1, STEEL);
        }
        // right: three stages
        double x = lx;
        disc(m, "Draw_R", x, LENS_Y, f + 8.5, 9, 4, STEEL);
        ring(m, "Draw_Band_R", x, LENS_Y, f + 8.5, 10, 8, 1, GOLD);
        disc(m, "Tip_R", x, LENS_Y, f + 12.5, 7, 4, ADAMANTITE);
        ring(m, "Bezel_R", x, LENS_Y, f + 15, 8, 6, 1, GOLD);
        lens(m, "Lens_R", x, LENS_Y, f + 14.75, 6, VOID);
        // left: one stage
        ring(m, "Bezel_L", -x, LENS_Y, f + 7, 11, 7, 1, GOLD);
        lens(m, "Lens_L", -x, LENS_Y, f + 7, 7, VOID);
        m.add(new Art.Box("Bridge", 0, LENS_Y + 1.25, f + 1.5, 4, 2, 1, GOLD));
        // gears at the temples
        for (int side = -1; side <= 1; side += 2) {
            gear(m, "Temple_Gear" + side(side), new double[]{15.75 * side, LENS_Y + 1, 5}, 9, Art.quaternion(0, 90, 0), ADAMANTITE, GOLD);
        }
        // a crest with a void crystal
        double cy = LENS_Y + 8.5, cf = front.applyAsDouble(0, cy);
        m.add(new Art.Box("Crest_Plate", 0, cy, cf + 0.5, 10, 6, 2, ADAMANTITE));
        m.add(new Art.Box("Crest_Wing_L", -6.5, cy + 1, cf + 0.5, 4, 3, 2, GOLD).rot(0, 0, 20));
        m.add(new Art.Box("Crest_Wing_R", 6.5, cy + 1, cf + 0.5, 4, 3, 2, GOLD).rot(0, 0, -20));
        m.crystal("Crest_Crystal", 0, cy - 1, cf + 2, 3, 6, 10, 0, 0, VOID, true);
    }

    // ---------------------------------------------------------------- night vision

    /** Tier I: square copper frames, dark hoods, green lenses, a little emitter. */
    static void night1(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        Art.Mat accent = Art.Mat.of("#2b2f33", Art.Style.METAL);
        m.pair("Frame", lx, LENS_Y, f + 1.5, 10, 9, 2, COPPER);
        m.add(new Art.Box("Bridge", 0, -1, f + 1.5, 5, 2, 2, COPPER));
        m.pair("Hood", lx, LENS_Y, f + 3.5, 9, 8, 2, accent);
        m.add(new Art.Box("Lens_L", -lx, LENS_Y, f + 4.75, 6, 6, 1, NIGHT)).glow();
        m.add(new Art.Box("Lens_R", lx, LENS_Y, f + 4.75, 6, 6, 1, NIGHT)).glow();
        m.add(new Art.Box("Emitter", 0, 3.5, f + 2, 4, 3, 3, accent));
        m.add(new Art.Box("Emitter_Light", 0, 3.5, f + 3.75, 2, 2, 1, NIGHT)).glow();
    }

    /** Tier II: rubber eyecups in iron rims, an infrared lamp on the forehead. */
    static void night2(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        for (int side = -1; side <= 1; side += 2) {
            String s = side(side);
            double x = lx * side;
            ring(m, "Rim" + s, x, LENS_Y, f + 1.5, 14, 12, 1, IRON);
            ring(m, "Cup" + s, x, LENS_Y, f + 2, 12, 8, 3, RUBBER);
            lens(m, "Lens" + s, x, LENS_Y, f + 2.5, 8, NIGHT);
            rivets(m, "Rivet" + s, x, LENS_Y, f + 2.5, 6, IRON);
        }
        m.add(new Art.Box("Bridge", 0, LENS_Y + 1.25, f + 2, 4, 2, 1, IRON));
        double ly = LENS_Y + 8, lf = front.applyAsDouble(0, ly);
        m.add(new Art.Box("Lamp", 0, ly, lf + 1.5, 6, 4, 4, IRON));
        ring(m, "Lamp_Bezel", 0, ly, lf + 3.5, 5, 3, 1, STEEL);
        lens(m, "Lamp_Light", 0, ly, lf + 3.25, 3, NIGHT);
    }

    /** Tier III: binocular tubes hanging from a forehead mount; a battery at the back. */
    static void night3(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        double my = LENS_Y + 9, mf = front.applyAsDouble(0, my);
        m.add(new Art.Box("Mount", 0, my, mf + 1, 12, 5, 3, THORIUM));
        m.add(new Art.Box("Mount_Knob", 0, my + 1.25, mf + 3, 3, 3, 2, GOLD));
        Shapes.beam(m, "Mount_Arm", new double[]{0, my - 1, mf + 2}, new double[]{0, LENS_Y + 3.5, f + 5}, 3, STEEL);
        m.add(new Art.Box("Housing", 0, LENS_Y + 1.25, f + 5, 9, 5, 5, STEEL));
        for (int side = -1; side <= 1; side += 2) {
            String s = side(side);
            double x = lx * side;
            ring(m, "Cup" + s, x, LENS_Y, f + 1, 10, 6, 2, RUBBER);
            disc(m, "Tube" + s, x, LENS_Y, f + 5, 10, 6, THORIUM);
            ring(m, "Band" + s, x, LENS_Y, f + 4, 11, 9, 1, GOLD);
            ring(m, "Bezel" + s, x, LENS_Y, f + 8.5, 11, 9, 1, STEEL);
            lens(m, "Lens" + s, x, LENS_Y, f + 8.5, 9, NIGHT);
        }
    }

    /** Tier IV: four tubes, the outer ones turned outwards, on an adamantite crest with a void crystal. */
    static void night4(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        double my = LENS_Y + 9, mf = front.applyAsDouble(0, my);
        m.add(new Art.Box("Crest_Plate", 0, my, mf + 1, 16, 6, 3, ADAMANTITE));
        m.add(new Art.Box("Crest_Wing_L", -9, my + 1.5, mf + 1, 5, 3, 3, GOLD).rot(0, 0, 25));
        m.add(new Art.Box("Crest_Wing_R", 9, my + 1.5, mf + 1, 5, 3, 3, GOLD).rot(0, 0, -25));
        ring(m, "Crest_Setting", 0, my, mf + 3, 6, 4, 1, GOLD);
        lens(m, "Crest_Crystal", 0, my, mf + 3, 4, VOID);
        Shapes.beam(m, "Mount_Arm", new double[]{0, my - 2, mf + 2.5}, new double[]{0, LENS_Y + 3, f + 4}, 3, STEEL);
        m.add(new Art.Box("Housing", 0, LENS_Y + 1.5, f + 4.25, 17, 4, 4, STEEL));
        double inner = Math.max(5, lx - 2.5);
        for (int side = -1; side <= 1; side += 2) {
            String s = side(side);
            double x = inner * side;
            ring(m, "Cup" + s, lx * side, LENS_Y, f + 1, 11, 7, 2, RUBBER);
            disc(m, "Tube" + s, x, LENS_Y, f + 5.5, 8, 7, ADAMANTITE);
            ring(m, "Bezel" + s, x, LENS_Y, f + 9.5, 8, 6, 1, GOLD);
            lens(m, "Lens" + s, x, LENS_Y, f + 9.5, 6, NIGHT);
            // the outer tubes look out to the side
            double[] q = Art.quaternion(0, 28 * side, 0);
            double[] base = {(inner + 8) * side, LENS_Y, f + 3.5};
            Shapes.disc(m, "Side_Tube" + s, base, 7, 0, 6, q, ADAMANTITE, false);
            double[] tip = Blocky.add(base, Art.rotate(new double[]{0, 0, 3.5}, q));
            Shapes.disc(m, "Side_Bezel" + s, tip, 7, 5, 1, q, GOLD, false);
            Shapes.disc(m, "Side_Lens" + s, tip, 5, 0, 1, q, NIGHT, true);
        }
    }

    // ---------------------------------------------------------------- ultra

    /** A gold visor glowing like lumenite, a scope over the right eye, night vision flipped up, a crest. */
    static void ultra(Art.Model m, DoubleBinaryOperator front, double lx) {
        double f = front.applyAsDouble(lx, LENS_Y);
        double w = 2 * lx + 14;
        m.add(new Art.Box("Visor_Glass", 0, LENS_Y, f + 1.5, (int) w - 6, 7, 1, LUMENITE)).glow();
        m.add(new Art.Box("Visor_Top", 0, LENS_Y + 4.5, f + 2, (int) w, 2, 3, GOLD));
        m.add(new Art.Box("Visor_Bottom", 0, LENS_Y - 4.5, f + 2, (int) w, 2, 3, GOLD));
        m.pair("Visor_Side", w / 2 - 1.5, LENS_Y, f + 2, 3, 7, 3, GOLD);
        // the scope over the right eye
        double x = lx + 1;
        disc(m, "Scope_Base", x, LENS_Y, f + 5.5, 9, 4, GOLD);
        ring(m, "Scope_Band", x, LENS_Y, f + 5.5, 10, 8, 1, VELVET);
        disc(m, "Scope_Draw", x, LENS_Y, f + 9.5, 7, 4, STEEL);
        ring(m, "Scope_Bezel", x, LENS_Y, f + 12, 8, 6, 1, GOLD);
        lens(m, "Scope_Lens", x, LENS_Y, f + 11.75, 6, LUMENITE);
        // night vision tubes, flipped up on a hinge above the visor
        double hy = LENS_Y + 6.5, hf = front.applyAsDouble(0, hy);
        m.add(new Art.Box("Hinge", 0, hy, hf + 2, 14, 2, 3, GOLD));
        double[] up = Art.quaternion(-75, 0, 0);
        for (int side = -1; side <= 1; side += 2) {
            double[] base = {4.5 * side, hy + 4, hf + 2.5};
            Shapes.disc(m, "Up_Tube" + side(side), base, 6, 0, 6, up, STEEL, false);
            double[] tip = Blocky.add(base, Art.rotate(new double[]{0, 0, 3.5}, up));
            Shapes.disc(m, "Up_Bezel" + side(side), tip, 6, 4, 1, up, GOLD, false);
            Shapes.disc(m, "Up_Lens" + side(side), tip, 4, 0, 1, up, NIGHT, true);
        }
        // a lumenite crest between the tubes
        m.crystal("Crest_1", 0, hy + 1, hf + 1.5, 3, 7, 6, 0, 0, LUMENITE, true);
        m.crystal("Crest_2", -2.5, hy + 1, hf + 1, 2, 4, 4, 0, 18, LUMENITE, true);
        m.crystal("Crest_3", 2.5, hy + 1, hf + 1, 2, 4, 4, 0, -18, LUMENITE, true);
        // gold discs with a glow at the temples
        for (int side = -1; side <= 1; side += 2) {
            double[] at = {15.75 * side, LENS_Y + 1, 4};
            Shapes.disc(m, "Temple_Disc" + side(side), at, 8, 4, 1, Art.quaternion(0, 90, 0), GOLD, false);
            Shapes.disc(m, "Temple_Core" + side(side), at, 4, 0, 1, Art.quaternion(0, 90, 0), LUMENITE, true);
        }
    }

    // ---------------------------------------------------------------- straps and parts

    /** The strap round the head; buckles from tier II, studs from tier III. */
    static void strap(Art.Model m, Kind kind, int tier) {
        Art.Mat leather = kind == Kind.ULTRA ? VELVET : tier >= 4 ? BLACK_LEATHER : tier == 3 ? DARK_LEATHER : BROWN;
        Art.Mat metal = kind == Kind.ULTRA ? GOLD : switch (tier) {
            case 1 -> COPPER;
            case 2 -> IRON;
            case 3 -> GOLD;
            default -> ADAMANTITE;
        };
        m.pair("Strap_Side", 15.5, -1.25, 1, 1, 4, 26, leather);
        m.add(new Art.Box("Strap_Back", 0, -1.25, -14.5, 32, 4, 1, leather));
        m.pair("Strap_Front", 13.75, -1.25, 14.25, 3, 3, 1, leather);
        if (tier >= 2) {
            m.pair("Strap_Buckle", 16, -1.25, -6, 3, 6, 4, metal);
        }
        if (tier >= 3) {
            for (int i = 0; i < 3; i++) {
                m.add(new Art.Box("Strap_Stud_" + i, -6 + 6 * i, -1, -15.25, 2, 2, 1, metal));
            }
        }
        if (kind == Kind.NIGHT && tier >= 3) {
            // the battery pack at the back
            m.add(new Art.Box("Back_Battery", 0, -1, -16.25, 10, 6, 4, tier >= 4 ? ADAMANTITE : THORIUM));
            m.add(new Art.Box("Back_Battery_Light", 3, 0.5, -18.5, 2, 1, 1, tier >= 4 ? VOID : NIGHT)).glow();
            m.add(new Art.Box("Back_Battery_Light_2", 0, 0.5, -18.5, 2, 1, 1, NIGHT)).glow();
        }
    }

    static String side(int side) {
        return side < 0 ? "_L" : "_R";
    }

    static void ring(Art.Model m, String name, double x, double y, double z, int d, int inner, int t, Art.Mat mat) {
        Shapes.disc(m, name, new double[]{x, y, z}, d, inner, t, Art.quaternion(0, 0, 0), mat, false);
    }

    static void disc(Art.Model m, String name, double x, double y, double z, int d, int t, Art.Mat mat) {
        Shapes.disc(m, name, new double[]{x, y, z}, d, 0, t, Art.quaternion(0, 0, 0), mat, false);
    }

    static void lens(Art.Model m, String name, double x, double y, double z, int d, Art.Mat mat) {
        Shapes.disc(m, name, new double[]{x, y, z}, d, 0, 1, Art.quaternion(0, 0, 0), mat, mat.style() == Art.Style.GLOW);
    }

    /** Four rivets on a rim, at the corners of a square {@code r} from its centre. */
    static void rivets(Art.Model m, String name, double x, double y, double z, double r, Art.Mat mat) {
        double d = r / Math.sqrt(2);
        for (int i = 0; i < 4; i++) {
            m.add(new Art.Box(name + "_" + i, x + (i % 2 == 0 ? -d : d), y + (i < 2 ? d : -d), z, 1, 1, 1, mat));
        }
    }

    /** A gear: a disc with eight teeth and a hub, facing along q's z axis. */
    static void gear(Art.Model m, String name, double[] c, int d, double[] q, Art.Mat mat, Art.Mat hub) {
        Shapes.disc(m, name + "_Wheel", c, d, 0, 2, q, mat, false);
        Shapes.disc(m, name + "_Hub", c, 3, 0, 3, q, hub, false);
        for (int i = 0; i < 8; i++) {
            double a = i * 45;
            double[] off = Art.rotate(new double[]{(d / 2.0 + 0.5) * Math.cos(Math.toRadians(a)), (d / 2.0 + 0.5) * Math.sin(Math.toRadians(a)), 0}, q);
            m.add(new Art.Box(name + "_Tooth_" + i, c[0] + off[0], c[1] + off[1], c[2] + off[2], 2, 2, 1, mat)
                    .orient(Blocky.mul(q, Art.quaternion(0, 0, a))));
        }
    }
}
