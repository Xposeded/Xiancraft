package zergnewbee.xiancraft.client.render.entity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import zergnewbee.xiancraft.client.render.entity.model.CastingBubbleEntityModel;
import zergnewbee.xiancraft.server.block.entity.projectile.CastingBubbleEntity;

public class CastingBubbleEntityRenderer extends EntityRenderer<CastingBubbleEntity> {
    private static final Identifier TEXTURE = new Identifier("textures/entity/shulker/spark.png");
    private static final RenderLayer LAYER;
    private final CastingBubbleEntityModel<CastingBubbleEntity> model;

    public CastingBubbleEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new CastingBubbleEntityModel<>(context.getPart(EntityModelLayers.SHULKER_BULLET));
    }

    protected int getBlockLight(CastingBubbleEntity castingBubbleEntity, BlockPos blockPos) {
        return 15;
    }

    public void render(CastingBubbleEntity castingBubbleEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        float h = MathHelper.lerpAngleDegrees(g, castingBubbleEntity.prevYaw, castingBubbleEntity.getYaw());
        float j = MathHelper.lerp(g, castingBubbleEntity.prevPitch, castingBubbleEntity.getPitch());
        float k = (float)castingBubbleEntity.age + g;
        matrixStack.translate(0.0F, 0.15F, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin(k * 0.1F) * 180.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.cos(k * 0.1F) * 180.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(k * 0.15F) * 360.0F));
        matrixStack.scale(-0.5F, -0.5F, 0.5F);
        this.model.setAngles(castingBubbleEntity, 0.0F, 0.0F, 0.0F, h, j);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.getLayer(TEXTURE));
        this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.scale(1.5F, 1.5F, 1.5F);
        VertexConsumer vertexConsumer2 = vertexConsumerProvider.getBuffer(LAYER);
        this.model.render(matrixStack, vertexConsumer2, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.15F);
        matrixStack.pop();
        super.render(castingBubbleEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    public Identifier getTexture(CastingBubbleEntity shulkerBulletEntity) {
        return TEXTURE;
    }

    static {
        LAYER = RenderLayer.getEntityTranslucent(TEXTURE);
    }
}
