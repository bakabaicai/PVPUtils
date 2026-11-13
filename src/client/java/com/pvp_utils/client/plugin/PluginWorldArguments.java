package com.pvp_utils.client.plugin;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

final class PluginWorldArguments {
    private PluginWorldArguments() {
    }

    static double distance(Object[] args) {
        if (args.length > 2) throw new IllegalArgumentException("entities accepts at most two arguments");
        return args.length == 0 || args[0] == Undefined.instance ? 64 : Math.clamp(number(args[0]), 1, 512);
    }

    static int limit(Object[] args) {
        if (args.length < 2 || args[1] == Undefined.instance) return 64;
        double value = number(args[1]);
        if (value != Math.rint(value)) throw new IllegalArgumentException("Entity limit must be an integer");
        return (int) Math.clamp(value, 1, 128);
    }

    static double coordinate(Object value) {
        double number = number(value);
        if (Math.abs(number) > 30_000_000) throw new IllegalArgumentException("World coordinate exceeds limit");
        return number;
    }

    static double[] point(Object[] args) {
        if (args.length != 3) throw new IllegalArgumentException("project requires three coordinates");
        return new double[]{coordinate(args[0]), coordinate(args[1]), coordinate(args[2])};
    }

    static double[] points(Object[] args, Runnable budget) {
        if (args.length != 1 || !(args[0] instanceof NativeArray points)) {
            throw new IllegalArgumentException("projectMany requires an array of points");
        }
        long length = points.getLength();
        if (length > 256) throw new IllegalArgumentException("At most 256 points per projection batch");
        double[] result = new double[(int) length * 3];
        for (int i = 0; i < length; i++) {
            budget.run();
            Object value = points.get(i, points);
            if (!(value instanceof Scriptable point) || value instanceof NativeArray) {
                throw new IllegalArgumentException("Each point requires numeric x, y and z");
            }
            result[i * 3] = coordinate(ScriptableObject.getProperty(point, "x"));
            result[i * 3 + 1] = coordinate(ScriptableObject.getProperty(point, "y"));
            result[i * 3 + 2] = coordinate(ScriptableObject.getProperty(point, "z"));
        }
        return result;
    }

    private static double number(Object value) {
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw new IllegalArgumentException("Expected finite numeric argument");
        }
        return number.doubleValue();
    }
}
