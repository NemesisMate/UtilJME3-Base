package com.nx.util.jme3.base;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.slf4j.LoggerFactory;

public abstract class SpatialAutoManager extends AbstractControl {

    ScenegraphAutoRemoverChecker checker;

    public abstract void onAttached();
    public abstract void onDetached();

    private void detach() {
        checker = null;
        onDetached();
    }

    public boolean isAttached() {
        return checker != null;
    }

    public Node getRootNode() {
        return checker != null ? (Node) checker.getSpatial() : null;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if(checker != null) {
            return;
        }

        Node rootParent = null;
        Node parent = spatial.getParent();
        while (parent != null) {
            rootParent = parent;
            parent = parent.getParent();
        }

        if (rootParent != null) {
//            ViewportPanel.this.rootNode = rootParent;
            checker = new ScenegraphAutoRemoverChecker();
            rootParent.addControl(checker);
            onAttached();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public class ScenegraphAutoRemoverChecker extends AbstractControl {

        boolean flagged;

        public void check() {
            flagged = false;
        }

//            public abstract void onClose();

        @Override
        public void setSpatial(Spatial spatial) {
            if(spatial == null) {
                if(!flagged) {
                    LoggerFactory.getLogger(this.getClass()).warn("Shouldn't be removing this control manually!");
//                    this.spatial.addControl(this); // Re-add??, or just better let the developer see the problem.
                }
                // Not closing here because of this way the developer sees if there is something wrong.
//                    close();
            }

            super.setSpatial(spatial);
        }

        @Override
        protected void controlUpdate(float tpf) {
            if(flagged) {
                detach();
                spatial.removeControl(this);
            } else {
                flagged = true;
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) { }
    }
}