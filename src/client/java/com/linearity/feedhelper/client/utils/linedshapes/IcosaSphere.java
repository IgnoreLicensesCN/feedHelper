package com.linearity.feedhelper.client.utils.linedshapes;

import org.joml.Vector3f;

public class IcosaSphere {
    private static final float PHI = (1 + (float)Math.sqrt(5)) / 2;

    // 顶点
    public static final Vector3f[] VERTICES = {
            new Vector3f(-1,  PHI, 0),
            new Vector3f( 1,  PHI, 0),
            new Vector3f(-1, -PHI, 0),
            new Vector3f( 1, -PHI, 0),

            new Vector3f(0, -1,  PHI),
            new Vector3f(0,  1,  PHI),
            new Vector3f(0, -1, -PHI),
            new Vector3f(0,  1, -PHI),

            new Vector3f( PHI, 0, -1),
            new Vector3f( PHI, 0,  1),
            new Vector3f(-PHI, 0, -1),
            new Vector3f(-PHI, 0,  1),
    };

    // 30 条边
    public static final int[][] EDGES = {
            {0,1},{0,5},{0,7},{0,10},{0,11},
            {1,5},{1,7},{1,8},{1,9},
            {2,3},{2,4},{2,6},{2,10},{2,11},
            {3,4},{3,6},{3,8},{3,9},
            {4,5},{4,9},{4,11},
            {5,9},{5,11},
            {6,7},{6,8},{6,10},
            {7,8},{7,10},
            {8,9},
            {10,11}
    };
}
