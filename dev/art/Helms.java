import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The helms with goggles: each starts from the game's own helmet of its tier (model and texture from
 * Assets.zip; Hypixel allows mod creators to use the game's assets) and gets our goggles fitted onto its
 * face, beside the helmet's centre ridge. The helmet texture keeps its place, the goggles' texture goes
 * below it.
 */
final class Helms {
    /** Lens centres, in head-centre coordinates (the eyes are at x +-7.5, y -1.8). */
    static final double LENS_X = 10.5;
    static final double LENS_Y = -2;

    private Helms() {
    }

    static Path assets() {
        String given = System.getenv("HYTALE_ASSETS");
        return given != null ? Path.of(given)
                : Path.of(System.getProperty("user.home"), "Library/Application Support/Hytale/install/release/package/game/latest/Assets.zip");
    }

    static void all(Art.Palette[] zoom, Art.Palette[] night, Art.Palette ultra) throws IOException {
        String[] vanilla = {"Armor_Copper_Head", "Armor_Iron_Head", "Armor_Thorium_Head", "Armor_Adamantite_Head"};
        try (ZipFile zip = new ZipFile(assets().toFile())) {
            for (int t = 0; t < 4; t++) {
                build(zip, "Flexii_Helm_Scout_" + (t + 1), "Helm_Scout_" + (t + 1), vanilla[t], "Zoom", zoom[t]);
                build(zip, "Flexii_Helm_Nightwatch_" + (t + 1), "Helm_Nightwatch_" + (t + 1), vanilla[t], "NightVision", night[t]);
            }
            build(zip, "Flexii_Helm_Overseer", "Helm_Overseer", vanilla[3], "Ultra", ultra);
        }
    }

    private static void build(ZipFile zip, String itemId, String file, String vanillaItem, String kind, Art.Palette palette) throws IOException {
        String modelPath = itemField(zip, vanillaItem, "Model");
        String texturePath = itemField(zip, vanillaItem, "Texture");
        Map<String, Object> model = Json.object(Json.parse(text(zip, "Common/" + modelPath)));
        BufferedImage helmet;
        try (InputStream in = zip.getInputStream(entry(zip, "Common/" + texturePath))) {
            helmet = ImageIO.read(in);
        }
        Map<String, Object> root = Json.object(Json.array(model.get("nodes")).get(0));
        double[] rootOffset = Blocky.vec(Json.object(root.get("shape")).get("offset"), 0);
        double front = frontAt(model, LENS_X, LENS_Y);

        Art.Model goggles = goggles(kind, palette, front);
        goggles.pack();
        BufferedImage gogglesTexture = goggles.texture(itemId.hashCode());
        int width = Math.max(helmet.getWidth(), gogglesTexture.getWidth());
        int height = (helmet.getHeight() + gogglesTexture.getHeight() + 31) / 32 * 32;
        BufferedImage texture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = texture.createGraphics();
        g.drawImage(helmet, 0, 0, null);
        g.drawImage(gogglesTexture, 0, helmet.getHeight(), null);
        g.dispose();

        // Our boxes are in head-centre coordinates: the head box centre is (0, 20, 2) from the head bone,
        // the helmet's children sit at the bone plus the root's offset.
        double dx = -rootOffset[0], dy = 20 - rootOffset[1], dz = 2 - rootOffset[2];
        List<Object> children = Json.array(root.computeIfAbsent("children", k -> new ArrayList<>()));
        int id = nextId(model);
        for (Art.Box box : goggles.boxes) {
            children.add(node(box, dx, dy, dz, helmet.getHeight(), String.valueOf(id++)));
        }
        String dir = "Items/FlexiisGoggles/";
        Path modelFile = Art.COMMON.resolve(dir + file + ".blockymodel");
        Files.createDirectories(modelFile.getParent());
        Files.writeString(modelFile, Json.write(model), StandardCharsets.UTF_8);
        Art.png(texture, dir + file + "_Texture.png");
        Art.png(Blocky.render(Blocky.faces(model), texture, -40, 20), "Icons/ItemsGenerated/" + itemId + ".png");
    }

    /** The goggles on a helmet whose face is at {@code front} (z, head-centre coordinates) at the lenses. */
    static Art.Model goggles(String kind, Art.Palette p, double front) {
        Art.Model m = new Art.Model("unused", true);
        Art.Mat bright = new Art.Mat(Art.shade(p.frame().rgb(), 1.12), Art.Style.METAL);
        m.pair("Goggle_Frame", LENS_X, LENS_Y, front + 1, 9, 8, 2, p.frame());
        switch (kind) {
            case "Zoom" -> {
                m.pair("Goggle_Tube", LENS_X, LENS_Y, front + 4, 6, 6, 4, p.frame());
                m.pair("Goggle_Ring", LENS_X, LENS_Y, front + 6.5, 7, 7, 1, bright);
                m.pair("Goggle_Lens", LENS_X, LENS_Y, front + 7.25, 4, 4, 1, p.lens());
            }
            case "NightVision" -> {
                m.pair("Goggle_Hood", LENS_X, LENS_Y, front + 2.5, 8, 7, 1, p.accent());
                for (Art.Box lens : List.of(
                        new Art.Box("Goggle_Lens_L", -LENS_X, LENS_Y, front + 3.25, 6, 5, 1, p.lens()),
                        new Art.Box("Goggle_Lens_R", LENS_X, LENS_Y, front + 3.25, 6, 5, 1, p.lens()))) {
                    m.add(lens).glow();
                }
            }
            default -> {
                for (Art.Box lens : List.of(
                        new Art.Box("Goggle_Lens_L", -LENS_X, LENS_Y, front + 2.25, 7, 6, 1, p.lens()),
                        new Art.Box("Goggle_Lens_R", LENS_X, LENS_Y, front + 2.25, 7, 6, 1, p.lens()))) {
                    m.add(lens).glow();
                }
                m.add(new Art.Box("Goggle_Scope", LENS_X, LENS_Y, front + 4.75, 6, 6, 4, p.accent()));
                m.add(new Art.Box("Goggle_Scope_Lens", LENS_X, LENS_Y, front + 7.25, 4, 4, 1, Art.Mat.of("#ffe28a", Art.Style.GLOW))).glow();
            }
        }
        return m;
    }

    /** How far forward the helmet reaches at (x, y), in head-centre coordinates: its face there. */
    static double frontAt(Map<String, Object> model, double x, double y) {
        double bx = x, by = y + 20;
        double front = 14;
        for (Blocky.Face face : Blocky.faces(model)) {
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            for (double[] c : face.corners()) {
                minX = Math.min(minX, c[0]);
                maxX = Math.max(maxX, c[0]);
                minY = Math.min(minY, c[1]);
                maxY = Math.max(maxY, c[1]);
                maxZ = Math.max(maxZ, c[2]);
            }
            if (bx >= minX && bx <= maxX && by >= minY && by <= maxY) {
                front = Math.max(front, maxZ - 2);
            }
        }
        return front;
    }

    private static Map<String, Object> node(Art.Box b, double dx, double dy, double dz, int vShift, String id) {
        double[] q = Art.quaternion(b.rx, b.ry, b.rz);
        Map<String, Object> layout = new LinkedHashMap<>();
        String[] faces = {"front", "back", "left", "right", "top", "bottom"};
        for (int f = 0; f < 6; f++) {
            layout.put(faces[f], Map.of(
                    "offset", Map.of("x", (double) b.uv[f][0], "y", (double) (b.uv[f][1] + vShift)),
                    "mirror", Map.of("x", false, "y", false),
                    "angle", 0.0));
        }
        Map<String, Object> shape = new LinkedHashMap<>();
        shape.put("type", "box");
        shape.put("offset", xyz(0, 0, 0));
        shape.put("stretch", xyz(1, 1, 1));
        shape.put("settings", Map.of("size", xyz(b.sx, b.sy, b.sz)));
        shape.put("textureLayout", layout);
        shape.put("unwrapMode", "custom");
        shape.put("visible", true);
        shape.put("doubleSided", false);
        shape.put("shadingMode", b.shading);
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("name", b.name);
        node.put("position", xyz(b.x + dx, b.y + dy, b.z + dz));
        node.put("orientation", Map.of("x", q[0], "y", q[1], "z", q[2], "w", q[3]));
        node.put("shape", shape);
        node.put("children", new ArrayList<>());
        return node;
    }

    private static Map<String, Object> xyz(double x, double y, double z) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        return m;
    }

    private static int nextId(Map<String, Object> model) {
        int[] max = {0};
        for (Object node : Json.array(model.get("nodes"))) {
            maxId(Json.object(node), max);
        }
        return max[0] + 1;
    }

    private static void maxId(Map<String, Object> node, int[] max) {
        try {
            max[0] = Math.max(max[0], Integer.parseInt(String.valueOf(node.get("id"))));
        } catch (NumberFormatException e) {
            // ids are numbers in the game's models; anything else just isn't counted
        }
        for (Object child : Json.array(node.getOrDefault("children", List.of()))) {
            maxId(Json.object(child), max);
        }
    }

    /** A field of an item, following its Parent chain. */
    private static String itemField(ZipFile zip, String itemId, String field) throws IOException {
        for (String id = itemId; id != null; ) {
            Map<String, Object> item = Json.object(Json.parse(text(zip, itemPath(zip, id))));
            if (item.get(field) instanceof String value) {
                return value;
            }
            id = item.get("Parent") instanceof String parent ? parent : null;
        }
        throw new IOException(itemId + " has no " + field);
    }

    private static String itemPath(ZipFile zip, String itemId) throws IOException {
        String suffix = "/" + itemId + ".json";
        return zip.stream().map(ZipEntry::getName)
                .filter(n -> n.startsWith("Server/Item/Items/") && n.endsWith(suffix))
                .findFirst().orElseThrow(() -> new IOException("no item " + itemId));
    }

    private static ZipEntry entry(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException("not in Assets.zip: " + name);
        }
        return entry;
    }

    private static String text(ZipFile zip, String name) throws IOException {
        try (InputStream in = zip.getInputStream(entry(zip, name))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
