package com.linearity.feedhelper.client.utils.linedshapes;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class OutlineBalls {

    record SphereKey(float radius, int lat, int lon) {

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SphereKey(float radius1, int lat1, int lon1))) return false;
            return lat == lat1 && lon == lon1 && Float.compare(radius, radius1) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(radius, lat, lon);
        }
    }

    public record Line(Vector3f a, Vector3f b) {}


    private static List<Line> getSphereLines(float radius, int latDivisions, int lonDivisions) {

        SphereKey key = new SphereKey(radius, latDivisions, lonDivisions);

//        return CACHE.computeIfAbsent(key, k -> {

            List<Line> lines = new ArrayList<>();

            // ---- 生成纬线 ----
            for (int lat = 0; lat <= latDivisions; lat++) {

                float theta = (float) Math.PI * lat / latDivisions;

                for (int lon = 0; lon < lonDivisions; lon++) {

                    float phi0 = 2f * (float) Math.PI * lon / lonDivisions;
                    float phi1 = 2f * (float) Math.PI * (lon + 1) / lonDivisions;

                    Vector3f p0 = sphericalToCartesian(radius, theta, phi0);
                    Vector3f p1 = sphericalToCartesian(radius, theta, phi1);

                    lines.add(new Line(p0, p1));
                }
            }

            // ---- 生成经线 ----
            for (int lon = 0; lon < lonDivisions; lon++) {

                float phi = 2f * (float) Math.PI * lon / lonDivisions;

                for (int lat = 0; lat < latDivisions; lat++) {

                    float theta0 = (float) Math.PI * lat / latDivisions;
                    float theta1 = (float) Math.PI * (lat + 1) / latDivisions;

                    Vector3f p0 = sphericalToCartesian(radius, theta0, phi);
                    Vector3f p1 = sphericalToCartesian(radius, theta1, phi);

                    lines.add(new Line(p0, p1));
                }
            }

            return lines;
//        });
    }
    private static final Map<SphereKey, List<Line>> SPHERE_LINES_CACHE = new ConcurrentHashMap<>();

    public static List<Line> getSphereLinesCached(float radius, int latDivisions, int lonDivisions) {
        SphereKey key = new SphereKey(radius, latDivisions, lonDivisions);
        return SPHERE_LINES_CACHE.computeIfAbsent(key, k -> getSphereLines(radius, latDivisions, lonDivisions));
    }



    /** 球面转直角坐标 */
    private static Vector3f sphericalToCartesian(float radius, float lat, float lon) {
        float x = (float)(radius * Math.sin(lat) * Math.cos(lon));
        float y = (float)(radius * Math.cos(lat));
        float z = (float)(radius * Math.sin(lat) * Math.sin(lon));
        return new Vector3f(x, y, z);
    }
}
