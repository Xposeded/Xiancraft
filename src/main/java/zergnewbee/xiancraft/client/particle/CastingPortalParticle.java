package zergnewbee.xiancraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zergnewbee.xiancraft.server.MainServerEntry;
import zergnewbee.xiancraft.server.entity.CastingPortalEntity;

import java.awt.*;
import java.util.ArrayList;

import static zergnewbee.xiancraft.server.item.NonBlockRegisterFactory.CASTING_PORTAL_PARTICLE;

@Environment(EnvType.CLIENT)
public class CastingPortalParticle extends AnimatedParticle {

    Entity attachedEntity;
    byte downID;
    byte rightID;

    float basic_scale;

    int type;
    int castingID;
    Vec3d vecOffset;

    float[][] colorTable =  {
            {0.0F,0.0F,0.0F} ,  //None
            {0.0F,0.0F,1.0F} ,  //All
            {0.1389F,0.6000F,1.0000F} ,  //Metal
            {0.3333F,0.6000F,0.5000F} ,  //Wood
            {0.5555F,0.7000F,1.0000F} ,  //Water
            {0.0000F,1.0F,1.0F} ,  //Fire
            {0.0833F,0.6000F,0.5000F}    //Earth
    };

    CastingPortalParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, spriteProvider, 0);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.scale *= 0.15F;
        this.maxAge = 30;
        this.setTargetColor(15916745);
        this.setSprite(spriteProvider);
        this.setAlpha(1.0F);

        type = 0;
        castingID = 0;
        basic_scale = scale;
        scale = 0.0F;
    }

    public void init(Entity attachedEntity, byte up, byte right, int pos, int type, Vec3d vec) {
        this.attachedEntity = attachedEntity;
        this.downID = up;
        this.rightID = right;
        this.castingID = pos;
        this.type = type;
        this.vecOffset = vec;
    }


    @Override
    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            float current_scale = basic_scale;
            // Casting
            if (type == 0) {
                this.setSprite(spriteProvider.getSprite((downID + rightID)  % 26 ,26));
                if (attachedEntity instanceof CastingPortalEntity portal) {
                    Vec3d vecUp = portal.getVecUp();
                    Vec3d vecRight = portal.getVecRight();
                    Vec3d vecForward = portal.getVecForward();

                    float angle, length;
                    if (downID < 8) {
                        angle = (-180f + 11.25f * rightID) * 0.017453292F;
                        length = 15 - downID;
                    } else {
                        angle = (11.25f * rightID) * 0.017453292F;
                        length = downID;
                    }
                    current_scale = length / 15.0f * current_scale;

                    // Make the outer ring larger
                    if (length == 15) length = 16;

                    float newX = length * MathHelper.cos(angle);
                    float newY = length * MathHelper.sin(angle);


                    Vec3d syncedPos = portal.getPos().add(vecForward.multiply(0.1)).add(vecUp.multiply(0.1 * newY)).add(vecRight.multiply(0.1 * newX));
                    this.prevPosX = x;
                    this.prevPosY = y;
                    this.prevPosZ = z;
                    setPos(syncedPos.x, syncedPos.y, syncedPos.z);

                    int mirrorDownID = downID;
                    int mirrorRightID = rightID;

                    if (mirrorRightID > 7) mirrorRightID = 15 - mirrorRightID;
                    if (mirrorDownID > 7) mirrorDownID = 15 - mirrorDownID;

                    int strengthBits = portal.pathBits[mirrorDownID] >>> ((7 - mirrorRightID) * 4);
                    strengthBits &= 0b1111;
                    if (mirrorDownID == 0) {
                        current_scale *= 1.25F;
                        setAlpha(1.0F);
                        int powerLevel = (int)((portal.getPower() - 50) * 8.0 / 50);
                        if (mirrorRightID < powerLevel) {
                            // Show remaining portal power
                            if (portal.clientCastingPos2 == 1) {
                                // Chaos
                                int colorIndex = random.nextBetween(2, 6);
                                setColor(Color.HSBtoRGB(
                                        colorTable[colorIndex][0],
                                        colorTable[colorIndex][1],
                                        colorTable[colorIndex][2]));
                            } else if (portal.clientCastingPos2 < 7) {
                                setColor(Color.HSBtoRGB(
                                        colorTable[portal.clientCastingPos2][0],
                                        colorTable[portal.clientCastingPos2][1],
                                        colorTable[portal.clientCastingPos2][2]));
                            }
                        }
                    }
                    else if(mirrorDownID == 7){
                        setAlpha(1.0F);
                    }
                    else {
                        if (strengthBits > 0) {
                            setAlpha(1.0F);
                            // Do hue shift as strength growing
                            float friction = (float) (strengthBits - 1) * 0.008f;

                            if (portal.clientCastingPos2 == 0) {
                                // No selection
                                setAlpha(0.0F);
                            } else if (portal.clientCastingPos2 == 1) {
                                // Chaos
                                int colorIndex = random.nextBetween(2,6);
                                setColor(Color.HSBtoRGB(
                                        colorTable[colorIndex][0],
                                        colorTable[colorIndex][1] / strengthBits,
                                        colorTable[colorIndex][2]));
                            }
                            else if (portal.clientCastingPos2 < 7) {
                                setColor(Color.HSBtoRGB(
                                        colorTable[portal.clientCastingPos2][0],
                                        colorTable[portal.clientCastingPos2][1] / strengthBits,
                                        colorTable[portal.clientCastingPos2][2]));
                            } else {
                                // Error
                                setColor(0xFFFFFFFF);
                            }
                        }
                        else {
                            setAlpha(0.0F);
                        }
                    }
                }
            } else if (type == 1) {
                // Selecting slots
                if (attachedEntity instanceof CastingPortalEntity portal) {
                    if (vecOffset != null) {
                        Vec3d syncedPos = portal.getPos().add(vecOffset);
                        this.prevPosX = x;
                        this.prevPosY = y;
                        this.prevPosZ = z;
                        setPos(syncedPos.x, syncedPos.y, syncedPos.z);
                    }
                    if (castingID == portal.clientCastingPos) {
                        // Random spr
                        this.setSprite(this.spriteProvider);
                        current_scale *=  1.25F;
                    }

                    if (portal.clientCastingPos2 == 0) {
                        // No selection
                        setAlpha(0.0F);
                    } else if (portal.clientCastingPos2 == 1) {
                        // Chaos
                        setAlpha(1.0F);
                        int colorIndex = random.nextBetween(2,6);
                        setColor(Color.HSBtoRGB(colorTable[colorIndex][0], colorTable[colorIndex][1], colorTable[colorIndex][2]));
                    }
                    else if (portal.clientCastingPos2 < 7) {
                        setAlpha(1.0F);
                        setColor(Color.HSBtoRGB(
                                colorTable[portal.clientCastingPos2][0],
                                colorTable[portal.clientCastingPos2][1],
                                colorTable[portal.clientCastingPos2][2]));
                    } else {
                        // Error
                        setColor(0xFFFFFFFF);
                    }
                }
            } else if (type == 2) {
                // Selecting elements
                if (attachedEntity instanceof CastingPortalEntity portal) {
                    if (vecOffset != null) {
                        Vec3d syncedPos = portal.getPos().add(vecOffset);
                        this.prevPosX = x;
                        this.prevPosY = y;
                        this.prevPosZ = z;
                        setPos(syncedPos.x, syncedPos.y, syncedPos.z);
                    }
                    if (castingID == portal.clientCastingPos2) {
                        // Random spr
                        this.setSprite(this.spriteProvider);

                        if (portal.clientCastingPos2 == 0) {
                            // No selection
                            setColor(0xFFFFFFFF);
                        } else if (portal.clientCastingPos2 == 1) {
                            // Chaos
                            int colorIndex = random.nextBetween(2,6);
                            setColor(Color.HSBtoRGB(colorTable[colorIndex][0], colorTable[colorIndex][1], colorTable[colorIndex][2]));
                        }
                        else if (portal.clientCastingPos2 < 7) {
                            setColor(Color.HSBtoRGB(
                                    colorTable[portal.clientCastingPos2][0],
                                    colorTable[portal.clientCastingPos2][1],
                                    colorTable[portal.clientCastingPos2][2]));
                        } else {
                            // Error
                            setColor(0xFFFFFFFF);
                        }
                    }
                    else {
                        current_scale *= 0.75F;
                        setColor(0xFFFFFFFF);
                    }
                }
            }
            float youngAge = (float) maxAge / 5;
            float oldAge = (float) maxAge / 5 * 4;
            if (age > oldAge) {
                scale = basic_scale * (1.0F - ((float) age - oldAge) / (maxAge - oldAge));
            } else if (age < youngAge) {
                scale = basic_scale * ((float) age / youngAge);
            }
            else {
                scale = current_scale;
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
            if (attachedEntity instanceof CastingPortalEntity portal) {
                if(portal.getStatusFromDataChecker() == CastingPortalEntity.PortalStatus.FINISHED) {
                    for (byte up = 0; up < 16; up++)
                        for (byte right = 0; right < 16; right++) {
                            CastingPortalParticle particle = (CastingPortalParticle) this.particleManager.addParticle(
                                    CASTING_PORTAL_PARTICLE, x, y, z, velocityX, velocityY, velocityZ);
                            if (particle != null) {
                                particle.init(attachedEntity, up, right,0, 0, null);
                            }
                        }
                } else if (portal.hasInitDirection() && portal.getStatusFromDataChecker().ordinal()
                        <= CastingPortalEntity.PortalStatus.CONVERTING.ordinal())  {
                    Vec3d vecUp = portal.getVecUp();
                    Vec3d vecRight = portal.getVecRight();

                    Vec3d startPoint = Vec3d.ZERO;
                    ArrayList<Vec3d> list = new ArrayList<>(8);

                    list.add(startPoint.add(vecUp));
                    list.add(startPoint.add(vecUp.multiply(0.7071)).add(vecRight.multiply(0.7071)));
                    list.add(startPoint.add(vecRight));
                    list.add(startPoint.subtract(vecUp.multiply(0.7071)).add(vecRight.multiply(0.7071)));
                    list.add(startPoint.subtract(vecUp));
                    list.add(startPoint.subtract(vecUp.multiply(0.7071)).subtract(vecRight.multiply(0.7071)));
                    list.add(startPoint.subtract(vecRight));
                    list.add(startPoint.add(vecUp.multiply(0.7071)).subtract(vecRight.multiply(0.7071)));

                    for ( int pos = 0; pos<list.size(); pos++) {
                        Vec3d vec = list.get(pos);
                        CastingPortalParticle particle = (CastingPortalParticle) this.particleManager.addParticle(
                                CASTING_PORTAL_PARTICLE, x+vec.x, y+vec.y, z+vec.z, velocityX, velocityY, velocityZ);
                        if (particle != null) {
                            particle.init(attachedEntity, (byte) 0, (byte) 0, pos+1, 1, vec);
                        }
                    }

                    ArrayList<Vec3d> list2 = new ArrayList<>(6);

                    list2.add(startPoint.add(vecUp));
                    list2.add(startPoint.add(vecUp.multiply(0.5)).add(vecRight.multiply(0.866)));
                    list2.add(startPoint.subtract(vecUp.multiply(0.5)).add(vecRight.multiply(0.866)));
                    list2.add(startPoint.subtract(vecUp));
                    list2.add(startPoint.subtract(vecUp.multiply(0.5)).subtract(vecRight.multiply(0.866)));
                    list2.add(startPoint.add(vecUp.multiply(0.5)).subtract(vecRight.multiply(0.866)));

                    for ( int pos = 0; pos<list2.size(); pos++) {
                        Vec3d vec = list2.get(pos);
                        CastingPortalParticle particle = (CastingPortalParticle) this.particleManager.addParticle(
                                CASTING_PORTAL_PARTICLE, x+0.5*vec.x, y+0.5*vec.y, z+0.5*vec.z, velocityX, velocityY, velocityZ);
                        if (particle != null) {
                            particle.init(attachedEntity, (byte) 0, (byte) 0, pos+1, 2, vec.multiply(0.5));
                        }
                    }
                }
            }
            this.markDead();
        }
    }
}
