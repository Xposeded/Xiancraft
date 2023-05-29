package zergnewbee.xiancraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.Vec3d;
import zergnewbee.xiancraft.server.entity.CastingPortalEntity;

import java.awt.*;

import static zergnewbee.xiancraft.server.item.NonBlockRegisterFactory.CASTING_PORTAL_PARTICLE;

@Environment(EnvType.CLIENT)
public class CastingPortalParticle extends AnimatedParticle {

    Entity attachedEntity;
    byte downID;
    byte rightID;

    CastingPortalParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, spriteProvider, 0);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.scale *= 0.2F;
        this.maxAge = 40;
        this.setTargetColor(15916745);
        this.setSpriteForAge(spriteProvider);
        this.setAlpha(0.01F);
    }

    public void init(Entity attachedEntity, byte up, byte right) {
        this.attachedEntity = attachedEntity;
        this.downID = up;
        this.rightID = right;
    }


    @Override
    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            float strength = 0.2f;
            if (attachedEntity instanceof CastingPortalEntity portal) {
                Vec3d vecUp = portal.getVecUp();
                Vec3d vecRight = portal.getVecRight();
                Vec3d vecForward = portal.getVecForward();
                Vec3d syncedPos = portal.getPos().add(vecForward.multiply(0.1)).add(vecUp.multiply(0.2 * (-7.5 + downID))).add(vecRight.multiply(0.2 * (-7.5 + rightID)));
                this.prevPosX = x;
                this.prevPosY = y;
                this.prevPosZ = z;
                setPos(syncedPos.x, syncedPos.y, syncedPos.z);

                int mirrorDownID = downID;
                int mirrorRightID = rightID;

                if (mirrorRightID > 7) mirrorRightID = 15 - mirrorRightID;
                if (mirrorDownID > 7) mirrorDownID = 15 - mirrorDownID;

                int strengthBits = portal.pathBits[mirrorDownID] >>> ((7-mirrorRightID) * 4);
                strengthBits &= 0b1111;
                if (downID == 0 || downID == 15 || rightID == 0 || rightID == 15) strength = 0.8f;
                else {
                    if (strengthBits > 0) {
                        strength = 0.6f;
                        float friction = (float)(strengthBits-1) * 0.05f;

                        setColor(Color.HSBtoRGB(friction,1.0f,1.0f));
                    }
                }
            }
            this.setSpriteForAge(this.spriteProvider);
            float youngAge = (float) maxAge / 5;
            float oldAge = (float) maxAge / 5 * 4;
            if (age > oldAge) {
                setAlpha(strength * (1.0F - ((float) age - oldAge) / maxAge));
            } else if (age < youngAge){
                setAlpha(strength * (float) age / youngAge);
            }
        }

    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
            return new CastingPortalParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
        }

    }

    @Environment(EnvType.CLIENT)
    public static class CastingPortalParticleCenter extends NoRenderParticle {

        private final ParticleManager particleManager;
        private final Entity attachedEntity;

        public CastingPortalParticleCenter(ClientWorld clientWorld, Entity entity, ParticleManager particleManager) {
            super(clientWorld, entity.getX(), entity.getY(), entity.getZ());
            this.particleManager = particleManager;
            this.maxAge = 5;
            this.attachedEntity = entity;
        }

        @Override
        public void tick() {
            for (byte up = 0; up<16; up++)
                for (byte right = 0; right<16; right++) {
                    CastingPortalParticle particle = (CastingPortalParticle) this.particleManager.addParticle(
                            CASTING_PORTAL_PARTICLE, x, y, z, velocityX, velocityY, velocityZ);
                    if (particle != null) {
                        particle.init(attachedEntity,up,right);
                    }
                }
            this.markDead();
        }
    }
}
