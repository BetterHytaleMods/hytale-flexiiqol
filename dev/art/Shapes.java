/**
 * Shapes the game builds from boxes: round discs and rings from pixel rows (one round picture across
 * their faces), square rods between two points, pieces along a turned axis.
 */
final class Shapes {
    private Shapes() {
    }

    /**
     * A round disc of diameter {@code d} (a ring if {@code inner} > 0, same parity as d), {@code t} thick,
     * built from pixel rows like the game's round parts: lying in the x-y plane facing +z, then turned by q.
     */
    static void disc(Art.Model m, String name, double[] c, int d, int inner, int t, double[] q, Art.Mat mat, boolean glow) {
        int[][] rows = new int[d][];
        for (int r = 0; r < d; r++) {
            double v = -d / 2.0 + r + 0.5;
            rows[r] = new int[]{span(d, v), inner > 0 ? span(inner, v) : 0};
        }
        // a solid disc shows one round picture on its two faces; a ring is plain
        Art.Decal decal = inner == 0 ? new Art.Decal(name, d, mat) : null;
        int part = 0;
        for (int r = 0; r < d; ) {
            int end = r;
            while (end + 1 < d && rows[end + 1][0] == rows[r][0] && rows[end + 1][1] == rows[r][1]) {
                end++;
            }
            int w = rows[r][0], wi = rows[r][1], h = end - r + 1;
            // rows run from the bottom (r = 0) up; the picture's rows run down from the top
            int top = d - (end + 1);
            if (wi == 0) {
                place(m, name + "_" + part++, c, q, 0, h, w, top, d, t, mat, glow, decal);
            } else {
                int side = (w - wi) / 2;
                double u = wi / 2.0 + side / 2.0;
                place(m, name + "_" + part++, c, q, -u, h, side, top, d, t, mat, glow, null);
                place(m, name + "_" + part++, c, q, u, h, side, top, d, t, mat, glow, null);
            }
            r = end + 1;
        }
    }

    /** Width of a pixel circle of diameter d at height v from its centre: 0 outside, else d's parity. */
    static int span(int d, double v) {
        double half = d / 2.0;
        if (Math.abs(v) >= half) {
            return 0;
        }
        double exact = 2 * Math.sqrt(half * half - v * v);
        int w = (int) Math.round(exact);
        if ((w & 1) != (d & 1)) {
            w += exact > w ? 1 : -1;
        }
        return Math.clamp(w, d & 1, d);
    }

    /** One piece of a disc: w x h at u across the centre, its top {@code top} rows below the disc's top. */
    private static void place(Art.Model m, String name, double[] c, double[] q, double u, int h, int w, int top, int d, int t,
                              Art.Mat mat, boolean glow, Art.Decal decal) {
        double v = d / 2.0 - top - h / 2.0;
        double[] at = Blocky.add(c, Art.rotate(new double[]{u, v, 0}, q));
        Art.Box box = m.add(new Art.Box(name, at[0], at[1], at[2], w, h, t, mat).orient(q));
        if (glow) {
            box.glow();
        }
        if (decal != null) {
            int left = (int) Math.round(u - w / 2.0 + d / 2.0);
            box.decal(0, decal, left, top);
            box.decal(1, decal, d - left - w, top);
        }
    }

    /** A square rod from a to b. */
    static Art.Box beam(Art.Model m, String name, double[] a, double[] b, int width, Art.Mat mat) {
        double[] d = Art.sub(b, a);
        double length = Math.sqrt(Art.dot(d, d));
        double[] q = towards(new double[]{0, 1, 0}, Art.norm(d));
        return m.add(new Art.Box(name, (a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2,
                width, (int) Math.max(1, Math.round(length)), width, mat).orient(q));
    }

    /** A square piece along the local y axis of q, from {@code from} to {@code to} past the pivot. */
    static Art.Box segment(Art.Model m, String name, double[] pivot, double[] q, double from, double to, int width, Art.Mat mat) {
        double[] at = Blocky.add(pivot, Art.rotate(new double[]{0, (from + to) / 2, 0}, q));
        return m.add(new Art.Box(name, at[0], at[1], at[2], width, (int) Math.round(to - from), width, mat).orient(q));
    }

    /** The shortest rotation turning unit vector a onto unit vector b. */
    static double[] towards(double[] a, double[] b) {
        double dot = Art.dot(a, b);
        if (dot < -0.9999) {
            return new double[]{1, 0, 0, 0};
        }
        double[] axis = Art.cross(a, b);
        double[] q = {axis[0], axis[1], axis[2], 1 + dot};
        double n = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        return new double[]{q[0] / n, q[1] / n, q[2] / n, q[3] / n};
    }
}
