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
    private static final Path COMMON = Path.of("src/main/resources/Common");

    public static void main(String[] args) throws IOException {
        Gear.all();
        Materials.all();
        Bench.all();
        System.out.println("art written to " + COMMON);
    }

    // ---------------------------------------------------------------- colours and materials

    enum Style { METAL, LEATHER, GLASS, GLOW, WOOD, STONE, CRYSTAL, PLAIN }

    record Mat(int rgb, Style style) {
        static Mat of(String hex, Style style) {
            return new Mat(Integer.parseInt(hex.substring(1), 16), style);
        }
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
                        c = mix(shade(mat.rgb(), 1.15), shade(mat.rgb(), 0.85), t);
                        if (x == w / 3 && !edge) c = mix(c, 0xffffff, 0.6);
                        if (edge) c = shade(mat.rgb(), 0.78);
                    }
                    case PLAIN -> c = shade(c, 0.97 + n * 0.06);
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

        Box rot(double rx, double ry, double rz) {
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            return this;
        }

        Box glow() {
            this.shading = "fullbright";
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
            for (Box box : this.boxes) {
                for (int f = 0; f < 6; f++) {
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
            for (Box box : this.boxes) {
                for (int f = 0; f < 6; f++) {
                    int[] s = box.faceSize(f);
                    paint(img, box.uv[f][0], box.uv[f][1], s[0], s[1], box.mat, rnd);
                }
            }
            return img;
        }

        String json() {
            StringBuilder out = new StringBuilder("{\n  \"nodes\": [\n");
            out.append("    {\"id\": \"0\", \"name\": \"").append(this.head ? "Head" : "Root").append("\", ")
                    .append("\"position\": {\"x\": 0, \"y\": 0, \"z\": 0}, \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1}, ")
                    .append("\"shape\": {\"type\": \"none\", \"offset\": ").append(this.head ? vec(0, 20, 2) : vec(0, 0, 0))
                    .append(", \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1}, ")
                    .append("\"settings\": {").append(this.head ? "\"isPiece\": true" : "").append("}, \"textureLayout\": {}, ")
                    .append("\"unwrapMode\": \"custom\", \"visible\": true, \"doubleSided\": false, \"shadingMode\": \"flat\"},\n")
                    .append("     \"children\": [\n");
            String[] faces = {"front", "back", "left", "right", "top", "bottom"};
            for (int i = 0; i < this.boxes.size(); i++) {
                Box b = this.boxes.get(i);
                double[] q = quaternion(b.rx, b.ry, b.rz);
                out.append("      {\"id\": \"").append(i + 1).append("\", \"name\": \"").append(b.name).append("\", ")
                        .append("\"position\": ").append(vec(b.x, b.y, b.z)).append(", ")
                        .append("\"orientation\": {\"x\": ").append(num(q[0])).append(", \"y\": ").append(num(q[1]))
                        .append(", \"z\": ").append(num(q[2])).append(", \"w\": ").append(num(q[3])).append("}, ")
                        .append("\"shape\": {\"type\": \"box\", \"offset\": {\"x\": 0, \"y\": 0, \"z\": 0}, \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1}, ")
                        .append("\"settings\": {\"size\": ").append(vec(b.sx, b.sy, b.sz)).append("}, \"textureLayout\": {");
                for (int f = 0; f < 6; f++) {
                    out.append(f == 0 ? "" : ", ").append('"').append(faces[f]).append("\": {\"offset\": {\"x\": ").append(b.uv[f][0])
                            .append(", \"y\": ").append(b.uv[f][1]).append("}, \"mirror\": {\"x\": false, \"y\": false}, \"angle\": 0}");
                }
                out.append("}, \"unwrapMode\": \"custom\", \"visible\": true, \"doubleSided\": false, \"shadingMode\": \"")
                        .append(b.shading).append("\"}, \"children\": []}").append(i + 1 < this.boxes.size() ? ",\n" : "\n");
            }
            out.append("     ]}\n  ],\n  \"lod\": \"auto\"\n}\n");
            return out.toString();
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
        final int ss = 4, size = 64 * ss;
        record Tri(double[][] p, double[][] t, double light, boolean glow) {
        }
        List<Tri> tris = new ArrayList<>();
        double cyaw = Math.cos(Math.toRadians(yaw)), syaw = Math.sin(Math.toRadians(yaw));
        double cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
        double[] lightDir = norm(new double[]{-0.35, 0.85, 0.4});
        for (Box b : model.boxes) {
            double[] q = quaternion(b.rx, b.ry, b.rz);
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
                double[] n = norm(cross(sub(world[1], world[0]), sub(world[3], world[0])));
                if (n[2] <= 0) {
                    continue;
                }
                int[] s = b.faceSize(f);
                double u0 = b.uv[f][0], v0 = b.uv[f][1];
                double[][] uv = {{u0, v0}, {u0 + s[0], v0}, {u0 + s[0], v0 + s[1]}, {u0, v0 + s[1]}};
                double light = b.shading.equals("fullbright") ? 1.15 : 0.55 + 0.5 * Math.max(0, dot(n, lightDir));
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
        double scale = (size - 10.0 * ss) / Math.max(maxX - minX, maxY - minY);
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
                    depth[idx] = z;
                    double u = w0 * t.t()[0][0] + w1 * t.t()[1][0] + w2 * t.t()[2][0];
                    double v = w0 * t.t()[0][1] + w1 * t.t()[1][1] + w2 * t.t()[2][1];
                    int tx = Math.clamp((int) Math.floor(u), 0, texture.getWidth() - 1);
                    int ty = Math.clamp((int) Math.floor(v), 0, texture.getHeight() - 1);
                    color[idx] = 0xff000000 | shade(texture.getRGB(tx, ty) & 0xffffff, t.light());
                }
            }
        }
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
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
            variants(p -> zoomGoggles(dir + "Goggles_Zoom", p, false), zoom, dir + "Goggles_",
                    ids("Flexii_Goggles_Zoom_"), 28, 14, front);
            variants(p -> nightGoggles(dir + "Goggles_NightVision", p, false), night, dir + "Goggles_",
                    ids("Flexii_Goggles_NightVision_"), 28, 14, front);
            variants(p -> ultraGoggles(dir + "Goggles_Ultra", p, false), new Palette[]{ultra}, dir + "Goggles_",
                    new String[]{"Flexii_Goggles_Ultra"}, 28, 14, front);
            // helms: the same goggles on a helmet shell in the tier's metal
            variants(p -> zoomGoggles(dir + "Helm_Scout", p, true), zoom, dir + "Helm_",
                    new String[]{"Flexii_Helm_Scout_1", "Flexii_Helm_Scout_2", "Flexii_Helm_Scout_3", "Flexii_Helm_Scout_4"}, 35, 20);
            variants(p -> nightGoggles(dir + "Helm_Nightwatch", p, true), night, dir + "Helm_",
                    new String[]{"Flexii_Helm_Nightwatch_1", "Flexii_Helm_Nightwatch_2", "Flexii_Helm_Nightwatch_3", "Flexii_Helm_Nightwatch_4"}, 35, 20);
            variants(p -> ultraGoggles(dir + "Helm_Overseer", p, true), new Palette[]{ultra}, dir + "Helm_",
                    new String[]{"Flexii_Helm_Overseer"}, 35, 20);
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

        /** A helmet shell in the frame's metal: crown, sides, back and a brow band; the face stays open. */
        static void helmet(Model m, Palette p) {
            Mat metal = p.frame();
            Mat dark = new Mat(shade(metal.rgb(), 0.78), Style.METAL);
            m.add(new Box("Crown", 0, 15.5, 0, 34, 5, 32, metal));
            m.pair("Side", 16.5, 5, 0, 3, 18, 30, dark);
            m.add(new Box("Back", 0, 5, -15.5, 30, 18, 3, dark));
            m.add(new Box("Brow", 0, 10, 15.5, 30, 4, 3, metal));
            m.add(new Box("Crest", 0, 19, -1, 4, 3, 26, p.accent()));
        }

        static Model zoomGoggles(String path, Palette p, boolean helm) {
            Model m = new Model(path, true);
            if (helm) helmet(m, p);
            strap(m, p);
            m.pair("Frame", 7.5, -2, 15.5, 10, 9, 2, p.frame());
            m.add(new Box("Bridge", 0, -1, 15.5, 5, 2, 2, p.frame()));
            m.pair("Tube", 7.5, -2, 19, 7, 7, 5, p.frame());
            m.pair("Ring", 7.5, -2, 21.75, 8, 8, 1, new Mat(shade(p.frame().rgb(), 1.12), Style.METAL));
            m.pair("Lens", 7.5, -2, 22.5, 5, 5, 1, p.lens());
            return m;
        }

        static Model nightGoggles(String path, Palette p, boolean helm) {
            Model m = new Model(path, true);
            if (helm) helmet(m, p);
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

        static Model ultraGoggles(String path, Palette p, boolean helm) {
            Model m = new Model(path, true);
            if (helm) helmet(m, p);
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
            // raw quartz: a small crystal cluster
            Model quartz = new Model(dir + "Quartz", false);
            quartz.add(new Box("Base", 0, 4, 0, 18, 8, 14, Mat.of("#8d8a87", Style.STONE)));
            quartz.add(new Box("Crystal_1", -2, 16, 0, 6, 20, 6, QUARTZ).rot(0, 0, 12));
            quartz.add(new Box("Crystal_2", 5, 13, 2, 6, 14, 6, QUARTZ_PINK).rot(10, 0, -18));
            quartz.add(new Box("Crystal_3", -6, 11, -3, 4, 12, 4, QUARTZ).rot(-14, 0, 24));
            single(quartz, dir + "Quartz_Texture.png", "Flexii_Quartz", 35, 20);

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

            // quartz ore: crystals breaking out of the stone, drawn over the stone cube
            Model ore = new Model(dir + "Ore_Quartz", false);
            double[][] spots = {{-9, 30, -6, 0, 0, 18}, {6, 31, 8, 12, 0, -10}, {10, 18, 15, -70, 0, 6}, {-12, 12, 15, -75, 20, 0},
                    {15, 22, -5, 0, 0, -75}, {-15, 8, 4, 10, 0, 72}, {2, 9, -15, 75, 0, 10}, {-4, 31, 10, -8, 30, 8}};
            for (int i = 0; i < spots.length; i++) {
                double[] s = spots[i];
                ore.add(new Box("Crystal_" + i, s[0], s[1], s[2], 5, 11, 5, i % 3 == 1 ? QUARTZ_PINK : QUARTZ).rot(s[3], s[4], s[5]));
            }
            single(ore, dir + "Ore_Quartz_Texture.png", null, 35, 25);
            // the ore's icon: the overlay on a stone cube
            Model oreIcon = new Model(dir + "Ore_Quartz_Icon", false);
            oreIcon.add(new Box("Stone", 0, 16, 0, 32, 32, 32, Mat.of("#7d7a76", Style.STONE)));
            ore.boxes.forEach(oreIcon.boxes::add);
            oreIcon.pack();
            png(icon(oreIcon, oreIcon.texture(5), 35, 25), "Icons/ItemsGenerated/Flexii_Ore_Quartz_Stone.png");
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

    /** The optician's bench: a desk with a grinding wheel, a lens on an arm and lens blanks. */
    static final class Bench {
        static void all() throws IOException {
            String dir = "Blocks/FlexiisGoggles/";
            Mat wood = Mat.of("#8a5a33", Style.WOOD);
            Mat darkWood = Mat.of("#5e3b20", Style.WOOD);
            Mat stone = Mat.of("#9a9690", Style.STONE);
            Palette[] tiers = {
                    new Palette("Tier1", COPPER, Mat.of("#cfe3ea", Style.GLASS), wood, darkWood),
                    new Palette("Tier2", IRON, Mat.of("#9fd3ff", Style.GLASS), wood, darkWood),
                    new Palette("Tier3", GOLD, Mat.of("#ffd36b", Style.GLASS), wood, Mat.of("#3e5c8a", Style.WOOD)),
                    new Palette("Tier4", VOID, Mat.of("#b98cff", Style.GLOW), Mat.of("#3a2a3e", Style.WOOD), Mat.of("#24182c", Style.WOOD))};
            variants(p -> desk(dir + "Optician_Bench", p, stone), tiers, dir + "Optician_Bench_",
                    new String[]{"Flexii_Bench_Optician", null, null, null}, 35, 25);
            // category icons: lenses, zoom, night vision, ultra
            Files.createDirectories(COMMON.resolve("Icons/CraftingCategories/FlexiisGoggles"));
            copyIcon("Flexii_Lens_Polished", "Materials");
            copyIcon("Flexii_Goggles_Zoom_2", "Zoom");
            copyIcon("Flexii_Goggles_NightVision_2", "NightVision");
            copyIcon("Flexii_Goggles_Ultra", "Ultra");
        }

        static void copyIcon(String item, String category) throws IOException {
            Files.copy(COMMON.resolve("Icons/ItemsGenerated/" + item + ".png"),
                    COMMON.resolve("Icons/CraftingCategories/FlexiisGoggles/" + category + ".png"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        static Model desk(String path, Palette p, Mat stone) {
            Model m = new Model(path, false);
            m.add(new Box("Top", 0, 22.5, 0, 32, 3, 28, p.strap()));
            m.pair("Leg_Front", 13.5, 10.5, 11.5, 3, 21, 3, p.accent());
            m.pair("Leg_Back", 13.5, 10.5, -11.5, 3, 21, 3, p.accent());
            m.add(new Box("Shelf", 0, 6, 0, 26, 2, 22, p.accent()));
            // grinding wheel on a frame, right side
            m.add(new Box("Wheel_Stand", 9, 25.5, -3, 8, 3, 6, p.frame()));
            m.add(new Box("Wheel", 9, 32, -3, 2, 10, 10, stone));
            m.add(new Box("Wheel_Axle", 9, 32, -3, 6, 2, 2, p.frame()));
            // lens on an arm, left side
            m.add(new Box("Arm_Base", -9, 25, 5, 5, 2, 5, p.frame()));
            m.add(new Box("Arm", -9, 30, 5, 2, 9, 2, p.frame()));
            m.add(new Box("Lens_Ring", -9, 36.5, 6, 9, 9, 1, p.frame()));
            m.add(new Box("Lens", -9, 36.5, 6.75, 7, 7, 1, p.lens()));
            // lens blanks lying on the desk
            m.add(new Box("Blank_1", 1, 24.5, 8, 4, 1, 4, p.lens()));
            m.add(new Box("Blank_2", 4, 24.5, 4, 3, 1, 3, p.lens()));
            m.add(new Box("Plate", -2, 24.25, -6, 7, 1, 5, p.frame()));
            return m;
        }
    }
}
