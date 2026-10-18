import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Renders any {@code .blockymodel} (parsed with {@link Json}): nested nodes with position, orientation,
 * shape offset and stretch; boxes and quads; texture layouts with mirror and angle. Coordinates are the
 * model's own: children sit at their parent's pivot plus its shape offset, in the parent's rotation.
 */
final class Blocky {
    /** One textured face: corners top-left, top-right, bottom-right, bottom-left, and their texture pixels. */
    record Face(double[][] corners, double[][] uv, boolean fullbright) {
    }

    private Blocky() {
    }

    static List<Face> faces(Map<String, Object> model) {
        List<Face> out = new ArrayList<>();
        for (Object node : Json.array(model.get("nodes"))) {
            walk(Json.object(node), new double[]{0, 0, 0}, new double[]{0, 0, 0, 1}, out, true);
        }
        return out;
    }

    private static void walk(Map<String, Object> node, double[] origin, double[] rotation, List<Face> out, boolean root) {
        Map<String, Object> shape = Json.object(node.getOrDefault("shape", Map.of()));
        double[] position = root ? new double[]{0, 0, 0} : vec(node.get("position"), 0);
        double[] q = mul(rotation, quat(node.get("orientation")));
        double[] pivot = add(origin, Art.rotate(position, rotation));
        double[] offset = vec(shape.get("offset"), 0);
        double[] centre = add(pivot, Art.rotate(offset, q));
        double[] stretch = vec(shape.get("stretch"), 1);
        String type = String.valueOf(shape.getOrDefault("type", "none"));
        boolean visible = !Boolean.FALSE.equals(shape.get("visible"));
        boolean fullbright = "fullbright".equals(shape.get("shadingMode"));
        Map<String, Object> settings = Json.object(shape.getOrDefault("settings", Map.of()));
        Map<String, Object> layout = Json.object(shape.getOrDefault("textureLayout", Map.of()));
        if (visible && "box".equals(type)) {
            double[] size = vec(settings.get("size"), 0);
            double hx = size[0] / 2 * stretch[0], hy = size[1] / 2 * stretch[1], hz = size[2] / 2 * stretch[2];
            double[][][] corners = {
                    {{-hx, hy, hz}, {hx, hy, hz}, {hx, -hy, hz}, {-hx, -hy, hz}},
                    {{hx, hy, -hz}, {-hx, hy, -hz}, {-hx, -hy, -hz}, {hx, -hy, -hz}},
                    {{-hx, hy, -hz}, {-hx, hy, hz}, {-hx, -hy, hz}, {-hx, -hy, -hz}},
                    {{hx, hy, hz}, {hx, hy, -hz}, {hx, -hy, -hz}, {hx, -hy, hz}},
                    {{-hx, hy, -hz}, {hx, hy, -hz}, {hx, hy, hz}, {-hx, hy, hz}},
                    {{-hx, -hy, hz}, {hx, -hy, hz}, {hx, -hy, -hz}, {-hx, -hy, -hz}}};
            String[] names = {"front", "back", "left", "right", "top", "bottom"};
            double[][] faceSize = {{size[0], size[1]}, {size[0], size[1]}, {size[2], size[1]}, {size[2], size[1]}, {size[0], size[2]}, {size[0], size[2]}};
            for (int f = 0; f < 6; f++) {
                Object face = layout.get(names[f]);
                if (face == null) {
                    continue;
                }
                out.add(new Face(place(corners[f], centre, q), uv(Json.object(face), faceSize[f][0], faceSize[f][1]), fullbright));
            }
        } else if (visible && "quad".equals(type)) {
            double[] size = vec(settings.get("size"), 0);
            double hx = size[0] / 2 * stretch[0], hy = size[1] / 2 * stretch[1];
            double[][] corners = quad(String.valueOf(settings.getOrDefault("normal", "+Z")), hx, hy);
            Object face = layout.get("front");
            if (face != null) {
                out.add(new Face(place(corners, centre, q), uv(Json.object(face), size[0], size[1]), fullbright));
            }
        }
        for (Object child : Json.array(node.getOrDefault("children", List.of()))) {
            walk(Json.object(child), centre, q, out, false);
        }
    }

    private static double[][] quad(String normal, double hx, double hy) {
        return switch (normal) {
            case "-Z" -> new double[][]{{hx, hy, 0}, {-hx, hy, 0}, {-hx, -hy, 0}, {hx, -hy, 0}};
            case "+X" -> new double[][]{{0, hy, hx}, {0, hy, -hx}, {0, -hy, -hx}, {0, -hy, hx}};
            case "-X" -> new double[][]{{0, hy, -hx}, {0, hy, hx}, {0, -hy, hx}, {0, -hy, -hx}};
            case "+Y" -> new double[][]{{-hx, 0, -hy}, {hx, 0, -hy}, {hx, 0, hy}, {-hx, 0, hy}};
            case "-Y" -> new double[][]{{-hx, 0, hy}, {hx, 0, hy}, {hx, 0, -hy}, {-hx, 0, -hy}};
            default -> new double[][]{{-hx, hy, 0}, {hx, hy, 0}, {hx, -hy, 0}, {-hx, -hy, 0}};
        };
    }

    private static double[][] place(double[][] corners, double[] centre, double[] q) {
        double[][] out = new double[4][];
        for (int i = 0; i < 4; i++) {
            out[i] = add(centre, Art.rotate(corners[i], q));
        }
        return out;
    }

    /** Texture pixels for the corners, honouring mirror and angle. */
    private static double[][] uv(Map<String, Object> face, double w, double h) {
        double[] offset = {Json.number(Json.object(face.getOrDefault("offset", Map.of())).get("x"), 0),
                Json.number(Json.object(face.getOrDefault("offset", Map.of())).get("y"), 0)};
        Map<String, Object> mirror = Json.object(face.getOrDefault("mirror", Map.of()));
        int angle = (int) Json.number(face.get("angle"), 0);
        double rw = angle % 180 == 0 ? w : h, rh = angle % 180 == 0 ? h : w;
        double[][] rect = {{offset[0], offset[1]}, {offset[0] + rw, offset[1]}, {offset[0] + rw, offset[1] + rh}, {offset[0], offset[1] + rh}};
        int shift = ((angle / 90) % 4 + 4) % 4;
        double[][] out = new double[4][];
        for (int i = 0; i < 4; i++) {
            out[i] = rect[(i + shift) % 4];
        }
        if (Boolean.TRUE.equals(mirror.get("x"))) {
            out = new double[][]{out[1], out[0], out[3], out[2]};
        }
        if (Boolean.TRUE.equals(mirror.get("y"))) {
            out = new double[][]{out[3], out[2], out[1], out[0]};
        }
        return out;
    }

    /** The model as an inventory icon, 64x64, seen from above front-right. */
    static BufferedImage render(List<Face> faces, BufferedImage texture, double yaw, double pitch) {
        return render(faces, texture, yaw, pitch, 64);
    }

    /** As above, {@code pixels} wide and high. */
    static BufferedImage render(List<Face> faces, BufferedImage texture, double yaw, double pitch, int pixels) {
        final int ss = 4, size = pixels * ss;
        double cyaw = Math.cos(Math.toRadians(yaw)), syaw = Math.sin(Math.toRadians(yaw));
        double cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
        double[] light = Art.norm(new double[]{-0.35, 0.85, 0.4});
        List<double[][]> view = new ArrayList<>();
        for (Face face : faces) {
            double[][] v = new double[4][];
            for (int i = 0; i < 4; i++) {
                double[] p = face.corners()[i];
                double vx = p[0] * cyaw - p[2] * syaw;
                double vz = p[0] * syaw + p[2] * cyaw;
                v[i] = new double[]{vx, p[1] * cp - vz * sp, p[1] * sp + vz * cp};
            }
            view.add(v);
        }
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[][] v : view) {
            for (double[] p : v) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        double scale = (size - 8.0 * ss) / Math.max(maxX - minX, maxY - minY);
        double ox = size / 2.0 - (minX + maxX) / 2 * scale, oy = size / 2.0 + (minY + maxY) / 2 * scale;
        int[] color = new int[size * size];
        double[] depth = new double[size * size];
        Arrays.fill(depth, -Double.MAX_VALUE);
        for (int f = 0; f < faces.size(); f++) {
            Face face = faces.get(f);
            double[][] v = view.get(f);
            double[] n = Art.norm(Art.cross(Art.sub(v[1], v[0]), Art.sub(v[3], v[0])));
            double shade = face.fullbright() ? 1.12 : 0.55 + 0.5 * Math.abs(Art.dot(n, light));
            int[][] tris = {{0, 1, 2}, {0, 2, 3}};
            for (int[] t : tris) {
                double[][] s = new double[3][];
                double[][] tuv = new double[3][];
                for (int i = 0; i < 3; i++) {
                    double[] p = v[t[i]];
                    s[i] = new double[]{ox + p[0] * scale, oy - p[1] * scale, p[2]};
                    tuv[i] = face.uv()[t[i]];
                }
                double area = Art.edge(s[0], s[1], s[2][0], s[2][1]);
                if (Math.abs(area) < 1e-9) {
                    continue;
                }
                int x0 = (int) Math.max(0, Math.floor(Math.min(s[0][0], Math.min(s[1][0], s[2][0]))));
                int x1 = (int) Math.min(size - 1, Math.ceil(Math.max(s[0][0], Math.max(s[1][0], s[2][0]))));
                int y0 = (int) Math.max(0, Math.floor(Math.min(s[0][1], Math.min(s[1][1], s[2][1]))));
                int y1 = (int) Math.min(size - 1, Math.ceil(Math.max(s[0][1], Math.max(s[1][1], s[2][1]))));
                for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                        double px = x + 0.5, py = y + 0.5;
                        double w0 = Art.edge(s[1], s[2], px, py) / area;
                        double w1 = Art.edge(s[2], s[0], px, py) / area;
                        double w2 = Art.edge(s[0], s[1], px, py) / area;
                        if (w0 < 0 || w1 < 0 || w2 < 0) {
                            continue;
                        }
                        double z = w0 * s[0][2] + w1 * s[1][2] + w2 * s[2][2];
                        int idx = y * size + x;
                        if (z <= depth[idx]) {
                            continue;
                        }
                        double u = w0 * tuv[0][0] + w1 * tuv[1][0] + w2 * tuv[2][0];
                        double vv = w0 * tuv[0][1] + w1 * tuv[1][1] + w2 * tuv[2][1];
                        int tx = Math.clamp((int) Math.floor(u), 0, texture.getWidth() - 1);
                        int ty = Math.clamp((int) Math.floor(vv), 0, texture.getHeight() - 1);
                        int argb = texture.getRGB(tx, ty);
                        if ((argb >>> 24) < 128) {
                            continue;
                        }
                        depth[idx] = z;
                        color[idx] = 0xff000000 | Art.shade(argb & 0xffffff, shade);
                    }
                }
            }
        }
        return Art.downsample(color, size, ss);
    }

    static double[] vec(Object value, double fallback) {
        Map<String, Object> m = value == null ? Map.of() : Json.object(value);
        return new double[]{Json.number(m.get("x"), fallback), Json.number(m.get("y"), fallback), Json.number(m.get("z"), fallback)};
    }

    static double[] quat(Object value) {
        Map<String, Object> m = value == null ? Map.of() : Json.object(value);
        return new double[]{Json.number(m.get("x"), 0), Json.number(m.get("y"), 0), Json.number(m.get("z"), 0), Json.number(m.get("w"), 1)};
    }

    /** Quaternion product a * b (apply b, then a). */
    static double[] mul(double[] a, double[] b) {
        return new double[]{
                a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1],
                a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0],
                a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3],
                a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2]};
    }

    static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }
}
