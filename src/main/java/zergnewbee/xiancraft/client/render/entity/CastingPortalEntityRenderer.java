package zergnewbee.xiancraft.client.render.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import zergnewbee.xiancraft.client.particle.CastingPortalParticle;
import zergnewbee.xiancraft.client.render.entity.model.CastingPortalEntityModel;
import zergnewbee.xiancraft.server.entity.CastingPortalEntity;

import java.util.ArrayList;

public class CastingPortalEntityRenderer extends EntityRenderer<CastingPortalEntity> {

    private static final Identifier TEXTURE = new Identifier("textures/entity/shulker/spark.png");
    private static final RenderLayer LAYER;
    private final CastingPortalEntityModel<CastingPortalEntity> model;

    public CastingPortalEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new CastingPortalEntityModel<>(context.getPart(EntityModelLayers.SHULKER_BULLET));
    }


    protected int getBlockLight(CastingPortalEntity castingPortalEntity, BlockPos blockPos) {
        return 12;
    }


    public void render(CastingPortalEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();

        float deltaTime = (float)entity.age + g;

        // Render panel
        Entity ownerEntity =  entity.getOwner();
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.getLayer(TEXTURE));
        VertexConsumer vertexConsumer2 = vertexConsumerProvider.getBuffer(LAYER);
        CastingPortalEntity.PortalStatus status = entity.getStatusFromDataChecker();
        if (entity.hasInitDirection() && ownerEntity != null && status == CastingPortalEntity.PortalStatus.SELECTING) {
            matrixStack.push();

            matrixStack.scale(0.1F, 0.1F, 0.1F);

            int castingPos = CastingPortalEntity.getCastingPos(ownerEntity, entity.getPitch(), entity.getYaw());

            Vec3d vecUp = entity.getVecUp();
            Vec3d vecRight = entity.getVecRight();


            Vec3d startPoint = Vec3d.ZERO;
            ArrayList<Vec3d> list = new ArrayList<>(8);

            list.add(startPoint.add(vecUp).add(vecRight.multiply(0.4142)));
            list.add(startPoint.add(vecRight).add(vecUp.multiply(0.4142)));
            list.add(startPoint.add(vecRight).subtract(vecUp.multiply(0.4142)));
            list.add(startPoint.subtract(vecUp).add(vecRight.multiply(0.4142)));
            list.add(startPoint.subtract(vecUp).subtract(vecRight.multiply(0.4142)));
            list.add(startPoint.subtract(vecRight).subtract(vecUp.multiply(0.4142)));
            list.add(startPoint.subtract(vecRight).add(vecUp.multiply(0.4142)));
            list.add(startPoint.add(vecUp).subtract(vecRight.multiply(0.4142)));


            for ( int pos = 0; pos<list.size(); pos++) {
                Vec3d vec = list.get(pos);
                matrixStack.push();
                matrixStack.translate(8*vec.x, 8*vec.y, 8*vec.z);
                // Highlight the selected
                this.model.setAngles(entity, 0.0F, 0.0F, 0.0F, MathHelper.sin(deltaTime * 0.1F) * 180.0F,
                        MathHelper.sin(deltaTime * 0.15F) * 360.0F);
                if ( pos + 1 == castingPos ) {
                    this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                else {
                    this.model.render(matrixStack, vertexConsumer2, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.15F);
                }
                matrixStack.pop();
            }
            // Restore angles
            this.model.setAngles(entity, 0.0F, 0.0F, 0.0F, 0, 0);

            matrixStack.pop();
        }



        // Render portal
        else {
            if (entity.hasInitDirection()) {
                float newYaw;
                float newPitch;
                float scale;

                if (ownerEntity != null) {
                    newYaw = MathHelper.clampAngle(ownerEntity.getYaw(), entity.getYaw(),entity.angleLimit);
                    newPitch = MathHelper.clampAngle(ownerEntity.getPitch(), entity.getPitch(), entity.angleLimit);
                } else {
                    newYaw = MathHelper.lerpAngleDegrees(g, entity.prevYaw, entity.getYaw());
                    newPitch = MathHelper.lerp(g, entity.prevPitch, entity.getPitch());
                }

                if (status == CastingPortalEntity.PortalStatus.CONVERTING) {
                    scale = MathHelper.clamp((entity.getPower() - 80.0f) / 20.0f, 0.0f, 1.0f);
                } else {
                    scale = 1.0f;
                }

                Vec3d vecUp = entity.getVecUp();
                if (vecUp != null) matrixStack.translate(-0.6 * vecUp.x, -0.6 * vecUp.y, -0.6 * vecUp.z);

                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-newYaw));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(newPitch + 90.0f));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(deltaTime * Math.min(deltaTime, 100) * 0.1F));

                matrixStack.scale(0.25F * scale, 0.05F * scale, 0.25F * scale);
                this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.15F);

                matrixStack.scale(1.5F, 1.5F, 1.5F);

                this.model.render(matrixStack, vertexConsumer2, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.15F);

                if (status == CastingPortalEntity.PortalStatus.FINISHED) renderCircuit(entity);
            }
        }
        matrixStack.pop();
        super.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    private void renderCircuit(CastingPortalEntity entity) {
        if( entity.world instanceof  ClientWorld world) {
            boolean shouldRender = false;
            if (entity.getInternalTicks() < entity.updateRenderTicks) {
                if(!entity.hasRendered) {
                    entity.hasRendered = true;
                    shouldRender = true;
                }
            } else {
                if(entity.hasRendered) {
                    entity.hasRendered = false;
                    shouldRender = true;
                }
            }
            if (shouldRender) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.particleManager.addParticle(
                        new CastingPortalParticle.CastingPortalParticleCenter(world, entity, client.particleManager));
            }
        }
    }


    @Override
    public Identifier getTexture(CastingPortalEntity entity ) {
        return TEXTURE;
    }

    static {
        LAYER = RenderLayer.getEntityTranslucent(TEXTURE);
    }
}
