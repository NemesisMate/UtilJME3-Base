package com.nx.util.jme3.base;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

/**
 *
 * @author NemesisMate
 */
public final class SceneUtil {

    private SceneUtil() {

    }

    /**
     *
     * @param inputManager
     * @param cam
     * @param coordsStore
     * @param dirStore
     * @return dirStore
     */
    @Deprecated
    public static Vector3f CamCoordsToWorld(InputManager inputManager, Camera cam, Vector3f coordsStore, Vector3f dirStore) {
        return CamCoordsToWorld(inputManager.getCursorPosition(), cam, coordsStore, dirStore);
    }


    public static Vector3f CamCoordsToWorld(Vector2f cursor2d, Camera cam, Vector3f coordsStore, Vector3f dirStore) {
        TempVars vars = TempVars.get();

//        Vector2f cursor2d = cursorPos;
        cam.getWorldCoordinates(vars.vect2d.set(cursor2d.x, cursor2d.y), 0f, coordsStore);
        cam.getWorldCoordinates(vars.vect2d, 1f, dirStore).subtractLocal(coordsStore).normalizeLocal();

        vars.release();

        return dirStore;
    }

    @Deprecated
    public static Vector3f getMousePoint(InputManager inputManager, Camera cam,  Spatial spat) {
        return getMousePoint(inputManager.getCursorPosition(), cam, spat);
    }

    public static Vector3f getMousePoint(Vector2f cursor2d, Camera cam,  Spatial spat) {
        SceneVars vars2 = SceneVars.get();

        CollisionResults localResults = getClickResults(cursor2d, cam, spat, vars2.collisionResults, vars2.ray);

        Vector3f contactPoint = null;
        if(localResults.size() > 0) {
            contactPoint = localResults.getClosestCollision().getContactPoint();
        }

        vars2.release();

        return contactPoint;
    }

    @Deprecated
    public static CollisionResult getMouseResult(InputManager inputManager, Camera cam, Spatial spat) {
        return getMouseResult(inputManager.getCursorPosition(), cam, spat);
    }

    public static CollisionResult getMouseResult(Vector2f cursor2d, Camera cam, Spatial spat) {
        SceneVars vars2 = SceneVars.get();

        CollisionResults localResults = getClickResults(cursor2d, cam, spat, vars2.collisionResults, vars2.ray);

        CollisionResult result = null;
        if(localResults.size() > 0) {
            result = localResults.getClosestCollision();
        }

        vars2.release();

        return result;
    }

//    private static CollisionResults getClickResults(InputManager inputManager, Camera cam, Spatial spat, CollisionResults localResults, Ray localRay) {
//        return getClickResults(inputManager.getCursorPosition(), cam, spat, localResults, localRay);
//    }

    private static CollisionResults getClickResults(Vector2f click2d, Camera cam, Spatial spat, CollisionResults localResults, Ray localRay) {
        TempVars vars = TempVars.get();
//        SceneVars vars2 = SceneVars.get();

//        CollisionResults localResults = new CollisionResults();

        // Convert screen click to 3d position
//        Vector2f click2d = inputManager.getCursorPosition();
        Vector3f click3d = cam.getWorldCoordinates(vars.vect2d.set(click2d.x, click2d.y), 0f, vars.vect1);
        // Aim the ray from the clicked spot forwards.
        Vector3f dir = cam.getWorldCoordinates(vars.vect2d, 1f, vars.vect2).subtractLocal(click3d).normalizeLocal();

//        Ray localRay = vars2.ray;
        localRay.setOrigin(click3d);
        localRay.setDirection(dir);
        // Collect intersections between ray and all nodes in results list.
        spat.collideWith(localRay, localResults);

        vars.release();

        return localResults;
    }



    public static CollisionResults getClickResults(Vector2f cursor2d, Camera cam, Spatial spat) {
        SceneVars vars2 = SceneVars.get();

        CollisionResults localResults = getClickResults(cursor2d, cam, spat, new CollisionResults(), vars2.ray);

        vars2.release();

        return localResults;
    }

    @Deprecated
    public static CollisionResults getClickResults(InputManager inputManager, Camera cam, Spatial spat) {
        return getClickResults(inputManager.getCursorPosition(), cam, spat);
    }
    

    
}
