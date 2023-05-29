package zergnewbee.xiancraft.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import zergnewbee.xiancraft.server.entity.CastingPortalEntity;

public class CastingPortalEntityModel<T extends Entity> extends SinglePartEntityModel<CastingPortalEntity> {
    private final ModelPart root;

    private final ModelPart base;

    public CastingPortalEntityModel(ModelPart modelPart) {
        root = modelPart;
        base = root.getChild("main");
    }

    @Override
    public void setAngles(CastingPortalEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.base.yaw = headYaw * 0.017453292F;
        this.base.pitch = headPitch * 0.017453292F;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild(EntityModelPartNames.CUBE, ModelPartBuilder.create().uv(0, 0).cuboid(-6F, 12F, -6F, 12F, 12F, 12F),
                ModelTransform.pivot(0F, 0F, 0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }
}
