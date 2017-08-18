package com.nx.util.jme3.base.appstates;

import com.jme3.app.Application;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.ui.Picture;

public class ViewportAppStateBackground extends ViewportAppState {

    Picture picture;
    String texturePath;


    public ViewportAppStateBackground(String texturePath) {
        super(Mode.BACKGROUND, null);
        this.texturePath = texturePath;
    }

    @Override
    protected void initialize(Application app) {
        super.initialize(app);

        picture = new Picture("background");
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", app.getAssetManager().loadTexture(texturePath));
        picture.setMaterial(material);

        picture.setWidth(viewPort.getCamera().getWidth());
        picture.setHeight(viewPort.getCamera().getHeight());

        picture.setQueueBucket(RenderQueue.Bucket.Gui);

        rootNode.attachChild(picture);

        rootNode.updateGeometricState();
    }

    public Picture getPicture() {
        return picture;
    }

    @Override
    public void update(float tpf) { }

    @Override
    public void render(RenderManager rm) { }

    @Override
    protected void cleanup(Application app) {
        super.cleanup(app);

        picture = null;
    }
}
