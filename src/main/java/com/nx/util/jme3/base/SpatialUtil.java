package com.nx.util.jme3.base;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.SafeArrayList;
import jme3tools.optimize.GeometryBatchFactory;
import org.slf4j.LoggerFactory;

import java.nio.*;
import java.util.*;

import static com.nx.util.jme3.base.DebugUtil.assetManager;

/**
 * TODO: merge all duplicated methods (they can be handled in a single one without any performance impact. Doubling them is just useless... I think.)
 * @author NemesisMate
 */
public final class SpatialUtil {

    private SpatialUtil() {

    }

    public interface Operation {
        
        /**
         * Must stop that rama
         * @return 
         */
        public boolean operate(Spatial spatial);
    }

    public interface ReturnOperation<T extends Object> extends Operation { public T getReturnObject(); }

    // TODO improve visit first to cut on the for if returnedObject on returnoperation == null.
    public static <T extends Operation> T visitNodeWith(Spatial spatial, T operation) {
        if(!operation.operate(spatial)) {
            if(spatial instanceof Node) {
                for(Spatial s : ((Node)spatial).getChildren()) {
                    visitNodeWith(s, operation);
                }
            }
        }

        return operation;
    }

//    public void generateTangents(Spatial task) {
//        if(task == null) {
//            return;
//        }
//
//        task.depthFirstTraversal(new SceneGraphVisitor() {
//            @Override
//            public void visit(Spatial task) {
//                if(task instanceof Geometry) {
//                    Mesh mesh = ((Geometry) task).getMesh();
//                    if(mesh.getBuffer(VertexBuffer.Type.Normal) != null && mesh.getBuffer(VertexBuffer.Type.Tangent) == null) {
//                        TangentBinormalGenerator.generate(mesh);
//                    }
//                }
//            }
//        });
//    }

    /**
     * Prints the scenegraph on the debug console.
     * 
     * @param spatial to print recursively.
     */
    public static void drawGraph(Spatial spatial) {
        System.out.println(spatial);
        
        if(!(spatial instanceof Node)) return;
        
        List<Spatial> spatials = new ArrayList<>();
        //Map<Spatial, Integer> repeated = new HashMap<>();
        List<String> lines = sceneGraphLoop(((Node)spatial).getChildren(), spatials, "", new ArrayList<String>());

        for(String line : lines) {
            System.out.println(line);
        }


        for(Spatial spat : spatials) {
            int count = 0;
            //System.out.println(task.getName());
            for(Spatial spat2 : spatials)
                if(spat == spat2) count++;
                
            if(count > 1){
                System.out.println(spat.getName() + "(REPEATED - IMPOSSIBLE): " + count);
                //repeated.put(task, count);
            }
        }
        System.out.println("Total: " + spatials.size() + "\n\n\n\n");
        
    }

    //TODO
    public static List<String> getDrawGraph(final Spatial spatial) {
        return sceneGraphLoop(new ArrayList<Spatial>(1) {{ add(spatial); }}, new ArrayList<Spatial>(), "", new ArrayList<String>());
    }
    
    private static List<String> sceneGraphLoop(List<Spatial> spatials, List<Spatial> all, String dashes, List<String> lines) {
        dashes += "-";
        for(Spatial spatial : spatials) {
            all.add(spatial);
            
            String data = dashes + spatial;
            if(spatial instanceof Geometry) {
                data += "(" + ((Geometry)spatial).getMaterial().getName() + " <>" + ((Geometry) spatial).getMaterial().getKey() + ")";
            }

            int numControls = spatial.getNumControls();

            data += " - (" + spatial.getTriangleCount() + "t, " + spatial.getVertexCount() +
                    "v) LOC[L:" + spatial.getLocalTranslation() + ", W:" + spatial.getWorldTranslation() +
                    "] SCALE[L:" + spatial.getLocalScale() + ", W:" + spatial.getWorldScale() +
                    "] ROT[L:" + spatial.getLocalRotation() + ", W:" + spatial.getWorldRotation() +  "]" +
                    " - Controls: " + numControls;

            if(numControls > 0) {
                data += " (";
                for(int i = 0; i < numControls; i++) {
                    Control control = spatial.getControl(i);
                    data += control.getClass().getSimpleName() + ", ";
                }

                data = data.substring(0, data.length() - 2);
                data += ")";
            }
            
//            System.out.println(data);
//            stringBuilder.append('\n' + data);
            lines.add(data);
            
            if(spatial instanceof Node) {
                sceneGraphLoop(((Node)spatial).getChildren(), all, dashes, lines);
            }
            
        }

        return lines;
    }



    public static Geometry gatherFirstGeom(Spatial spatial) {
        if (spatial instanceof Node) {
            for (Spatial child : ((SafeArrayList<Spatial>)((Node)spatial).getChildren()).getArray()) {
                return gatherFirstGeom(child);
            }
        } else if (spatial instanceof Geometry) {
            return (Geometry) spatial;
        }

        return null;
    }

    public static List<Mesh> gatherMeshes(Spatial spatial, List<Mesh> meshStore) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
                gatherMeshes(child, meshStore);
            }
        } else if (spatial instanceof Geometry) {
            meshStore.add(((Geometry) spatial).getMesh());
        }

        return meshStore;
    }

    public static void gatherGeoms(Spatial spatial, Mesh.Mode mode, List<Geometry> geomStore) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
                gatherGeoms(child, mode, geomStore);
            }
        } else if (spatial instanceof Geometry && ((Geometry) spatial).getMesh().getMode() == mode) {
            geomStore.add((Geometry) spatial);
        }
    }

    public static Node getAllBounds(Spatial spatial) {
        final Node bounds = new Node(spatial.getName() + "_BOUNDS");

        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if(spatial instanceof Geometry) {
                    BoundingBox bb = (BoundingBox) spatial.getWorldBound();
                    Box b = new Box(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
                    Geometry geom = new Geometry(null, b);

                    bounds.attachChild(geom);
                } else {
                    if(((Node)spatial).getQuantity() == 0) {
                        Sphere b = new Sphere(10, 10, 0.25f * spatial.getWorldScale().length());
                        Geometry geom = new Geometry(null, b);

                        bounds.attachChild(geom);
                    }
                }
            }
        });

        return bounds;
    }

    public static Map<Spatial, Geometry> getBounds(Spatial spatial, Map<Spatial, Geometry> bounds) {
        if(bounds == null) {
            bounds = new HashMap<>();
        }

        final Map<Spatial, Geometry> finalBounds = bounds;

        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if(spatial instanceof Geometry) {
                    BoundingBox bb = (BoundingBox) spatial.getWorldBound();
                    Box b = new Box(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
                    Geometry geom = new Geometry(null, b);

                    finalBounds.put(spatial, geom);
                } else {
                    if(((Node)spatial).getQuantity() == 0) {
                        Sphere b = new Sphere(10, 10, 0.25f * spatial.getWorldScale().length());
                        Geometry geom = new Geometry(null, b);

                        finalBounds.put(spatial, geom);
                    }
                }
            }
        });

        return finalBounds;
    }

    public static boolean isChild(Spatial child, Spatial parent) {
        if(child == parent) {
            return true;
        }

        if(parent instanceof Node) {
            for (Spatial c : (((SafeArrayList<Spatial>)((Node)parent).getChildren()).getArray())) {
                if(isChild(child, c)) {
                    return true;
                }
            }
        }

        return false;
    };



    /**
     * Finds a task on the given task (usually, a node).
     * 
     * @param spatial
     * @param name
     * @return 
     */
    public static Spatial find(Spatial spatial, final String name) {
        if(spatial != null) {
            String spatName = spatial.getName();
            if(spatName != null && spatName.equals(name)) return spatial;
//            if(task instanceof Node) return nodeFind((Node)task, name);
            if(spatial instanceof Node) return ((Node)spatial).getChild(name);
        }
        
        return null;
    }

    public static Geometry findGeometry(Spatial spatial, final String name) {
        if(spatial != null) {
            String spatName = spatial.getName();

//            if(task instanceof Node) return nodeFind((Node)task, name);
            if(spatial instanceof Node) {
                return findGeometry((Node)spatial, name);
            } else {
                if(spatName != null && spatName.equals(name)) return (Geometry) spatial;
            }
        }

        return null;
    }

    public static Geometry findGeometry(Node node, String name) {
        for (Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
            String spatName = child.getName();
            if(child instanceof Node) {
                Geometry out = findGeometryStartsWith((Node)child, name);
                if(out != null) {
                    return out;
                }
            } else {
                if (spatName != null && spatName.equals(name)) {
                    return (Geometry) child;
                }
            }
        }

        return null;
    }

    public static List<Spatial> findAllStartsWith(Spatial spatial, final String name, List<Spatial> storeList, boolean nested) {
        if(spatial != null) {
            String spatName = spatial.getName();
            if(spatName != null && spatName.startsWith(name)) {
                storeList.add(spatial);

                if(nested) {
                    if(spatial instanceof Node) {
                        findAllStartsWith((Node)spatial, name, storeList, nested);
                    }
                }

            } else if(spatial instanceof Node) {
                return findAllStartsWith((Node)spatial, name, storeList, nested);
            }
        }

        return storeList;
    }

    public static List<Spatial> findAllStartsWith(Node node, final String name, List<Spatial> storeList, boolean nested) {
        for (Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
            String spatName = child.getName();
            if (spatName != null && spatName.startsWith(name)) {
                storeList.add(child);
                if(nested) {
                    if(child instanceof Node) {
                        findAllStartsWith((Node)child, name, storeList, nested);
                    }
                }
            } else if(child instanceof Node) {
                findAllStartsWith((Node)child, name, storeList, nested);
            }
        }

        return storeList;
    }

    public static Spatial findStartsWith(Spatial spatial, final String name) {
        if(spatial != null) {
            String spatName = spatial.getName();
            if(spatName != null && spatName.startsWith(name)) return spatial;
//            if(task instanceof Node) return nodeFind((Node)task, name);
            if(spatial instanceof Node) {
                return findStartsWith((Node)spatial, name);
            }
        }

        return null;
    }

    public static Spatial findStartsWith(Node node, String name) {
        for (Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
            String spatName = child.getName();
            if (spatName != null && spatName.startsWith(name)) {
                return child;
            } else if(child instanceof Node) {
                Spatial out = findStartsWith((Node)child, name);
                if(out != null) {
                    return out;
                }
            }
        }

        return null;
    }


    public static String getUserDataKeyStartsWith(Spatial spatial, final String name) {
        Collection<String> keys = spatial.getUserDataKeys();

        if(keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                if(key.startsWith(name)) {
                    return key;
                }
            }
        }

        return null;
    }


    public static <T> Collection<T> getAllUserDataStartsWith(Spatial spatial, final String name) {
        Collection<String> keys = spatial.getUserDataKeys();

        if(keys != null && !keys.isEmpty()) {
            Collection<T> userDatas = new ArrayList<T>(keys.size());

            for (String key : keys) {
                if(key.startsWith(name)) {
                    userDatas.add((T) spatial.getUserData(key));
                }
            }

            return userDatas;
        }

        return null;
    }

    public static <T> T getUserDataStartsWith(Spatial spatial, final String name) {
        Collection<String> keys = spatial.getUserDataKeys();

        if(keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                if(key.startsWith(name)) {
                    return spatial.getUserData(key);
                }
            }
        }

        return null;
    }

    public static List<Spatial> findAllWithDataStartsWith(Spatial spatial, final String name, List<Spatial> storeList, boolean nested) {
        if(spatial != null) {
            Collection<String> keys = spatial.getUserDataKeys();
            if(keys != null && !keys.isEmpty()) {
                for(String key : keys) {
                    if(key.startsWith(name)) {
                        storeList.add(spatial);
                        if(!nested) {
                            return storeList;
                        } else {
                            break;
                        }
                    }
                }

            }

            if(spatial instanceof Node) {
                return findAllWithDataStartsWith((Node) spatial, name, storeList, nested);
            }
        }

        return storeList;
    }

    public static List<Spatial> findAllWithDataStartsWith(Node node, final String name, List<Spatial> storeList, boolean nested) {
        for (Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
            findAllWithDataStartsWith(child, name, storeList, nested);
        }

        return storeList;
    }




    public static Geometry findGeometryStartsWith(Spatial spatial, final String name) {
        if(spatial != null) {
            String spatName = spatial.getName();

//            if(task instanceof Node) return nodeFind((Node)task, name);
            if(spatial instanceof Node) {
                return findGeometryStartsWith((Node)spatial, name);
            } else {
                if(spatName != null && spatName.startsWith(name)) return (Geometry) spatial;
            }
        }

        return null;
    }

    public static Geometry findGeometryStartsWith(Node node, String name) {
        for (Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
            String spatName = child.getName();
            if(child instanceof Node) {
                Geometry out = findGeometryStartsWith((Node)child, name);
                if(out != null) {
                    return out;
                }
            } else {
                if (spatName != null && spatName.startsWith(name)) {
                    return (Geometry) child;
                }
            }
        }

        return null;
    }



    public static Spatial findHasUserData(Node node, final String key) {
        if(node != null) {
            for(Spatial spatial : node.getChildren()) {
                Spatial found = findHasUserData(spatial, key);
                if(found != null) {
                    return found;
                }
            }
        }

        return null;
    }


    public static Spatial findHasUserData(Spatial spatial, final String key) {
        if(spatial != null) {
            if(spatial.getUserData(key) != null) {
                return spatial;
            }
//            if(task instanceof Node) return nodeFind((Node)task, name);
            if(spatial instanceof Node) {
                return findHasUserData((Node)spatial, key);
            }
        }

        return null;
    }


    
    // NOT NEEDED, getChild(name) does the same thing
//    private static Spatial nodeFind(Node node, final String name) {
//        Spatial ret = null;
//        
//        for(Spatial spat : node.getChildren()) {
//            String spatName = spat.getName();
//            
//            if(spatName != null && spat.getName().equals(name)) return spat;
//            
//            if(spat instanceof Node) {
//                ret = nodeFind((Node)spat, name);
//                if(ret != null) return ret;
//            }
//            
//        }
//        
//        return ret;
//    }
    
    /**
     * Not safe (not enough tested).
     * 
     * Resizes a task without altering it scale.
     * TODO: make sure the animations still fine.
     * 
     * @param spatial
     * @param scale 
     */
    public static void resize(Spatial spatial, final float scale) {
        SceneGraphVisitor visitor = new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spat) {
                if(spat instanceof Geometry) {
                    VertexBuffer pb = ((Geometry)spat).getMesh().getBuffer(VertexBuffer.Type.Position);
                    FloatBuffer positions = (FloatBuffer) pb.getData();

                    for(int i = 0; i < positions.capacity(); i++) {
                        positions.put(i, positions.get(i) * scale);
                    }
                }
            }
        };
        
        spatial.depthFirstTraversal(visitor);
//        task.updateGeometricState();
//        task.updateModelBound();
        
        SkeletonControl control = spatial.getControl(SkeletonControl.class);
        if(control == null) return;
        
        for(Bone bone : control.getSkeleton().getRoots()) {
            // If don't want to change the whole model (only the instance on the task) uncomment
            // the following two lines (bone.setUser..., and comment bone.setBindTransforms(...)
            bone.setUserControl(true);
            bone.setUserTransforms(new Vector3f(Vector3f.ZERO), Quaternion.IDENTITY, new Vector3f(scale, scale, scale));
            //bone.setBindTransforms(Vector3f.ZERO, Quaternion.IDENTITY, new Vector3f(scale, scale, scale));
        }
        
//        AnimControl animControl = task.getControl(AnimControl.class);
//        if(animControl == null) return;
//        
//        
//        animControl.getAnim("").getTracks()[0]
    }
    
    /**
     * Translates the internal geometries of this task without altering the task local translation.
     * @param spatial
     * @param translation 
     */
    public static void translateGeom(final Spatial spatial, final Vector3f translation) {
        SceneGraphVisitor visitor = new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spat) {
                if(spat instanceof Geometry) {
//                    VertexBuffer pb = ((Geometry)spat).getMesh().getBuffer(VertexBuffer.Type.Position);
//                    FloatBuffer positions = (FloatBuffer) pb.getData();
//
//                    for(int i = 0; i < positions.capacity(); i++)
//                        positions.put(i, positions.get(i) + (1f/task.getLocalScale().y));
                    
                    //spat.setLocalTranslation(new Vector3f(0f, 1f, 0f).multLocal(Vector3f.UNIT_XYZ.divide(task.getLocalScale())));
                    spat.setLocalTranslation(translation.divide(spatial.getLocalScale()));
                }
            }
        };
        
        spatial.depthFirstTraversal(visitor);
    }
    
    /**
     * Rotates the internal geometries of this task without altering the task local Rotates.
     * 
     * @param spatial
     * @param angle
     * @param axis 
     */
    public static void rotateGeom(final Spatial spatial, final float angle, final Vector3f axis) {
        SceneGraphVisitor visitor = new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spat) {
                if(spat instanceof Geometry)
                    spat.getLocalRotation().fromAngleAxis(angle, axis);
                    //spat.setLocalRotation(rotation);
            }
        };
        
        spatial.depthFirstTraversal(visitor);
    }
    
    /**
     * Optimizes Geometries and Nodes for the given task.
     * Basically, performs a {@link #optimizeGeoms(Spatial)} followed by a {@link #optimizeNodes(Spatial)}
     * 
     * @param spatial to optimize.
     */
    public static void optimizeAll(Spatial spatial) {
        optimizeGeoms(spatial);
        optimizeNodes(spatial);
    }
    
    /**
     * Optimizes Geometries for the given task.
     * 
     * @param spatial to optimize.
     */
    public static void optimizeGeoms(Spatial spatial) {
        if(spatial instanceof Node) {
            GeometryBatchFactory.optimize((Node)spatial);
//            System.gc();
        }
    }
    
    /**
     * Optimizes Nodes for the given task.
     * 
     * @param spatial to optimize.
     */
    public static void optimizeNodes(Spatial spatial) {
        SceneGraphVisitor visitor = new SceneGraphVisitor() {
//            public int i = 0;
//            Node compareNode = new Node("node");
            @Override
            public void visit(Spatial spat) {
//                SceneGraphVisitor visitor = new SceneGraphVisitor() {
//                    @Override
//                    public void visit(Spatial spat) {
//                        
//                    }
//                
//                };
                if(spat instanceof Node) {
                    if(((Node)spat).getChildren().isEmpty()) spat.removeFromParent();
                    else for(Spatial spat2 : ((Node)spat).getChildren()) {
                        if(spat2 instanceof Node) {
                            if(spat2.getLocalScale().equals(Vector3f.UNIT_XYZ) &&
                               spat2.getLocalTranslation().equals(Vector3f.ZERO) &&
                               spat2.getLocalRotation().equals(Quaternion.IDENTITY) &&
                               spat2.getLocalLightList().size() == 0) {
//                            spat2.setName("node");
//                            if(spat2.equals(compareNode)) {
//                                System.out.println("Nodo sobrante");
//                                i++;
                                for(Spatial spat3 :((Node)spat2).getChildren())
                                    ((Node)spat).attachChild(spat3);
                                
                                spat.removeFromParent(); // igual a ((Node)spat).detachChild(spat2);
                            }
                        }
                    }
                    //spat.depthFirstTraversal(visitor);
                }
//                System.out.println("Total: " + i);
            }
        };
        
        spatial.depthFirstTraversal(visitor);
//        System.gc();
    }

    public static boolean hasControl(Spatial spat, Control control) {
        int numControl = spat.getNumControls();

        for(int i = 0; i < numControl; i++) {
            if(spat.getControl(i) == control) {
                return true;
            }
        }

        return false;
    }

    public static Collection<AnimControl> getAnimControlsFor(Spatial spat) {
        return getControlsFor(spat, AnimControl.class);
    }

    public static AnimControl getFirstAnimControlFor(Spatial spat) {
        return getFirstControlFor(spat, AnimControl.class);
    }



    public static void removeControlsFor(Spatial spat, final Class<? extends Control> controlClass) {
//        List<T> controls = new ArrayList<>(3);

        spat.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                Control control = spatial.getControl(controlClass);
                if(control != null) {
                    spatial.removeControl(control);
//                    controls.add(control);
                }
            }
        });

//        return controls;
    }

    public static <T extends Control> Collection<T> getControlsNoRecursive(Spatial spat, Class<T> controlClass) {
        ArrayList<T> controls = new ArrayList<>(3);

        int numControls = spat.getNumControls();

        for(int i = 0; i < numControls; i++) {
            Control control = spat.getControl(i);
            if(controlClass.isAssignableFrom(control.getClass())) {
                controls.add((T) control);
            }
        }
        controls.trimToSize();

        return controls;
    }

    public static <T extends Control> Collection<T> getControlsFor(Spatial spat, final Class<T> controlClass) {
        final List<T> controls = new ArrayList<>(3);

        spat.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                T control = spatial.getControl(controlClass);
                if(control != null) {
                    controls.add(control);
                }
            }
        });

        return controls;
    }



    public static <T extends Control> T getFirstControlFor(Spatial spat, final Class<T> controlClass) {
        return visitNodeWith(spat, new Operation() {

            T firstControl;

            @Override
            public boolean operate(Spatial spatial) {
                if(firstControl != null) {
                    return true;
                }

                T control = spatial.getControl(controlClass);
                if(control != null) {
                    this.firstControl = control;
                    return true;
                }

                return false;
            }
        }).firstControl;
    }

    public static <T extends AbstractControl> T getFirstEnabledControlFor(Spatial spat, final Class<T> controlClass) {
        return visitNodeWith(spat, new Operation() {

            T firstControl;

            @Override
            public boolean operate(Spatial spatial) {
                if(firstControl != null) {
                    return true;
                }

                T control = spatial.getControl(controlClass);
                if(control != null && control.isEnabled()) {
                    this.firstControl = control;
                    return true;
                }

                return false;
            }
        }).firstControl;
    }

    public static Geometry createArrow(Vector3f direction) {
        Geometry geometry = new Geometry("SU Created Arrow", new Arrow(direction));
        if(geometry.getMesh().getBuffer(VertexBuffer.Type.Normal) == null) {
            int vCount = geometry.getMesh().getVertexCount();
            float[] buff = new float[vCount * 3];
            for(int i = 0; i < vCount; i++) {
                buff[i] = 0;
            }

            geometry.getMesh().setBuffer(VertexBuffer.Type.Normal, 3, buff);
        } else {
            LoggerFactory.getLogger(SpatialUtil.class).error("I know, this is not an error, but all this normal buffers stuff is not more needed.");
        }
        return geometry;
    }

    /**
     * Use the DebugUtil getDebugArrow instead.
     * @param direction
     * @param color
     * @return
     */
    @Deprecated
    public static Geometry createArrow(Vector3f direction, ColorRGBA color) {
        Arrow arrow = new Arrow(direction);
        Geometry geometry = new Geometry("SU Created Arrow", arrow);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        if(color == null) {
            color = ColorRGBA.randomColor();
        }

        material.setColor("Color", color);

        geometry.setMaterial(material);

        return geometry;
    }


    public static Geometry createBox(float size) {
        return createBox(size, size, size);
    }

    public static Geometry createBox(Vector3f extents) {
        return createBox(extents.x, extents.y, extents.z);
    }

    public static Geometry createBox(float halfX, float halfY, float halfZ) {
        return new Geometry("SU Created Box", new Box(halfX, halfY, halfZ));  // create cube geometry from the shape
    }

    public static Geometry createSphere(float size) {
        return new Geometry("SU Created Sphere", new Sphere(10, 10, size));
    }

    public static Geometry createSphere(Vector3f extents) {
        return createSphere(extents.x, extents.y, extents.z);
    }

    public static Geometry createSphere(float halfX, float halfY, float halfZ) {
        if(halfY > halfX) {
            if(halfZ > halfY) {
                return createSphere(halfZ);
            }
            return createSphere(halfY);
        }

        return createSphere(halfX);
    }

    public static Material createMaterial(AssetManager assetManager, ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color != null ? color : ColorRGBA.randomColor());

        return mat;
    }

    //TODO: redo the full split method having in mind it optimization for usages with substring (avoiding it intensive current new allocation)
    public static List<String> splitTextIntoLines(BitmapFont font, float maxChatWidth, String text, List<String> finalMessage) {
        if(finalMessage == null) {
            finalMessage = new ArrayList<>();
        } else {
            if(!(finalMessage instanceof ArrayList)) {
//                LoggerFactory.getLogger(SpatialUtil.class).error("The current implementation only supports ArrayList");
                throw new UnsupportedOperationException("The current implementation only supports ArrayList");
            }
        }

        finalMessage = splitTextIntoLines(font, maxChatWidth, finalMessage, text.split(" "), 0);

        ((ArrayList)finalMessage).trimToSize();
        return finalMessage;
    }

    private static List<String> splitTextIntoLines(BitmapFont font, float maxChatWidth, List<String> finalMessage, String[] words, int index) {
        String current;
        String substring;
        float accumulated = 0;
        float whiteSpaceSize = font.getLineWidth(" ");
        float dashSpaceSize = font.getLineWidth("-");

        if(finalMessage.isEmpty()) finalMessage.add("");
        else accumulated = font.getLineWidth(finalMessage.get(finalMessage.size() - 1)) + whiteSpaceSize;

        for(;index < words.length; ++index) {
            accumulated += font.getLineWidth(words[index]) + whiteSpaceSize;
            if(accumulated > maxChatWidth) {
                // If the word itself is too big
                if(font.getLineWidth(words[index]) > maxChatWidth) {
                    if(words[index].length() <= 1) {
//                        LoggerFactory.getLogger(SpatialUtil.class).error("Given max width too small: {}", maxChatWidth);
                        throw new UnsupportedOperationException("Given max width too small: " + maxChatWidth);
                    }

                    int wordIndex = 0;
                    while(wordIndex < words[index].length()) {
                        substring = words[index].substring(wordIndex, words[index].length());
                        float size = font.getLineWidth(substring) + dashSpaceSize;
                        while(size > maxChatWidth) {
                            substring = substring.substring(0, substring.length()/2);
                            size = font.getLineWidth(substring) + dashSpaceSize;
                        }

                        // < because we have to add the "-" character at the end of that word
                        int wordEnd = wordIndex + substring.length();
                        while(size < maxChatWidth && wordEnd < words[index].length()) {
                            substring = words[index].substring(wordIndex, wordEnd++);
                            size = font.getLineWidth(substring) + dashSpaceSize;
                        }

                        wordIndex += substring.length();

                        int currentIndex = finalMessage.size() - 1;
                        current = finalMessage.get(currentIndex);
                        if(current.length() != 0) {
                            current = substring; //new StringBuilder(substring);
                            finalMessage.add(current);
                            currentIndex++;
                        } else current += substring;

                        if(wordIndex < words[index].length()) current += "-";
                        else current += " ";

                        finalMessage.set(currentIndex, current);
                    }

                    return splitTextIntoLines(font, maxChatWidth, finalMessage, words, index + 1);

                }
                else {
                    accumulated = font.getLineWidth(words[index]) + whiteSpaceSize;
                    finalMessage.add("");
                }
            }

            int currentIndex = finalMessage.size() - 1;
            current = finalMessage.get(currentIndex) + words[index];

            if(index < words.length - 1) current += " ";

            finalMessage.set(currentIndex, current);
        }

        return finalMessage;
    }

    public static List<StringBuilder> splitTextIntoLines2(BitmapFont font, float maxChatWidth, String text, List<StringBuilder> finalMessage) {
        if(finalMessage == null) {
            finalMessage = new ArrayList<>();
        } else {
            if(!(finalMessage instanceof ArrayList)) {
//                LoggerFactory.getLogger(SpatialUtil.class).error("The current implementation only supports ArrayList");
                throw new UnsupportedOperationException("The current implementation only supports ArrayList");
            }
        }

        finalMessage = splitTextIntoLines2(font, maxChatWidth, finalMessage, text.split(" "), 0);

        ((ArrayList)finalMessage).trimToSize();
        return finalMessage;
    }

    private static List<StringBuilder> splitTextIntoLines2(BitmapFont font, float maxChatWidth, List<StringBuilder> finalMessage, String[] words, int index) {
        StringBuilder current;
        String substring;
        float accumulated = 0;
        float whiteSpaceSize = font.getLineWidth(" ");
        float dashSpaceSize = font.getLineWidth("-");

        if(finalMessage.isEmpty()) finalMessage.add(new StringBuilder(""));
        else accumulated = font.getLineWidth(finalMessage.get(finalMessage.size() - 1).toString()) + whiteSpaceSize;

        for(;index < words.length; ++index) {
            accumulated += font.getLineWidth(words[index]) + whiteSpaceSize;
            if(accumulated > maxChatWidth) {
                // If the word itself is too big
                if(font.getLineWidth(words[index]) > maxChatWidth) {

                    int wordIndex = 0;
                    while(wordIndex < words[index].length()) {
                        substring = words[index].substring(wordIndex, words[index].length());
                        float size = font.getLineWidth(substring) + dashSpaceSize;
                        while(size > maxChatWidth) {
                            substring = substring.substring(0, substring.length()/2);
                            size = font.getLineWidth(substring) + dashSpaceSize;
                        }

                        // < because we have to add the "-" character at the end of that word
                        int wordEnd = wordIndex + substring.length();
                        while(size < maxChatWidth && wordEnd < words[index].length()) {
                            substring = words[index].substring(wordIndex, wordEnd++);
                            size = font.getLineWidth(substring) + dashSpaceSize;
                        }

                        wordIndex += substring.length();

                        current = finalMessage.get(finalMessage.size() - 1);
                        if(current.length() != 0) {
                            current = new StringBuilder(substring);
                            finalMessage.add(current);
                        } else current.append(substring);

                        if(wordIndex < words[index].length()) current.append("-");
                        else current.append(" ");
                    }

                    return splitTextIntoLines2(font, maxChatWidth, finalMessage, words, index+1);

                }
                else {
                    accumulated = font.getLineWidth(words[index]) + whiteSpaceSize;
                    finalMessage.add(new StringBuilder(""));
                }
            }

            current = finalMessage.get(finalMessage.size() - 1).append(words[index]);
            if(index < words.length - 1) current.append(" ");
        }
        return finalMessage;
    }


//    public static void enablePhysicsFor(Spatial spatial) {
//        spatial.depthFirstTraversal(new SceneGraphVisitor() {
//
//            @Override
//            public void visit(Spatial spatial) {
//                RigidBodyControl rigid = spatial.getControl(RigidBodyControl.class);
//                if(rigid != null) {
//                    //                    rigid.setPhysicsLocation(new Vector3f(0, 2, 0));
//                    rigid.setEnabled(true);
//                    rigid.activate();
//                }
//            }
//        });
//    }
//
//    public static void disablePhysicsFor(Spatial spatial) {
//        spatial.depthFirstTraversal(new SceneGraphVisitor() {
//
//            @Override
//            public void visit(Spatial spatial) {
//                RigidBodyControl rigid = spatial.getControl(RigidBodyControl.class);
//                if(rigid != null) {
////                    rigid.clearForces();
////                    rigid.setApplyPhysicsLocal(true);
//                    rigid.setEnabled(false);
//                }
//            }
//        });
//    }

    public static boolean hasGeometry(Spatial spatial) {
        if(spatial instanceof Node) {
            for(Spatial s : ((Node) spatial).getChildren()) {
                if(hasGeometry(s)) {
                    return true;
                }
            }
        } else {
            return true;
        }

        return false;
    }

    public static Spatial getDirectChild(Node node, String childName) {
        for(Spatial child : ((SafeArrayList<Spatial>)node.getChildren()).getArray()) {
            if(childName.equals(child.getName())) {
                return child;
            }
        }

        return null;
    }



    public static void offsetMesh(Mesh mesh, Vector3f offset) {
        FloatBuffer buffer = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        for(int i = 0; i < buffer.capacity(); i+=3) {
            buffer.put(i, buffer.get(i) + offset.x);
            buffer.put(i+1, buffer.get(i+1) + offset.y);
            buffer.put(i+2, buffer.get(i+2) + offset.z);
        }

        mesh.updateBound();
    }


    public static int collidesWithBounds(Collidable other, CollisionResults results, Spatial spatial) {
        BoundingVolume bv = spatial.getWorldBound();
        if(bv == null) {
            return 0;
        }

        int total = bv.collideWith(other, results);
        if(total != 0) {
            return total;
        }

        if(spatial instanceof Node) {
            for(Spatial child : ((SafeArrayList<Spatial>)((Node) spatial).getChildren()).getArray()) {
                total += collidesWithBounds(other, results, child);
            }
        }

        return total;
    }


    public static boolean meshEquals(Mesh mesh1, Mesh mesh2) {
        if(mesh1 == mesh2) {
            return true;
        }

        if(mesh1 == null || mesh2 == null) {
            return false;
        }

        if(mesh1.getVertexCount() != mesh2.getVertexCount()) {
            return false;
        }

        if(mesh1.getTriangleCount() != mesh2.getTriangleCount()) {
            return false;
        }

        if(mesh1.getMode() != mesh2.getMode()) {
            return false;
        }

        Collection<VertexBuffer> buffers1 = mesh1.getBufferList();
        Collection<VertexBuffer> buffers2 = mesh2.getBufferList();

        if(buffers1.size() != buffers2.size()) {
            return false;
        }

        outer:
        for(VertexBuffer vertexBuffer1 : buffers1) {
            for(VertexBuffer vertexBuffer2 : buffers2) {
                if(vertexBuffer1.getBufferType() == vertexBuffer2.getBufferType()) {
                    if(vertexBuffer1.getFormat() != vertexBuffer2.getFormat()) {
                        return false;
                    }

                    if(vertexBuffer1.getUsage() != vertexBuffer2.getUsage()) {
                        return false;
                    }

                    if(vertexBuffer1.getNumElements() != vertexBuffer2.getNumElements()) {
                        return false;
                    }

                    if(vertexBuffer1.getNumComponents() != vertexBuffer2.getNumComponents()) {
                        return false;
                    }

                    Buffer data1 = vertexBuffer1.getData();
                    Buffer data2 = vertexBuffer2.getData();

                    if (data1 instanceof FloatBuffer) {
                        FloatBuffer buf1 = (FloatBuffer) data1;
                        FloatBuffer buf2 = (FloatBuffer) data2;

                        int capacity = buf1.capacity();
                        for(int i = 0; i < capacity; i++) {
                            if(buf1.get(i) != buf2.get(i)) {
                                return false;
                            }
                        }
                    } else if (data1 instanceof ShortBuffer) {
                        ShortBuffer buf1 = (ShortBuffer) data1;
                        ShortBuffer buf2 = (ShortBuffer) data2;

                        int capacity = buf1.capacity();
                        for(int i = 0; i < capacity; i++) {
                            if(buf1.get(i) != buf2.get(i)) {
                                return false;
                            }
                        }
                    } else if (data1 instanceof ByteBuffer) {
                        ByteBuffer buf1 = (ByteBuffer) data1;
                        ByteBuffer buf2 = (ByteBuffer) data2;

                        int capacity = buf1.capacity();
                        for(int i = 0; i < capacity; i++) {
                            if(buf1.get(i) != buf2.get(i)) {
                                return false;
                            }
                        }
                    } else if (data1 instanceof IntBuffer) {
                        IntBuffer buf1 = (IntBuffer) data1;
                        IntBuffer buf2 = (IntBuffer) data2;

                        int capacity = buf1.capacity();
                        for(int i = 0; i < capacity; i++) {
                            if(buf1.get(i) != buf2.get(i)) {
                                return false;
                            }
                        }
                    } else if (data1 instanceof DoubleBuffer) {
                        DoubleBuffer buf1 = (DoubleBuffer) data1;
                        DoubleBuffer buf2 = (DoubleBuffer) data2;

                        int capacity = buf1.capacity();
                        for(int i = 0; i < capacity; i++) {
                            if(buf1.get(i) != buf2.get(i)) {
                                return false;
                            }
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }



                    continue outer;
                }
            }

            return false;
        }

        return true;
    }

}
