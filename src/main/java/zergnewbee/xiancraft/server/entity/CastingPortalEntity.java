package zergnewbee.xiancraft.server.entity;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import zergnewbee.xiancraft.server.MainServerEntry;
import zergnewbee.xiancraft.server.entity.projectile.CastingBubbleEntity;

import java.util.UUID;

import static zergnewbee.xiancraft.server.item.NonBlockRegisterFactory.CASTING_PORTAL_ENTITY;

public class CastingPortalEntity extends Entity implements Ownable {

    private PortalStatus status;
    private boolean hasInitDirection = false;
    private Vec3d vecUp;
    private Vec3d vecForward;
    private Vec3d vecRight;
    private Vec3i spawnPos;
    private Vec3i corePos;

    public boolean hasRendered;
    public int clientCastingPos;
    public int clientCastingPos2;

    public int[] pathBits = {0,0,0,0,0,0,0,0};

    public float angleLimit = 45.0f;
    public final byte updateRenderTicks = 20; // Require: updateRenderTicks < 64

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private Entity owner;


    private static final TrackedData<Byte> POWER;

    private static final TrackedData<Byte> STATUS_BYTE;

    // No need to be stored into NBT
    private static final TrackedData<Byte> INTERNAL_TICKS;

    public CastingPortalEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public CastingPortalEntity(World world, double x, double y, double z){
        this(CASTING_PORTAL_ENTITY, world);
        this.setPosition(x, y, z);

        setPower((byte) 100);
        vecUp = getRotationVector(getPitch() + 90, getYaw()).multiply(0.25);
        setStatusByte(PortalStatus.SELECTING_NONE);
    }

    public CastingPortalEntity(World world, LivingEntity owner){
        this(world, owner.getX(), owner.getEyeY() - 0.1, owner.getZ());

        setOwner(owner);
    }

    public CastingPortalEntity(World world, Vec3d pos, LivingEntity owner){
        this(world, pos.x, pos.y, pos.z);

        setOwner(owner);
        setPitch(owner.getPitch());
        setYaw(owner.getYaw());
    }

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
        dataTracker.startTracking(POWER, (byte)0);
        dataTracker.startTracking(STATUS_BYTE, (byte)0);
        this.dataTracker.startTracking(INTERNAL_TICKS, (byte)0);
    }



    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setPower(nbt.getByte("Power"));

        if (nbt.containsUuid("Owner")) {
            this.ownerUuid = nbt.getUuid("Owner");
        }

        status = convertToStatus(nbt.getByte("PStatus")) ;
        setStatusByte(status);

        int[] intArray = nbt.getIntArray("CorePos" );
        if (intArray.length > 2) corePos = new Vec3i(intArray[0], intArray[1], intArray[2]);

        int[] summonPos = nbt.getIntArray("BSpawnPos" );
        if (summonPos.length > 2) spawnPos = new Vec3i(summonPos[0], summonPos[1], summonPos[2]);

        pathBits = nbt.getIntArray("CorePos" );
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("Owner", this.ownerUuid);
        }
        nbt.putByte("Power", getPower());
        nbt.putByte("PStatus", (byte) status.ordinal());
        if (corePos != null) {
            nbt.putIntArray("CorePos", new int[]{corePos.getX(), corePos.getY(), corePos.getZ()});
        }
        if (spawnPos != null) {
            nbt.putIntArray("BSpawnPos", new int[]{spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()});
        }

        if (pathBits.length > 7) {
            nbt.putIntArray("PathBits", pathBits);
        }
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        // NO UPDATE!
    }


    @Override
    public void tick() {
        super.tick();
        initDirections();

        if(getPower() < 3) {
            onPortalShuttingDown();
        }

        // Particle render tick
        if (!world.isClient) {
            byte internalTicks = (byte) (getInternalTicks() + 1);
            if (internalTicks > updateRenderTicks * 2) {
                internalTicks = 0;
            }
            setInternalTicks(internalTicks);
        }

        if (world.isClient) {
            world.addParticle(ParticleTypes.PORTAL, getPos().x, getPos().y - 0.15, getPos().z,
                    1.2 * (Math.random() - 0.5), 2.4 * (Math.random() - 0.5), 1.2 * (Math.random() - 0.5));
        }

        status = getStatusFromDataChecker();
        switch (status) {
            case FINISHED -> casting();
            case CONVERTING -> converting();
            default -> selecting();
        }
        setStatusByte(status);

    }

    public void selecting() {
        if (owner != null) {
            Vec3d startPoint = owner.getEyePos();
            Vec3d endPoint = startPoint.add(vecForward.multiply(4));
            setPosition(endPoint);
            // Update particle effect in client side
            if(world.isClient) {
                getCastingPos(owner, getPitch(), getYaw(), this);
            }
        }

        byte power = getPower();

        if (!world.isClient) {
            byte internalTicks = (byte) (getInternalTicks() + 1);
            if (internalTicks > updateRenderTicks + updateRenderTicks / 4) {
                power--;
                setPower(power);
            }
        }

        if (power < 1) {
            playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            kill();
        }
    }

    public void converting() {
        byte power = getPower();
        if (power < 1 ) {
            playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            kill();
            return;
        }

        syncPositionsWithPlayer();

        power++;
        setPower(power);

        if (power > 99) {
            status = PortalStatus.FINISHED;

            if (owner != null) {
                Vec3d spawnPos = Vec3d.ofCenter(this.spawnPos);
                CastingBubbleEntity entity = new CastingBubbleEntity(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), this);
                world.spawnEntity(entity);
            }


        }
    }

    public void casting() {
        if (getPower() < 50) {
            kill();
        }

        syncPositionsWithPlayer();

        if (world.isClient) {
            if (owner != null) {
                // Show hint for owner only
                if (MinecraftClient.getInstance().player == owner) {

                    Vec3d velocity = vecForward.multiply(12).add(vecUp.multiply(0.1));
                    world.addParticle(ParticleTypes.END_ROD,
                            getPos().x - vecUp.x, getPos().y - vecUp.y, getPos().z - vecUp.z,
                            velocity.x, velocity.y, velocity.z);
                }
            }
        }

    }

    private void syncPositionsWithPlayer(){
        if(world.isClient) {
            if (owner != null) {

                float newYaw = MathHelper.clampAngle(owner.getYaw(), getYaw(), angleLimit);
                float newPitch = MathHelper.clampAngle(owner.getPitch(), getPitch(), angleLimit);
                vecForward = getRotationVector(newPitch, newYaw).multiply(0.25);
                vecRight = getRotationVector(0, newYaw + 90).multiply(0.25);
                vecUp = getRotationVector(newPitch - 90, newYaw).multiply(0.25);

                Vec3d startPoint = owner.getEyePos();
                Vec3d endPoint = startPoint.add(vecForward.multiply(4));
                setPosition(endPoint);
            }
        }
    }

    public void onPortalShuttingDown() {

        for (int i = 0; i < 3; i++) {
            world.addParticle(ParticleTypes.GLOW, getPos().x, getPos().y - 0.15, getPos().z, 1.2 * (Math.random() - 0.5), 2.4 * (Math.random() - 0.5), 1.2 * (Math.random() - 0.5));
        }

    }

    public void initDirections() {
        if (!hasInitDirection) {
            vecForward = getRotationVector(getPitch(), getYaw()).multiply(0.25);
            vecRight = getRotationVector(0, getYaw() + 90).multiply(0.25);
            vecUp = getRotationVector(getPitch() - 90, getYaw()).multiply(0.25);


            hasInitDirection = true;
        }
    }

    public void onConfirmCasting(PlayerEntity player, int castingPos, Vec3i corePosInput) {
        if (owner != null && owner == player && getStatusFromDataChecker().ordinal() <= PortalStatus.SELECTING_EARTH.ordinal()) {
            corePos = corePosInput;
            BlockState blockState = world.getBlockState(new BlockPos(corePos));

            Direction right = blockState.get(Properties.HORIZONTAL_FACING).rotateYClockwise();
            Direction down = right.rotateYClockwise();
            switch (castingPos) {
                case 2 -> spawnPos = corePos.offset(right,15).offset(down);
                case 3 -> spawnPos = corePos.offset(right,15).offset(down,14);
                case 4 -> spawnPos = corePos.offset(down,15).offset(right,14);
                case 5 -> spawnPos = corePos.offset(down,15).offset(right,1);
                case 6 -> spawnPos = corePos.offset(down,14);
                case 7 -> spawnPos = corePos.offset(down);
                case 8 -> spawnPos = corePos.offset(right);
                default -> spawnPos = corePos.offset(right,14);
            }

            setStatusByte(PortalStatus.CONVERTING);
            setPower((byte) 80);
        }
    }

    public static int getCastingPos(Entity owner, float initPitch, float initYaw, CastingPortalEntity portal) {
        float curYaw = MathHelper.wrapDegrees(owner.getYaw());

        float deltaYaw = MathHelper.subtractAngles(initYaw, curYaw);
        float deltaPitch = MathHelper.subtractAngles(initPitch, MathHelper.wrapDegrees(owner.getPitch()));

        // Not recognizable movement (deltaAngle < 3°)
        if (Math.abs(deltaPitch) < 3 && Math.abs(deltaYaw) < 3) {
            if (portal != null ) {
                portal.status = PortalStatus.SELECTING_NONE;
                portal.clientCastingPos2 = 0;
            }
            return 0;
        }
        float upLength = MathHelper.sin(deltaPitch * 0.017453292F) / MathHelper.cos(deltaPitch * 0.017453292F);
        float rightLength = MathHelper.sin(deltaYaw * 0.017453292F) / MathHelper.cos(deltaYaw * 0.017453292F);

        double rotationDegrees = Math.toDegrees(Math.atan2(upLength, rightLength));
        if (portal != null ) {
            // Select a type when deltaAngle is ranged from 3° to 10°
            if (Math.abs(deltaPitch) < 10 && Math.abs(deltaYaw) < 10) {
                int ID2 = (((int) (rotationDegrees + 120.0 + 360) % 360) / 60) % 6 + 1;
                portal.status = portal.convertToStatus((byte) ID2);
                portal.clientCastingPos2 = ID2;


            }
        }

        int ID = (((int) (rotationDegrees + 112.5 + 360) % 360) / 45) % 8 + 1;

        if (portal != null )
            portal.sendHint((PlayerEntity) portal.owner, "rotationDegrees[ "
                + (rotationDegrees + 112.5 + 360) % 360
                + " ]");

        if (portal != null) portal.clientCastingPos = ID;
        return ID;
    }

    public void updatePathBits(int x, int z) {
        if (corePos != null && pathBits.length > 7) {
            BlockState blockState = world.getBlockState(new BlockPos(corePos));

            Direction right = blockState.get(Properties.HORIZONTAL_FACING).rotateYClockwise();
            Direction down = right.rotateYClockwise();

            Vec3i vecRightDir = right.getVector();
            Vec3i vecDownDir = down.getVector();

            int deltaX = x - corePos.getX();
            int deltaZ = z - corePos.getZ();

            int rightDistance = vecRightDir.getX() * deltaX + vecRightDir.getZ() * deltaZ;
            int downDistance = vecDownDir.getX() * deltaX + vecDownDir.getZ() * deltaZ;

            if (rightDistance > -1 && rightDistance < 16 && downDistance > -1 && downDistance < 16) {
                if (rightDistance > 7) rightDistance = 15 - rightDistance;
                if (downDistance > 7) downDistance = 15 - downDistance;

                int strengthBits = pathBits[downDistance] >>> ((7-rightDistance) * 4);
                strengthBits &= 0b1111;

                strengthBits = strengthBits > 14 ? 15 : strengthBits + 1;

                pathBits[downDistance] &= ~(0b1111 << ((7-rightDistance) * 4));
                pathBits[downDistance] |= (strengthBits << ((7-rightDistance) * 4));

            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        Entity entity = this.getOwner();
        return new EntitySpawnS2CPacket(this, entity == null ? 0 : entity.getId());
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        Entity entity = this.world.getEntityById(packet.getEntityData());
        if (entity != null) {
            setOwner(entity);
        }
    }

    public byte getPower() {
        return this.dataTracker.get(POWER);
    }

    public void setPower(byte power) {
        this.dataTracker.set(POWER, power);
    }

    public PortalStatus getStatusFromDataChecker() {
        return convertToStatus(dataTracker.get(STATUS_BYTE));
    }

    private void setStatusByte(PortalStatus status) {
        dataTracker.set(STATUS_BYTE, (byte)status.ordinal());
        this.status = status;
    }

    public Vec3d getVecUp() {
        return vecUp;
    }

    public Vec3d getVecRight() {
        return vecRight;
    }

    public Vec3d getVecForward() {
        return vecForward;
    }

    public byte getInternalTicks() {
        return this.dataTracker.get(INTERNAL_TICKS);
    }

    private void setInternalTicks(byte ticks) {
        this.dataTracker.set(INTERNAL_TICKS, ticks);
    }


    public boolean hasInitDirection() {
        return hasInitDirection;
    }

    @Override
    public boolean doesRenderOnFire() {
        return false;
    }
    public void setOwner(@Nullable Entity entity) {
        if (entity != null) {
            this.ownerUuid = entity.getUuid();
            this.owner = entity;
        }

    }

    @Nullable
    public Entity getOwner() {
        if (this.owner != null && !this.owner.isRemoved()) {
            return this.owner;
        } else if (this.ownerUuid != null && this.world instanceof ServerWorld) {
            this.owner = ((ServerWorld)this.world).getEntity(this.ownerUuid);
            return this.owner;
        } else {
            return null;
        }
    }

    protected boolean isOwner(Entity entity) {
        return entity.getUuid().equals(this.ownerUuid);
    }

    private PortalStatus convertToStatus(byte status){
        return switch (status) {
            case 1 -> PortalStatus.SELECTING_ALL;
            case 2 -> PortalStatus.SELECTING_METAL;
            case 3 -> PortalStatus.SELECTING_WOOD;
            case 4 -> PortalStatus.SELECTING_WATER;
            case 5 -> PortalStatus.SELECTING_FIRE;
            case 6 -> PortalStatus.SELECTING_EARTH;
            case 7 -> PortalStatus.CONVERTING;
            case 8 -> PortalStatus.FINISHED;
            default -> PortalStatus.SELECTING_NONE;
        };
    }

    static {
        POWER = DataTracker.registerData(CastingPortalEntity.class, TrackedDataHandlerRegistry.BYTE);
        STATUS_BYTE = DataTracker.registerData(CastingPortalEntity.class, TrackedDataHandlerRegistry.BYTE);
        INTERNAL_TICKS = DataTracker.registerData(CastingPortalEntity.class, TrackedDataHandlerRegistry.BYTE);
    }


    public enum PortalStatus{
        SELECTING_NONE,
        SELECTING_ALL,
        SELECTING_METAL,
        SELECTING_WOOD,
        SELECTING_WATER,
        SELECTING_FIRE,
        SELECTING_EARTH,
        CONVERTING,
        FINISHED
    }

    private void sendHint(PlayerEntity playerEntity, String str) {
        if (playerEntity instanceof ClientPlayerEntity clientPlayerEntity) {
            Text text;
            text = Text.of(str);
            clientPlayerEntity.sendMessage(text, true);
        }
    }
}
