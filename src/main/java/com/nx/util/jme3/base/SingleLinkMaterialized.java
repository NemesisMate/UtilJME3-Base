package com.nx.util.jme3.base;

import com.jme3.asset.MaterialKey;
import com.jme3.asset.ModelKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Spatial;

import java.io.IOException;

/**
 * Created by NemesisMate on 28/04/17.
 *
 * Based on: {@link com.jme3.scene.AssetLinkNode}
 */
public class SingleLinkMaterialized extends SingleLinkNode {

    MaterialKey materialKey;


    public SingleLinkMaterialized() {
    }

    public SingleLinkMaterialized(ModelKey key, MaterialKey materialKey) {
        super(key);

        this.materialKey = materialKey;
    }

    public SingleLinkMaterialized(String name, ModelKey key, MaterialKey materialKey) {
        super(name, key);

        this.materialKey = materialKey;
    }

    public SingleLinkMaterialized(String name, Spatial asset, ModelKey key, MaterialKey materialKey) {
        super(name, asset, key);

        this.materialKey = materialKey;
    }

    public MaterialKey getMaterialKey() {
        return materialKey;
    }

    public void setMaterialKey(MaterialKey materialKey) {
        this.materialKey = materialKey;
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);

        InputCapsule capsule = e.getCapsule(this);

        materialKey = (MaterialKey) capsule.readSavable("materialLoaderKey", null);

        if(assetChild != null){
            assetChild.setMaterial(e.getAssetManager().loadAsset(materialKey));
        }
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);

        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(materialKey, "materialLoaderKey", null);
    }

}
