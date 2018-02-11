package com.nx.util.jme3.base;

public final class CameraUtil {

    private CameraUtil() { }

//    public static Vector3f getFocusPosition(Camera cam, Spatial spatial, Vector3f store) {
//
//    }
//    public static float getFocusIdealDistance(Camera cam, Spatial spatial) {
//        BoundingBox bb = (BoundingBox) spatial.getWorldBound();
//
//        if (bb != null) {
//            float x = bb.getXExtent();
//            float y = bb.getYExtent();
//            float z = bb.getZExtent();
//
//
//            float dimensions;
//
//            float bigger = x;
//
//            if(z > bigger) {
//                bigger = z;
//            }
//
//            if (y > bigger) {
//                bigger = y;
//                dimensions = cam.getFrustumTop() - cam.getFrustumBottom();
//            } else {
//                dimensions = cam.getFrustumRight() - cam.getFrustumLeft();
//            }
//
//
//
//            // Teoria de los triangulos semejantes
//            float distance = (bigger * cam.getFrustumNear()) / (dimensions / 2f);
//
//
//
//            store.set(bb.getCenter()).addLocal(0, 0, distance + bigger);
//        }
//
//        return store;
//    }

}
