import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Draws the mod's art: block models built from boxes, their pixel textures, and 64x64 icons rendered
 * from the models. Run from the repository root with {@code java dev/art/Art.java}; it rewrites
 * everything under {@code src/main/resources/Common/} that it owns.
 *
 * <p>Model format ({@code .blockymodel}): nodes with a box shape each, sized in model units, one texel
 * per unit. Worn items hang under a {@code Head} node and use 64 units per block. The head bone sits at
 * the neck; the root's offset (0, 20, 2) moves the children's origin to the centre of the 30x28x28 head
 * box, so the face is at z = +14 and the eyes at (+-7.5, -1.8). Blocks and items use 32 units per
 * block, origin at the bottom centre; an ingot is 26 units long.
 */
public final class Art {
    static final Path COMMON = Path.of("src/main/resources/Common");

    public static void main(String[] args) throws IOException {
        Gear.all();
        Materials.all();
        Optician.all();
        System.out.println("art written to " + COMMON);
    }

    // ---------------------------------------------------------------- colours and materials

    enum Style { METAL, LEATHER, GLASS, PANE, GLOW, WOOD, STONE, CRYSTAL, PLAIN, TEXTURE, CHART }

    /** A colour and how to paint it; {@code TEXTURE} copies a game block texture (Common/ path) instead. */
    record Mat(int rgb, Style style, String texture) {
        Mat(int rgb, Style style) {
            this(rgb, style, null);
        }

        static Mat of(String hex, Style style) {
            return new Mat(Integer.parseInt(hex.substring(1), 16), style);
        }

        /** One of the game's block textures, 32 texels per block like our models. */
        static Mat tex(String path) {
            return new Mat(0xffffff, Style.TEXTURE, path);
        }
    }

    private static final java.util.Map<String, BufferedImage> GAME_TEXTURES = new java.util.HashMap<>();

    /** A texture from the game's Assets.zip (Hypixel allows mod creators to use the game's assets). */
    static BufferedImage gameTexture(String path) {
        return GAME_TEXTURES.computeIfAbsent(path, key -> {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(Helms.assets().toFile())) {
                java.util.zip.ZipEntry entry = zip.getEntry("Common/" + key);
                if (entry == null) {
                    throw new IllegalArgumentException("not in Assets.zip: " + key);
                }
                try (java.io.InputStream in = zip.getInputStream(entry)) {
                    return ImageIO.read(in);
                }
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }

    static int shade(int rgb, double factor) {
        int r = (rgb >> 16) & 0xff, g = (rgb >> 8) & 0xff, b = rgb & 0xff;
        r = (int) Math.clamp(Math.round(r * factor), 0, 255);
        g = (int) Math.clamp(Math.round(g * factor), 0, 255);
        b = (int) Math.clamp(Math.round(b * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xff) * (1 - t) + ((b >> 16) & 0xff) * t);
        int g = (int) Math.round(((a >> 8) & 0xff) * (1 - t) + ((b >> 8) & 0xff) * t);
        int bl = (int) Math.round((a & 0xff) * (1 - t) + (b & 0xff) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Paints one face of {@code w} x {@code h} texels in the material's style. */
    static void paint(BufferedImage img, int u, int v, int w, int h, Mat mat, Random rnd) {
        if (mat.style() == Style.TEXTURE) {
            BufferedImage source = gameTexture(mat.texture());
            int sw = source.getWidth(), sh = source.getHeight();
            int ox = rnd.nextInt(sw), oy = rnd.nextInt(sh);
            // planks and grain run along the face's long side, like on the game's furniture
            boolean turned = h > w;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = (turned ? source.getRGB((ox + y) % sw, (oy + x) % sh) : source.getRGB((ox + x) % sw, (oy + y) % sh)) & 0xffffff;
                    img.setRGB(u + x, v + y, 0xff000000 | bevel(c, x, y, w, h));
                }
            }
            return;
        }
        if (mat.style() == Style.PANE) {
            // see-through glass: a light rim and a glint, the rest is cut out
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean rim = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                    boolean glint = !rim && x + y == Math.min(w, h) / 2 && x > 0;
                    int c = rim ? mix(mat.rgb(), 0xffffff, y == 0 ? 0.55 : 0.25) : glint ? mix(mat.rgb(), 0xffffff, 0.8) : 0;
                    img.setRGB(u + x, v + y, rim || glint ? 0xff000000 | c : 0);
                }
            }
            return;
        }
        if (mat.style() == Style.CHART) {
            chart(img, u, v, w, h);
            return;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                int c = mat.rgb();
                double n = rnd.nextDouble();
                switch (mat.style()) {
                    case METAL -> {
                        c = shade(c, 0.94 + n * 0.1);
                        if (y == 0) c = shade(mat.rgb(), 1.22);
                        else if (edge) c = shade(mat.rgb(), 0.72);
                        if (!edge && n > 0.97) c = shade(mat.rgb(), 1.3);
                    }
                    case LEATHER -> {
                        c = shade(c, 0.88 + n * 0.18);
                        if (edge && (y == 0 || y == h - 1)) c = shade(mat.rgb(), 0.7);
                        if (!edge && (x + y) % 5 == 0 && n > 0.5) c = shade(mat.rgb(), 0.82);
                    }
                    case WOOD -> {
                        double grain = Math.sin((y * 1.7 + Math.sin(x * 0.35) * 1.4)) * 0.5 + 0.5;
                        c = shade(c, 0.85 + grain * 0.15 + n * 0.06);
                        if (edge) c = shade(mat.rgb(), 0.72);
                    }
                    case STONE -> {
                        c = shade(c, 0.82 + n * 0.3);
                        if (edge && n > 0.4) c = shade(mat.rgb(), 0.75);
                    }
                    case GLASS -> {
                        double t = (double) (x + y) / Math.max(1, w + h - 2);
                        c = mix(shade(mat.rgb(), 1.12), shade(mat.rgb(), 0.82), t);
                        if (x + y == Math.min(w, h) / 2 || x + y == Math.min(w, h) / 2 + 1) c = mix(c, 0xffffff, 0.75);
                        if (edge) c = shade(mat.rgb(), 0.7);
                    }
                    case GLOW -> {
                        double cx = (w - 1) / 2.0, cy = (h - 1) / 2.0;
                        double d = Math.hypot((x - cx) / Math.max(1, cx), (y - cy) / Math.max(1, cy));
                        c = mix(mix(mat.rgb(), 0xffffff, 0.55), mat.rgb(), Math.min(1, d));
                        if (edge) c = shade(mat.rgb(), 0.8);
                    }
                    case CRYSTAL -> {
                        double t = (double) x / Math.max(1, w - 1);
                        c = mix(mix(mat.rgb(), 0xffffff, 0.25), shade(mat.rgb(), 0.95), t);
                        if (!edge && (x == w / 3 || (w > 4 && x == 2 * w / 3 && n > 0.5))) c = mix(c, 0xffffff, 0.7);
                        if (edge) c = shade(mat.rgb(), 0.86);
                    }
                    case PLAIN -> c = shade(c, 0.97 + n * 0.06);
                }
                img.setRGB(u + x, v + y, 0xff000000 | c);
            }
        }
    }

    /** Lighter top and left rim, darker bottom and right rim: the game's bevelled look. */
    static int bevel(int c, int x, int y, int w, int h) {
        if (w < 3 || h < 3) {
            return c;
        }
        if (y == 0) return shade(c, 1.16);
        if (y == h - 1) return shade(c, 0.74);
        if (x == 0) return shade(c, 1.06);
        if (x == w - 1) return shade(c, 0.84);
        return c;
    }

    /**
     * An optician's eye chart on cream paper in a dark frame: a big E, then rows of ever smaller
     * letters, a line of fine print and, if there is room, the red-green bar of the duochrome test.
     */
    static void chart(BufferedImage img, int u, int v, int w, int h) {
        int frame = 0x4a3826, paper = 0xeee6cf, ink = 0x26252a, red = 0xc0463c, green = 0x4c9a48;
        Random grain = new Random(w * 31L + h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean border = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                img.setRGB(u + x, v + y, 0xff000000 | (border ? frame : shade(paper, 0.97 + grain.nextDouble() * 0.05)));
            }
        }
        String[] big = {"1111111", "1111111", "11", "111111", "11", "1111111", "1111111"};
        String[][] medium = {{"11111", "1", "1111", "1", "11111"}, {"11111", "1", "1111", "1", "1"}, {"1111", "1   1", "1111", "1", "1"},
                {"11111", "  1", "  1", "  1", "  1"}, {" 111", "1   1", "1   1", "1   1", " 111"}, {"1111", "1   1", "1   1", "1   1", "1111"}};
        String[][] small = {{"111", "11", "111"}, {"111", "1 1", "111"}, {"111", " 1", " 1"}, {"11", "1 1", "11"}, {"111", "11", "1"}};
        int y = 2;
        int bigSize = h >= 24 ? 7 : 5;
        int g = 0;
        // the big E, cut down to 5 rows on small charts
        String[] e = bigSize == 7 ? big : new String[]{"11111", "1", "1111", "1", "11111"};
        y = glyphRow(img, u, v, w, y, new String[][]{e}, ink, 1) + 1;
        if (y + 5 <= h - 2) {
            y = glyphRow(img, u, v, w, y, new String[][]{medium[g++ % medium.length], medium[g++ % medium.length]}, ink, 1) + 1;
        }
        if (y + 3 <= h - 2) {
            y = glyphRow(img, u, v, w, y, new String[][]{small[0], small[1], small[2]}, ink, 1) + 1;
        }
        if (y + 1 <= h - 3) {
            for (int x = 3; x < w - 3; x++) {
                if (x % 2 == 1 || x % 5 == 0) img.setRGB(u + x, v + y, 0xff000000 | shade(ink, 1.6));
            }
            y += 2;
        }
        if (y + 1 <= h - 2) {
            for (int x = 2; x < w - 2; x++) {
                img.setRGB(u + x, v + h - 2, 0xff000000 | (x < w / 2 ? red : green));
            }
        }
    }

    /** One centred row of glyphs (strings of '1' per pixel row), {@code gap} apart; returns the row below it. */
    private static int glyphRow(BufferedImage img, int u, int v, int w, int y, String[][] glyphs, int ink, int gap) {
        int width = 0, height = 0;
        for (String[] glyph : glyphs) {
            int gw = 0;
            for (String row : glyph) gw = Math.max(gw, row.length());
            width += gw + gap;
            height = Math.max(height, glyph.length);
        }
        width -= gap;
        int x = (w - width + 1) / 2;
        for (String[] glyph : glyphs) {
            int gw = 0;
            for (int r = 0; r < glyph.length; r++) {
                gw = Math.max(gw, glyph[r].length());
                for (int c = 0; c < glyph[r].length(); c++) {
                    if (glyph[r].charAt(c) == '1') img.setRGB(u + x + c, v + y + r, 0xff000000 | ink);
                }
            }
            x += gw + gap;
        }
        return y + height;
    }

    /**
     * One picture shared by several faces, painted once: the round face of a disc built from pixel rows
     * shows one round picture (a wheel's grooves, a lens' glint) instead of one strip per row.
     */
    record Decal(String key, int size, Mat mat) {
    }

    /** A node without a shape of its own: its boxes turn together around (x, y, z). */
    record Group(String name, double x, double y, double z) {
    }

    /** Paints a round decal: a wheel for textures, a lens with a glint for glass, a glow for light. */
    static void paintDecal(BufferedImage img, int u, int v, Decal decal, Random rnd) {
        int d = decal.size();
        Mat mat = decal.mat();
        double r = d / 2.0;
        BufferedImage source = mat.style() == Style.TEXTURE ? gameTexture(mat.texture()) : null;
        int ox = source != null ? rnd.nextInt(source.getWidth()) : 0, oy = source != null ? rnd.nextInt(source.getHeight()) : 0;
        for (int y = 0; y < d; y++) {
            for (int x = 0; x < d; x++) {
                double dx = x + 0.5 - r, dy = y + 0.5 - r;
                double t = Math.hypot(dx, dy) / r;
                int c;
                switch (mat.style()) {
                    case TEXTURE -> {
                        c = source.getRGB((ox + x) % source.getWidth(), (oy + y) % source.getHeight()) & 0xffffff;
                        if (t > 0.84) {
                            c = shade(c, dx + dy < 0 ? 0.92 : 0.7);
                        } else if (Math.abs(t - 0.6) < 0.07 || (d >= 10 && Math.abs(t - 0.32) < 0.06)) {
                            c = shade(c, 0.84);
                        }
                    }
                    case GLASS -> {
                        c = mix(mix(mat.rgb(), 0xffffff, 0.35), shade(mat.rgb(), 0.86), t);
                        if (t > 0.8) {
                            c = shade(mat.rgb(), 0.8);
                        }
                        if (Math.hypot(dx + r * 0.38, dy + r * 0.38) < Math.max(0.75, r * 0.22)) {
                            c = mix(c, 0xffffff, 0.85);
                        }
                    }
                    case GLOW -> c = mix(mix(mat.rgb(), 0xffffff, 0.6), mat.rgb(), Math.min(1, t * 1.1));
                    default -> c = t > 0.84 ? shade(mat.rgb(), 0.75) : shade(mat.rgb(), 1.05 - 0.2 * t);
                }
                img.setRGB(u + x, v + y, 0xff000000 | c);
            }
        }
    }

    // ---------------------------------------------------------------- geometry

    static final class Box {
        final String name;
        final double x, y, z;
        final int sx, sy, sz;
        final Mat mat;
        double rx, ry, rz;
        String shading = "standard";
        /** u, v of each face: front, back, left, right, top, bottom. */
        final int[][] uv = new int[6][2];
        /** Materials for single faces, overriding {@link #mat}. */
        final Mat[] faces = new Mat[6];
        /** Decals for single faces, and where in the decal each face starts. */
        final Decal[] decals = new Decal[6];
        final int[][] decalAt = new int[6][2];

        Box(String name, double x, double y, double z, int sx, int sy, int sz, Mat mat) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.mat = mat;
        }

        /** An orientation given directly, instead of the angles. */
        double[] q;
        /** The node holding this box, if not the root. */
        String group;

        Box rot(double rx, double ry, double rz) {
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            return this;
        }

        Box orient(double[] q) {
            this.q = q;
            return this;
        }

        double[] rotation() {
            return this.q != null ? this.q : quaternion(this.rx, this.ry, this.rz);
        }

        Box glow() {
            this.shading = "fullbright";
            return this;
        }

        /** Another material for one face: 0 front, 1 back, 2 left, 3 right, 4 top, 5 bottom. */
        Box face(int face, Mat mat) {
            this.faces[face] = mat;
            return this;
        }

        /** Shows the part of a shared decal that starts at (du, dv) on one face. */
        Box decal(int face, Decal decal, int du, int dv) {
            this.decals[face] = decal;
            this.decalAt[face][0] = du;
            this.decalAt[face][1] = dv;
            return this;
        }

        int[] faceSize(int face) {
            return switch (face) {
                case 0, 1 -> new int[]{this.sx, this.sy};
                case 2, 3 -> new int[]{this.sz, this.sy};
                default -> new int[]{this.sx, this.sz};
            };
        }
    }

    static final class Model {
        final String path;
        final boolean head;
        final List<Box> boxes = new ArrayList<>();
        int texW, texH;
        /** Nodes that hold boxes, by name. */
        final java.util.Map<String, Group> groups = new java.util.LinkedHashMap<>();
        /** Where each decal went in the texture, and the decals by key. */
        final java.util.Map<String, int[]> decals = new java.util.LinkedHashMap<>();
        final java.util.Map<String, Decal> decalOf = new java.util.LinkedHashMap<>();
        /** The root node's name and position; benches two blocks wide sit one half block to the side. */
        String rootName;
        double[] rootPosition = {0, 0, 0};

        /** @param path relative to Common/, without extension */
        Model(String path, boolean head) {
            this.path = path;
            this.head = head;
        }

        Box add(Box box) {
            this.boxes.add(box);
            return box;
        }

        /** Mirrors a box to the other side of x = 0. */
        void pair(String name, double x, double y, double z, int sx, int sy, int sz, Mat mat) {
            this.add(new Box(name + "_L", -x, y, z, sx, sy, sz, mat));
            this.add(new Box(name + "_R", x, y, z, sx, sy, sz, mat));
        }

        /**
         * A pointed crystal standing on (x, y, z): a prism {@code w} wide and {@code h} long with a
         * stepped tip, its axis turned from +y by the angles.
         */
        void crystal(String name, double x, double y, double z, int w, int h, double rx, double ry, double rz, Mat mat, boolean glow) {
            double[] q = quaternion(rx, ry, rz);
            int[][] layers = {{w, h}, {w - 2, Math.max(2, w / 2)}, {w - 4, 2}};
            double along = 0;
            for (int i = 0; i < layers.length && layers[i][0] >= 1; i++) {
                int lw = layers[i][0], lh = layers[i][1];
                double[] off = rotate(new double[]{0, along + lh / 2.0, 0}, q);
                Mat m = i == 0 ? mat : new Mat(shade(mat.rgb(), 1.0 + 0.08 * i), mat.style());
                Box box = this.add(new Box(name + "_" + i, x + off[0], y + off[1], z + off[2], lw, lh, lw, m).rot(rx, ry, rz));
                if (glow) {
                    box.glow();
                }
                along += lh;
            }
        }

        /** Shelf-packs every face into a texture whose sides are multiples of 32. */
        void pack() {
            for (int width = 32; ; width += 32) {
                if (this.tryPack(width)) {
                    return;
                }
            }
        }

        private boolean tryPack(int width) {
            int x = 0, y = 0, rowH = 0;
            this.decals.clear();
            // decals first, one region each
            for (Box box : this.boxes) {
                for (Decal decal : box.decals) {
                    if (decal == null || this.decals.containsKey(decal.key())) {
                        continue;
                    }
                    if (decal.size() > width) {
                        return false;
                    }
                    if (x + decal.size() > width) {
                        x = 0;
                        y += rowH;
                        rowH = 0;
                    }
                    this.decals.put(decal.key(), new int[]{x, y});
                    this.decalOf.put(decal.key(), decal);
                    x += decal.size();
                    rowH = Math.max(rowH, decal.size());
                }
            }
            for (Box box : this.boxes) {
                for (int f = 0; f < 6; f++) {
                    if (box.decals[f] != null) {
                        int[] at = this.decals.get(box.decals[f].key());
                        box.uv[f][0] = at[0] + box.decalAt[f][0];
                        box.uv[f][1] = at[1] + box.decalAt[f][1];
                        continue;
                    }
                    int[] s = box.faceSize(f);
                    if (s[0] > width) {
                        return false;
                    }
                    if (x + s[0] > width) {
                        x = 0;
                        y += rowH;
                        rowH = 0;
                    }
                    box.uv[f][0] = x;
                    box.uv[f][1] = y;
                    x += s[0];
                    rowH = Math.max(rowH, s[1]);
                }
            }
            int height = y + rowH;
            if (height > width * 2) {
                return false;
            }
            this.texW = width;
            this.texH = Math.max(32, (height + 31) / 32 * 32);
            return true;
        }

        BufferedImage texture(long seed) {
            BufferedImage img = new BufferedImage(this.texW, this.texH, BufferedImage.TYPE_INT_ARGB);
            Random rnd = new Random(seed);
            for (java.util.Map.Entry<String, int[]> e : this.decals.entrySet()) {
                paintDecal(img, e.getValue()[0], e.getValue()[1], this.decalOf.get(e.getKey()), rnd);
            }
            for (Box box : this.boxes) {
                for (int f = 0; f < 6; f++) {
                    if (box.decals[f] != null) {
                        continue;
                    }
                    int[] s = box.faceSize(f);
                    paint(img, box.uv[f][0], box.uv[f][1], s[0], s[1], box.faces[f] != null ? box.faces[f] : box.mat, rnd);
                }
            }
            return img;
        }

        String json() {
            StringBuilder out = new StringBuilder("{\n  \"nodes\": [\n");
            out.append("    {\"id\": \"0\", \"name\": \"").append(this.rootName != null ? this.rootName : this.head ? "Head" : "Root").append("\", ")
                    .append("\"position\": ").append(vec(this.rootPosition[0], this.rootPosition[1], this.rootPosition[2]))
                    .append(", \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1}, ")
                    .append("\"shape\": {\"type\": \"none\", \"offset\": ").append(this.head ? vec(0, 20, 2) : vec(0, 0, 0))
                    .append(", \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1}, ")
                    .append("\"settings\": {").append(this.head ? "\"isPiece\": true" : "").append("}, \"textureLayout\": {}, ")
                    .append("\"unwrapMode\": \"custom\", \"visible\": true, \"doubleSided\": false, \"shadingMode\": \"flat\"},\n")
                    .append("     \"children\": [\n");
            List<String> children = new ArrayList<>();
            int id = 1;
            java.util.Set<String> written = new java.util.HashSet<>();
            for (Box b : this.boxes) {
                if (b.group == null) {
                    children.add(boxNode(b, new double[]{0, 0, 0}, id++, "      "));
                    continue;
                }
                if (!written.add(b.group)) {
                    continue;
                }
                Group g = this.groups.get(b.group);
                double[] at = {g.x(), g.y(), g.z()};
                StringBuilder node = new StringBuilder("      {\"id\": \"").append(id++).append("\", \"name\": \"").append(g.name()).append("\", ")
                        .append("\"position\": ").append(vec(at[0], at[1], at[2])).append(", \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1}, ")
                        .append("\"shape\": {\"type\": \"none\", \"offset\": {\"x\": 0, \"y\": 0, \"z\": 0}, \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1}, ")
                        .append("\"settings\": {\"isPiece\": false}, \"textureLayout\": {}, \"unwrapMode\": \"custom\", \"visible\": true, \"doubleSided\": false, ")
                        .append("\"shadingMode\": \"flat\"},\n       \"children\": [\n");
                List<String> inner = new ArrayList<>();
                for (Box c : this.boxes) {
                    if (g.name().equals(c.group)) {
                        inner.add(boxNode(c, at, id++, "        "));
                    }
                }
                children.add(node.append(String.join(",\n", inner)).append("\n       ]}").toString());
            }
            out.append(String.join(",\n", children)).append("\n");
            out.append("     ]}\n  ],\n  \"lod\": \"auto\"\n}\n");
            return out.toString();
        }

        /** One box as a node, placed relative to {@code origin}. */
        private static String boxNode(Box b, double[] origin, int id, String indent) {
            String[] faces = {"front", "back", "left", "right", "top", "bottom"};
            double[] q = b.rotation();
            StringBuilder out = new StringBuilder(indent).append("{\"id\": \"").append(id).append("\", \"name\": \"").append(b.name).append("\", ")
                    .append("\"position\": ").append(vec(b.x - origin[0], b.y - origin[1], b.z - origin[2])).append(", ")
                    .append("\"orientation\": {\"x\": ").append(num(q[0])).append(", \"y\": ").append(num(q[1]))
                    .append(", \"z\": ").append(num(q[2])).append(", \"w\": ").append(num(q[3])).append("}, ")
                    .append("\"shape\": {\"type\": \"box\", \"offset\": {\"x\": 0, \"y\": 0, \"z\": 0}, \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1}, ")
                    .append("\"settings\": {\"size\": ").append(vec(b.sx, b.sy, b.sz)).append("}, \"textureLayout\": {");
            for (int f = 0; f < 6; f++) {
                out.append(f == 0 ? "" : ", ").append('"').append(faces[f]).append("\": {\"offset\": {\"x\": ").append(b.uv[f][0])
                        .append(", \"y\": ").append(b.uv[f][1]).append("}, \"mirror\": {\"x\": false, \"y\": false}, \"angle\": 0}");
            }
            return out.append("}, \"unwrapMode\": \"custom\", \"visible\": true, \"doubleSided\": false, \"shadingMode\": \"")
                    .append(b.shading).append("\"}, \"children\": []}").toString();
        }

        /**
         * Puts the boxes added since {@code mark} into a node of their own with its pivot at (x, y, z), so an
         * animation can turn them together.
         */
        void group(String name, int mark, double x, double y, double z) {
            this.groups.put(name, new Group(name, x, y, z));
            for (int i = mark; i < this.boxes.size(); i++) {
                this.boxes.get(i).group = name;
            }
        }

        /** Packs the textures and writes the model file. */
        void write() throws IOException {
            this.pack();
            Path model = COMMON.resolve(this.path + ".blockymodel");
            Files.createDirectories(model.getParent());
            Files.writeString(model, this.json());
        }
    }

    static String vec(double x, double y, double z) {
        return "{\"x\": " + num(x) + ", \"y\": " + num(y) + ", \"z\": " + num(z) + "}";
    }

    static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.ROOT, "%.4f", v);
    }

    /** XYZ Euler angles in degrees to a quaternion (x, y, z, w). */
    static double[] quaternion(double rx, double ry, double rz) {
        double cx = Math.cos(Math.toRadians(rx) / 2), sx = Math.sin(Math.toRadians(rx) / 2);
        double cy = Math.cos(Math.toRadians(ry) / 2), sy = Math.sin(Math.toRadians(ry) / 2);
        double cz = Math.cos(Math.toRadians(rz) / 2), sz = Math.sin(Math.toRadians(rz) / 2);
        return new double[]{
                sx * cy * cz + cx * sy * sz,
                cx * sy * cz - sx * cy * sz,
                cx * cy * sz + sx * sy * cz,
                cx * cy * cz - sx * sy * sz};
    }

    static double[] rotate(double[] p, double[] q) {
        double x = p[0], y = p[1], z = p[2];
        double qx = q[0], qy = q[1], qz = q[2], qw = q[3];
        double tx = 2 * (qy * z - qz * y), ty = 2 * (qz * x - qx * z), tz = 2 * (qx * y - qy * x);
        return new double[]{x + qw * tx + (qy * tz - qz * ty), y + qw * ty + (qz * tx - qx * tz), z + qw * tz + (qx * ty - qy * tx)};
    }

    // ---------------------------------------------------------------- icon renderer

    /** Renders the model the way inventory icons look: from above front-right, softly lit, 64x64. */
    static BufferedImage icon(Model model, BufferedImage texture, double yaw, double pitch) {
        return icon(model, texture, yaw, pitch, 5, 1.0, 4);
    }

    /**
     * As above, with {@code margin} free pixels around the model, the light scaled by {@code gain} and
     * {@code ss} x {@code ss} samples per pixel (fewer keep the texture's pixels crisper).
     */
    static BufferedImage icon(Model model, BufferedImage texture, double yaw, double pitch, int margin, double gain, int ss) {
        final int size = 64 * ss;
        record Tri(double[][] p, double[][] t, double light, boolean glow) {
        }
        List<Tri> tris = new ArrayList<>();
        double cyaw = Math.cos(Math.toRadians(yaw)), syaw = Math.sin(Math.toRadians(yaw));
        double cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
        double[] lightDir = norm(new double[]{-0.35, 0.85, 0.4});
        for (Box b : model.boxes) {
            double[] q = b.rotation();
            double hx = b.sx / 2.0, hy = b.sy / 2.0, hz = b.sz / 2.0;
            // corners per face: front +z, back -z, left -x, right +x, top +y, bottom -y; ordered top-left, top-right, bottom-right, bottom-left as seen from outside
            double[][][] faces = {
                    {{-hx, hy, hz}, {hx, hy, hz}, {hx, -hy, hz}, {-hx, -hy, hz}},
                    {{hx, hy, -hz}, {-hx, hy, -hz}, {-hx, -hy, -hz}, {hx, -hy, -hz}},
                    {{-hx, hy, -hz}, {-hx, hy, hz}, {-hx, -hy, hz}, {-hx, -hy, -hz}},
                    {{hx, hy, hz}, {hx, hy, -hz}, {hx, -hy, -hz}, {hx, -hy, hz}},
                    {{-hx, hy, -hz}, {hx, hy, -hz}, {hx, hy, hz}, {-hx, hy, hz}},
                    {{-hx, -hy, hz}, {hx, -hy, hz}, {hx, -hy, -hz}, {-hx, -hy, -hz}}};
            for (int f = 0; f < 6; f++) {
                double[][] world = new double[4][];
                for (int i = 0; i < 4; i++) {
                    double[] r = rotate(faces[f][i], q);
                    double wx = r[0] + b.x, wy = r[1] + b.y, wz = r[2] + b.z;
                    // view: yaw around y, then pitch around x
                    double vx = wx * cyaw - wz * syaw;
                    double vz = wx * syaw + wz * cyaw;
                    double vy = wy * cp - vz * sp;
                    double vz2 = wy * sp + vz * cp;
                    world[i] = new double[]{vx, vy, vz2};
                }
                // the corners run clockwise as seen from outside: this order gives the outward normal
                double[] n = norm(cross(sub(world[3], world[0]), sub(world[1], world[0])));
                if (n[2] <= 0) {
                    continue;
                }
                int[] s = b.faceSize(f);
                double u0 = b.uv[f][0], v0 = b.uv[f][1];
                double[][] uv = {{u0, v0}, {u0 + s[0], v0}, {u0 + s[0], v0 + s[1]}, {u0, v0 + s[1]}};
                double light = b.shading.equals("fullbright") ? 1.15 : gain * (0.55 + 0.5 * Math.max(0, dot(n, lightDir)));
                tris.add(new Tri(new double[][]{world[0], world[1], world[2]}, new double[][]{uv[0], uv[1], uv[2]}, light, false));
                tris.add(new Tri(new double[][]{world[0], world[2], world[3]}, new double[][]{uv[0], uv[2], uv[3]}, light, false));
            }
        }
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Tri t : tris) {
            for (double[] p : t.p()) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        double scale = (size - 2.0 * margin * ss) / Math.max(maxX - minX, maxY - minY);
        double ox = size / 2.0 - (minX + maxX) / 2 * scale, oy = size / 2.0 + (minY + maxY) / 2 * scale;
        int[] color = new int[size * size];
        double[] depth = new double[size * size];
        java.util.Arrays.fill(depth, -Double.MAX_VALUE);
        for (Tri t : tris) {
            double[][] sp2 = new double[3][];
            for (int i = 0; i < 3; i++) {
                sp2[i] = new double[]{ox + t.p()[i][0] * scale, oy - t.p()[i][1] * scale, t.p()[i][2]};
            }
            int x0 = (int) Math.max(0, Math.floor(Math.min(sp2[0][0], Math.min(sp2[1][0], sp2[2][0]))));
            int x1 = (int) Math.min(size - 1, Math.ceil(Math.max(sp2[0][0], Math.max(sp2[1][0], sp2[2][0]))));
            int y0 = (int) Math.max(0, Math.floor(Math.min(sp2[0][1], Math.min(sp2[1][1], sp2[2][1]))));
            int y1 = (int) Math.min(size - 1, Math.ceil(Math.max(sp2[0][1], Math.max(sp2[1][1], sp2[2][1]))));
            double area = edge(sp2[0], sp2[1], sp2[2][0], sp2[2][1]);
            if (Math.abs(area) < 1e-9) {
                continue;
            }
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    double px = x + 0.5, py = y + 0.5;
                    double w0 = edge(sp2[1], sp2[2], px, py) / area;
                    double w1 = edge(sp2[2], sp2[0], px, py) / area;
                    double w2 = edge(sp2[0], sp2[1], px, py) / area;
                    if (w0 < 0 || w1 < 0 || w2 < 0) {
                        continue;
                    }
                    double z = w0 * sp2[0][2] + w1 * sp2[1][2] + w2 * sp2[2][2];
                    int idx = y * size + x;
                    if (z <= depth[idx]) {
                        continue;
                    }
                    double u = w0 * t.t()[0][0] + w1 * t.t()[1][0] + w2 * t.t()[2][0];
                    double v = w0 * t.t()[0][1] + w1 * t.t()[1][1] + w2 * t.t()[2][1];
                    int tx = Math.clamp((int) Math.floor(u), 0, texture.getWidth() - 1);
                    int ty = Math.clamp((int) Math.floor(v), 0, texture.getHeight() - 1);
                    int argb = texture.getRGB(tx, ty);
                    if ((argb >>> 24) < 128) {
                        continue;
                    }
                    depth[idx] = z;
                    color[idx] = 0xff000000 | shade(argb & 0xffffff, t.light());
                }
            }
        }
        return downsample(color, size, ss);
    }

    /** Averages {@code ss} x {@code ss} samples into each pixel of the image. */
    static BufferedImage downsample(int[] color, int size, int ss) {
        int pixels = size / ss;
        BufferedImage out = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < pixels; y++) {
            for (int x = 0; x < pixels; x++) {
                double r = 0, g = 0, bl = 0, a = 0;
                for (int dy = 0; dy < ss; dy++) {
                    for (int dx = 0; dx < ss; dx++) {
                        int c = color[(y * ss + dy) * size + x * ss + dx];
                        double ca = ((c >>> 24) & 0xff) / 255.0;
                        r += ((c >> 16) & 0xff) * ca;
                        g += ((c >> 8) & 0xff) * ca;
                        bl += (c & 0xff) * ca;
                        a += ca;
                    }
                }
                if (a > 0) {
                    int alpha = (int) Math.round(a / (ss * ss) * 255);
                    out.setRGB(x, y, (alpha << 24) | ((int) Math.round(r / a) << 16) | ((int) Math.round(g / a) << 8) | (int) Math.round(bl / a));
                }
            }
        }
        return out;
    }

    static double edge(double[] a, double[] b, double px, double py) {
        return (b[0] - a[0]) * (py - a[1]) - (b[1] - a[1]) * (px - a[0]);
    }

    static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] norm(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l == 0 ? a : new double[]{a[0] / l, a[1] / l, a[2] / l};
    }

    static void png(BufferedImage img, String path) throws IOException {
        Path file = COMMON.resolve(path);
        Files.createDirectories(file.getParent());
        ImageIO.write(img, "png", file.toFile());
    }

    /** A model drawn once per palette: same shape and layout, its own texture and icon each. */
    static void variants(java.util.function.Function<Palette, Model> shape, Palette[] palettes, String texturePrefix,
                         String[] iconIds, double yaw, double pitch) throws IOException {
        variants(shape, palettes, texturePrefix, iconIds, yaw, pitch, box -> true);
    }

    /** As above; the icon shows only the boxes {@code inIcon} accepts. */
    static void variants(java.util.function.Function<Palette, Model> shape, Palette[] palettes, String texturePrefix,
                         String[] iconIds, double yaw, double pitch, java.util.function.Predicate<Box> inIcon) throws IOException {
        Model first = shape.apply(palettes[0]);
        first.write();
        for (int i = 0; i < palettes.length; i++) {
            Model model = shape.apply(palettes[i]);
            model.pack();
            BufferedImage texture = model.texture(31L * i + 7);
            png(texture, texturePrefix + palettes[i].name() + ".png");
            if (iconIds[i] != null) {
                Model shown = new Model(model.path, model.head);
                model.boxes.stream().filter(inIcon).forEach(shown.boxes::add);
                png(icon(shown, texture, yaw, pitch), "Icons/ItemsGenerated/" + iconIds[i] + ".png");
            }
        }
    }

    record Palette(String name, Mat frame, Mat lens, Mat strap, Mat accent) {
    }

    // ---------------------------------------------------------------- the art

    static final Mat LEATHER = Mat.of("#5a3a22", Style.LEATHER);
    static final Mat COPPER = Mat.of("#c06a35", Style.METAL);
    static final Mat IRON = Mat.of("#9aa3ab", Style.METAL);
    static final Mat THORIUM = Mat.of("#6f9a5a", Style.METAL);
    static final Mat ADAMANTITE = Mat.of("#b23a3a", Style.METAL);
    static final Mat GOLD = Mat.of("#d4af37", Style.METAL);
    static final Mat VOID = Mat.of("#6a2c91", Style.METAL);

    /** Goggles and helms: zoom tiers, night vision tiers, ultra. */
    static final class Gear {
        static final Mat[] FRAMES = {COPPER, IRON, THORIUM, ADAMANTITE};
        static final Mat[] ZOOM_LENSES = {
                Mat.of("#cfe3ea", Style.GLASS), Mat.of("#9fd3ff", Style.GLASS), Mat.of("#ffd36b", Style.GLASS), Mat.of("#b98cff", Style.GLASS)};
        static final Mat NIGHT_LENS = Mat.of("#5cff6a", Style.GLOW);
        static final Mat ULTRA_LENS = Mat.of("#57f0ff", Style.GLOW);

        static void all() throws IOException {
            Palette[] zoom = new Palette[4];
            Palette[] night = new Palette[4];
            for (int t = 0; t < 4; t++) {
                zoom[t] = new Palette("Zoom_" + (t + 1), FRAMES[t], ZOOM_LENSES[t], LEATHER, FRAMES[t]);
                night[t] = new Palette("NightVision_" + (t + 1), FRAMES[t], NIGHT_LENS, LEATHER, Mat.of("#2b2f33", Style.METAL));
            }
            Palette ultra = new Palette("Ultra", GOLD, ULTRA_LENS, Mat.of("#2a1838", Style.LEATHER), VOID);
            String dir = "Items/FlexiisGoggles/";
            java.util.function.Predicate<Box> front = box -> !box.name.startsWith("Strap_Side") && !box.name.startsWith("Strap_Back");
            variants(p -> zoomGoggles(dir + "Goggles_Zoom", p), zoom, dir + "Goggles_",
                    ids("Flexii_Goggles_Zoom_"), 28, 14, front);
            variants(p -> nightGoggles(dir + "Goggles_NightVision", p), night, dir + "Goggles_",
                    ids("Flexii_Goggles_NightVision_"), 28, 14, front);
            variants(p -> ultraGoggles(dir + "Goggles_Ultra", p), new Palette[]{ultra}, dir + "Goggles_",
                    new String[]{"Flexii_Goggles_Ultra"}, 28, 14, front);
            // helms: the game's helmet of each tier with the goggles fitted in
            Helms.all(zoom, night, ultra);
        }

        static String[] ids(String prefix) {
            return new String[]{prefix + 1, prefix + 2, prefix + 3, prefix + 4};
        }

        /** The strap around the head, open at the lenses. */
        static void strap(Model m, Palette p) {
            m.pair("Strap_Side", 15.5, -1, 1, 1, 4, 26, p.strap());
            m.add(new Box("Strap_Back", 0, -1, -14.5, 32, 4, 1, p.strap()));
            m.pair("Strap_Front", 13.5, -1, 14.5, 3, 4, 1, p.strap());
        }

        static Model zoomGoggles(String path, Palette p) {
            Model m = new Model(path, true);
            strap(m, p);
            m.pair("Frame", 7.5, -2, 15.5, 10, 9, 2, p.frame());
            m.add(new Box("Bridge", 0, -1, 15.5, 5, 2, 2, p.frame()));
            m.pair("Tube", 7.5, -2, 19, 7, 7, 5, p.frame());
            m.pair("Ring", 7.5, -2, 21.75, 8, 8, 1, new Mat(shade(p.frame().rgb(), 1.12), Style.METAL));
            m.pair("Lens", 7.5, -2, 22.5, 5, 5, 1, p.lens());
            return m;
        }

        static Model nightGoggles(String path, Palette p) {
            Model m = new Model(path, true);
            strap(m, p);
            m.pair("Frame", 7.5, -2, 15.5, 10, 9, 2, p.frame());
            m.add(new Box("Bridge", 0, -1, 15.5, 5, 2, 2, p.frame()));
            m.pair("Hood", 7.5, -2, 17.5, 9, 8, 2, p.accent());
            for (Box lens : List.of(new Box("Lens_L", -7.5, -2, 18.75, 6, 6, 1, p.lens()), new Box("Lens_R", 7.5, -2, 18.75, 6, 6, 1, p.lens()))) {
                m.add(lens).glow();
            }
            m.add(new Box("Emitter", 0, 3.5, 16, 4, 3, 3, p.accent()));
            m.add(new Box("Emitter_Light", 0, 3.5, 17.75, 2, 2, 1, p.lens())).glow();
            return m;
        }

        static Model ultraGoggles(String path, Palette p) {
            Model m = new Model(path, true);
            strap(m, p);
            m.add(new Box("Visor_Frame", 0, -2, 15.5, 28, 10, 2, p.frame()));
            m.add(new Box("Visor", 0, -2, 16.75, 24, 6, 1, p.lens())).glow();
            m.add(new Box("Scope", 9.5, -2, 19.5, 6, 6, 5, p.accent()));
            m.add(new Box("Scope_Lens", 9.5, -2, 22.25, 4, 4, 1, Mat.of("#ffe28a", Style.GLOW))).glow();
            m.add(new Box("Emitter", -9, 4.5, 16, 4, 3, 3, p.accent()));
            m.add(new Box("Emitter_Light", -9, 4.5, 17.75, 2, 2, 1, p.lens())).glow();
            m.add(new Box("Crest_Gem", 0, 4, 16.25, 3, 3, 1, p.accent()));
            return m;
        }
    }

    /** Quartz, glass, lenses and the quartz ore overlay. */
    static final class Materials {
        static final Mat QUARTZ = Mat.of("#f6f1fa", Style.CRYSTAL);
        static final Mat QUARTZ_PINK = Mat.of("#f2c2e2", Style.CRYSTAL);

        static void all() throws IOException {
            String dir = "Resources/FlexiisGoggles/";
            // raw quartz: a cluster of pointed crystals
            Model quartz = new Model(dir + "Quartz", false);
            quartz.crystal("Crystal_1", 0, 0, 0, 8, 22, 0, 0, 6, QUARTZ, false);
            quartz.crystal("Crystal_2", 6, 0, 2, 6, 14, 8, 0, -22, QUARTZ_PINK, false);
            quartz.crystal("Crystal_3", -6, 0, -2, 6, 11, -10, 0, 26, QUARTZ, false);
            quartz.crystal("Crystal_4", 2, 0, -6, 4, 8, -25, 0, -8, QUARTZ_PINK, false);
            single(quartz, dir + "Quartz_Texture.png", "Flexii_Quartz", 30, 15);

            // glass: a thick pane
            Model glass = new Model(dir + "Glass", false);
            glass.add(new Box("Pane", 0, 3, 0, 24, 6, 24, Mat.of("#cfe3ea", Style.GLASS)));
            glass.add(new Box("Bubble", 4, 6.5, -4, 6, 1, 6, Mat.of("#e9f6fa", Style.GLASS)));
            single(glass, dir + "Glass_Texture.png", "Flexii_Glass", 35, 35);

            // lenses: a ring with a glass disc, tinted by tier
            Mat[] rings = {Mat.of("#b9b2a5", Style.METAL), IRON, GOLD, VOID};
            Mat[] discs = {Mat.of("#cfe3ea", Style.GLASS), Mat.of("#9fd3ff", Style.GLASS), Mat.of("#ffd36b", Style.GLASS), Mat.of("#b98cff", Style.GLASS)};
            String[] names = {"Glass", "Polished", "Crystal", "Void"};
            Palette[] lenses = new Palette[4];
            for (int i = 0; i < 4; i++) {
                lenses[i] = new Palette(names[i], rings[i], discs[i], rings[i], rings[i]);
            }
            variants(p -> lens(dir + "Lens", p), lenses, dir + "Lens_",
                    new String[]{"Flexii_Lens_Glass", "Flexii_Lens_Polished", "Flexii_Lens_Crystal", "Flexii_Lens_Void"}, 20, 25);

            // quartz ore: clusters of pointed crystals growing out of the stone
            Model ore = new Model(dir + "Ore_Quartz", false);
            outcrops(ore, QUARTZ, QUARTZ_PINK, false, 1.0);
            single(ore, dir + "Ore_Quartz_Texture.png", null, 35, 25);
            oreIcon(ore, "Flexii_Ore_Quartz_Stone");

            // lumenite: glowing crystals, seen only through strong night vision
            Model lumenite = new Model(dir + "Lumenite", false);
            lumenite.crystal("Crystal_1", 0, 0, 0, 8, 20, 0, 0, -8, LUMENITE, true);
            lumenite.crystal("Crystal_2", -6, 0, 1, 6, 13, 6, 0, 24, LUMENITE_VIOLET, true);
            lumenite.crystal("Crystal_3", 6, 0, -2, 6, 10, -8, 0, -26, LUMENITE, true);
            single(lumenite, dir + "Lumenite_Texture.png", "Flexii_Lumenite", 30, 15);
            Model revealed = new Model(dir + "Ore_Lumenite", false);
            outcrops(revealed, LUMENITE, LUMENITE_VIOLET, true, 0.8);
            single(revealed, dir + "Ore_Lumenite_Texture.png", null, 35, 25);
            oreIcon(revealed, "Flexii_Ore_Lumenite_Stone");
        }

        static final Mat LUMENITE = Mat.of("#3fe6d6", Style.CRYSTAL);
        static final Mat LUMENITE_VIOLET = Mat.of("#9c84ff", Style.CRYSTAL);

        /** Crystals growing out of the top and the four sides of a block (32 units, origin at the bottom centre). */
        static void outcrops(Model m, Mat a, Mat b, boolean glow, double scale) {
            // x, y, z of the base, axis angles, width, length
            double[][] spots = {
                    {-7, 29, -5, 0, 0, 12, 8, 16}, {5, 29, 6, 10, 0, -14, 7, 13}, {8, 29, -8, -12, 0, -8, 6, 10}, {-6, 29, 8, 8, 0, 18, 5, 8},
                    {-6, 22, 13, 90, 0, 10, 7, 13}, {7, 10, 13, 78, 0, -14, 6, 10}, {-8, 6, 13, 100, 0, 6, 5, 7},
                    {-7, 12, -13, -90, 0, 12, 7, 12}, {6, 24, -13, -78, 0, -10, 6, 10},
                    {13, 18, 5, 10, 0, -90, 7, 14}, {13, 7, -7, 0, 0, -76, 6, 9},
                    {-13, 20, -4, -8, 0, 90, 7, 13}, {-13, 8, 8, 0, 0, 80, 6, 9}};
            for (int i = 0; i < spots.length; i++) {
                double[] s = spots[i];
                m.crystal("Crystal_" + i, s[0], s[1], s[2], (int) Math.max(3, Math.round(s[6] * scale)), (int) Math.max(5, Math.round(s[7] * scale)),
                        s[3], s[4], s[5], i % 3 == 1 ? b : a, glow);
            }
        }

        /** The ore block's icon: the overlay on a stone cube. */
        static void oreIcon(Model overlay, String iconId) throws IOException {
            Model icon = new Model(overlay.path + "_Icon", false);
            icon.add(new Box("Stone", 0, 16, 0, 32, 32, 32, Mat.of("#7d7a76", Style.STONE)));
            overlay.boxes.forEach(icon.boxes::add);
            icon.pack();
            png(icon(icon, icon.texture(5), 35, 25), "Icons/ItemsGenerated/" + iconId + ".png");
        }

        static Model lens(String path, Palette p) {
            Model m = new Model(path, false);
            m.add(new Box("Ring_Top", 0, 21, 0, 20, 4, 4, p.frame()));
            m.add(new Box("Ring_Bottom", 0, 3, 0, 20, 4, 4, p.frame()));
            m.pair("Ring_Side", 8, 12, 0, 4, 14, 4, p.frame());
            m.add(new Box("Disc", 0, 12, 0, 12, 14, 2, p.lens()));
            return m;
        }

        static void single(Model model, String texture, String iconId, double yaw, double pitch) throws IOException {
            model.write();
            BufferedImage tex = model.texture(model.path.hashCode());
            png(tex, texture);
            if (iconId != null) {
                png(icon(model, tex, yaw, pitch), "Icons/ItemsGenerated/" + iconId + ".png");
            }
        }
    }

}
