package zergnewbee.xiancraft.server.block.entity.projectile;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static zergnewbee.xiancraft.server.MainServerEntry.ModID;
import static zergnewbee.xiancraft.server.item.ItemFactory.CASTING_BUBBLE_ENTITY;

public class CastingBubbleEntity extends ProjectileEntity {

    public static final Identifier TEXTURE = new Identifier(ModID,"textures/entity/projectiles/arrow.png");

    public CastingBubbleEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public CastingBubbleEntity(World world, double x, double y, double z){
        this(CASTING_BUBBLE_ENTITY, world);
        this.setPosition(x, y, z);
    }

    public CastingBubbleEntity(World world, LivingEntity owner){
        this(world, owner.getX(), owner.getEyeY() - 0.10000000149011612, owner.getZ());
        setOwner(owner);

    }

    @Override
    public boolean shouldRender(double distance) {
        double d = this.getBoundingBox().getAverageSideLength() * 4.0;
        if (Double.isNaN(d)) {
            d = 4.0;
        }

        d *= 64.0;
        return distance < d * d;
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    public boolean doesRenderOnFire() {
        return false;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.world.isClient) {
            LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
            if(lightningEntity != null){
                lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(this.getBlockPos()));
                world.spawnEntity(lightningEntity);
            }
            this.kill();
        }
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        boolean isTouchingPortal = false;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos);
            if (blockState.isOf(Blocks.NETHER_PORTAL)) {
                this.setInNetherPortal(blockPos);
                isTouchingPortal = true;
            } else if (blockState.isOf(Blocks.END_GATEWAY)) {
                BlockEntity blockEntity = this.world.getBlockEntity(blockPos);
                if (blockEntity instanceof EndGatewayBlockEntity && EndGatewayBlockEntity.canTeleport(this)) {
                    EndGatewayBlockEntity.tryTeleportingEntity(this.world, blockPos, blockState, this, (EndGatewayBlockEntity)blockEntity);
                }

                isTouchingPortal = true;
            }
        }

        if (hitResult.getType() != HitResult.Type.MISS && !isTouchingPortal) {
            this.onCollision(hitResult);
        }

        this.checkBlockCollision();
        Vec3d vec3d = this.getVelocity();
        double nextX = this.getX() + vec3d.x;
        double nextY = this.getY() + vec3d.y;
        double nextZ = this.getZ() + vec3d.z;
        this.updateRotation();
        float acceleration;
        if (this.isTouchingWater()) {
            for(int i = 0; i < 4; ++i) {
                float fraction = 0.25F;
                this.world.addParticle(ParticleTypes.BUBBLE, nextX - vec3d.x * fraction, nextY - vec3d.y * fraction, nextZ - vec3d.z * fraction, vec3d.x, vec3d.y, vec3d.z);
            }

            acceleration = 0.8F;
        } else {
            acceleration = 0.99F;
        }

        this.setVelocity(vec3d.multiply(acceleration));
        if (!this.hasNoGravity()) {
            Vec3d vec3d2 = this.getVelocity();
            this.setVelocity(vec3d2.x, vec3d2.y - (double)this.getGravity(), vec3d2.z);
        }

        this.setPosition(nextX, nextY, nextZ);
    }


    protected float getGravity() {
        return 0.03F;
    }

}
