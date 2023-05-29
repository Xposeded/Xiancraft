package zergnewbee.xiancraft.server.entity.projectile;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import zergnewbee.xiancraft.server.entity.CastingPortalEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static zergnewbee.xiancraft.server.item.NonBlockRegisterFactory.CASTING_BUBBLE_ENTITY;

public class CastingBubbleEntity extends ProjectileEntity {

    private int abilitiesFlag;
    private BubbleStatus status;

    private static final TrackedData<Byte> POWER;

    private static final TrackedData<Byte> AVAILABLE_POWER;

    private static final TrackedData<Byte> INTERNAL_TICKS;
    private static final TrackedData<Byte> STATUS_BYTE;


    private final List<BubbleStatus> directions = new ArrayList<>(4);

    public CastingBubbleEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);

        abilitiesFlag = 0;
        status = BubbleStatus.CASTING_UP;
        // The priority is set randomly once the bubble was created
        initRandomDirections();

        setPower((byte )48);
        setAvailablePower((byte) 0);
        setInternalTicks((byte) 0);
        setStatusByte(status);
    }

    public CastingBubbleEntity(World world, double x, double y, double z, Entity owner){
        this(CASTING_BUBBLE_ENTITY, world);
        this.setPosition(x, y, z);
        setOwner(owner);
    }

    public CastingBubbleEntity(World world, LivingEntity owner){
        this(world, owner.getX(), owner.getEyeY() - 0.10000000149011612, owner.getZ(), owner);

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
        this.dataTracker.startTracking(POWER, (byte)0);
        this.dataTracker.startTracking(AVAILABLE_POWER, (byte)0);
        this.dataTracker.startTracking(INTERNAL_TICKS, (byte)0);
        this.dataTracker.startTracking(STATUS_BYTE, (byte)0);
    }

    public byte getPower() {
        return this.dataTracker.get(POWER);
    }

    private void setPower(byte power) {
        this.dataTracker.set(POWER, power);
    }

    public byte getAvailablePower() {
        return this.dataTracker.get(AVAILABLE_POWER);
    }

    private void setAvailablePower(byte power) {
        this.dataTracker.set(AVAILABLE_POWER, power);
    }

    private byte getInternalTicks() {
        return this.dataTracker.get(INTERNAL_TICKS);
    }

    private void setInternalTicks(byte ticks) {
        this.dataTracker.set(INTERNAL_TICKS, ticks);
    }

    private BubbleStatus getStatusFromDataChecker() {
        return convertToStatus(dataTracker.get(STATUS_BYTE));
    }

    private void setStatusByte(BubbleStatus status) {
        this.dataTracker.set(STATUS_BYTE, (byte)status.ordinal());
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        setPower(nbt.getByte("Power"));
        setAvailablePower(nbt.getByte("APower"));
        setInternalTicks(nbt.getByte("ITicks"));
        status = convertToStatus(nbt.getByte("BStatus"));
        setStatusByte(status);
        abilitiesFlag = nbt.getInt("BAbilities");

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        nbt.putByte("Power", getPower());
        nbt.putByte("APower", getAvailablePower());
        nbt.putByte("ITicks", getInternalTicks());
        nbt.putByte("BStatus", (byte) status.ordinal());
        nbt.putInt("BAbilities", abilitiesFlag);
    }


    @Override
    public boolean doesRenderOnFire() {
        return false;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (status == BubbleStatus.RELEASED) {
            LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
            if (lightningEntity != null){
                lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(this.getBlockPos()));
                world.spawnEntity(lightningEntity);
            }
            this.kill();
        } else if (status == BubbleStatus.PREPARING) {

            status = BubbleStatus.CASTING_UP;
            setVelocity(Vec3d.ZERO);
        }
    }


    @Override
    public void tick() {
        super.tick();

        if (world.isClient) {
            Vec3d velocity = this.getVelocity();
            this.world.addParticle(ParticleTypes.END_ROD, this.getX() - velocity.x, this.getY() - velocity.y + 0.15, this.getZ() - velocity.z,
                    0.2*(Math.random()-0.5), 0.4*(Math.random()-0.5), 0.2*(Math.random()-0.5));
        }

        status = getStatusFromDataChecker();
        switch (status) {
            case PREPARING -> normalMovement();
            case RELEASED -> releasedMovement();
            case CASTING_UP -> castingUpMovement();
            default -> castingHorizontalMovement();
        }
        setStatusByte(status);
    }

    public boolean waitTicks(){
        byte internalTicks = getInternalTicks();
        if(internalTicks > 0){
            internalTicks--;
            setInternalTicks(internalTicks);
            return true;
        }

        setInternalTicks((byte) 1);
        return false;
    }

    public void releasedMovement(){
        normalMovement();
    }

    public void castingUpMovement(){

        if(waitTicks()) return;

        // Check whether the bubble can move upward
        BlockPos curPos = BlockPos.ofFloored(getPos().x,getPos().y,getPos().z);
        BlockPos nextPos = curPos.up();
        BlockState blockState = this.world.getBlockState(nextPos);
        boolean hasHitTarget = true;
        if (blockState.isOf(Blocks.AIR)) {
            // Move upward
            if(!world.isClient) setPosition(Vec3d.ofCenter(nextPos));
            movingEmitSound(blockState);

            // Will die if without enough power
            if(!convertPower((byte) 16)){
                // No power, entity should be removed
                return;
            }
            // Nothing hit, find a horizontal way before floating up again
            hasHitTarget = false;
        }

        // Blocked by a block from upward
        // Try to interact with the block that was hit
        if(hasHitTarget) xianReaction(blockState);

        boolean nowhereToGo = true;
        for (int i = 0; i < 4 ; i++) {
            // Fake random
            BubbleStatus dir = directions.get(i);
            BlockPos nextPos2;
            if (hasHitTarget) {
                // Failed to move up
                nextPos2 = curPos.offset(Direction.fromHorizontal(horizontalID(dir)));
            }
            else{
                // Moved up Successfully
                nextPos2 = nextPos.offset(Direction.fromHorizontal(horizontalID(dir)));
            }

            // Check whether the bubble can move forward
            if (this.world.getBlockState(nextPos2).isOf(Blocks.AIR)) {

                // Move forward in next attempt
                status = dir;
                nowhereToGo = false;
                break;
            }
        }

        // Nowhere to go! An accident happened!
        if(nowhereToGo) accident(1);
    }


    public void castingHorizontalMovement(){

        if(waitTicks()) return;


        BlockPos curPos = BlockPos.ofFloored(getPos().x,getPos().y,getPos().z);
        BlockPos nextPos;
        nextPos = curPos.offset(Direction.fromHorizontal(horizontalID(status)));

        BlockState blockState = this.world.getBlockState(nextPos);

        // Record Path
        if (getOwner() instanceof CastingPortalEntity portal){
            portal.updatePathBits(curPos.getX(), curPos.getZ());
        }

        // Check whether the bubble can move forward
        if (blockState.isOf(Blocks.AIR)) {
            // Move forward
            if(!world.isClient) setPosition(Vec3d.ofCenter(nextPos));
            movingEmitSound(blockState);
            // No more power
            if(!convertPower((byte) 1)){
                status = BubbleStatus.RELEASED;
            }

            // Finish moving
            return;
        }

        // Blocked by a block
        // Try to interact with the block that was hit
        xianReaction(blockState);

        // Blocked, find another way
        for (int i = 0; i < 4 ; i++) {
            // Choose a random direction
            BubbleStatus dir = directions.get(i);

            // Don't move back
            if (status != BubbleStatus.CASTING_UP) {
                int oppositeID = Direction.fromHorizontal(horizontalID(status)).getOpposite().getHorizontal();
                if (horizontalID(dir) == oppositeID) {
                    continue;
                }
            }

            nextPos = curPos.offset(Direction.fromHorizontal(horizontalID(dir)));

            // Check whether the bubble can move forward
            if (this.world.getBlockState(nextPos).isOf(Blocks.AIR)) {
                // Move forward
                if(!world.isClient) setPosition(Vec3d.ofCenter(nextPos));
                if(convertPower((byte) 1)){
                    status = dir;
                }
                // Finish moving
                return;
            }
        }

//        MainServerEntry.LOGGER.info("Blocked in horizontal directions, try to move up next");
        // Blocked in horizontal directions, try to move up next
        BlockPos nextPos3 = BlockPos.ofFloored(getPos().x,getPos().y,getPos().z).add(Direction.UP.getVector());
        BlockState blockState3 = this.world.getBlockState(nextPos3);
        if (blockState3.isOf(Blocks.AIR)) {
            // Move upward in next attempt
            status = BubbleStatus.CASTING_UP;
            return;
        }

        // Nowhere to go! An accident happened!
        accident(1);
    }

    public void normalMovement(){
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

    public boolean convertPower(byte amount){
        byte power = getPower();
        byte availablePower = getAvailablePower();
        if(power + availablePower < amount) {
            accident(0);
            return false;
        }

        if(getPower()  >= amount) {
            power -= amount;
            availablePower += amount;
        }
        else{
            availablePower += power ;
            availablePower -= amount - power ;
            power = 0;
        }
        setPower(power);
        setAvailablePower(availablePower);

        return true;
    }

    public void accident( int level ){
        LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
        if(lightningEntity != null){
            lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(this.getBlockPos()));
            world.spawnEntity(lightningEntity);
        }
        Entity owner = getOwner();
        if (owner instanceof CastingPortalEntity portal) {
            portal.setPower((byte) 0);
        }
        this.kill();
    }

    public void xianReaction(BlockState state){
        float fraction = 0.5f * (float)getPower() / 48.0f;
        playSound(SoundEvents.BLOCK_WOOD_BREAK, 1.0f, 1.0f + fraction );
    }

    public void movingEmitSound(BlockState state){
        playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, 1.0f, 1.0f );
    }

    public void initRandomDirections(){
        /*
         Would be better using "Collections.shuffle",
         but this may cause Server-Client syncing problems
         which means they produce different random orders.
         */
        directions.add(BubbleStatus.CASTING_SOUTH);
        directions.add(BubbleStatus.CASTING_WEST);
        directions.add(BubbleStatus.CASTING_NORTH);
        directions.add(BubbleStatus.CASTING_EAST);
        Collections.shuffle(directions);

    }

    public void addAbility(BubbleAbilities abilities){
        abilitiesFlag &= (1 << abilities.ordinal());
    }

    public boolean hasAbility(BubbleAbilities abilities){
        return (abilitiesFlag & (1 << abilities.ordinal())) != 0;
    }

    private BubbleStatus convertToStatus(byte status){
        return switch (status) {
            case 1 -> BubbleStatus.CASTING_UP;
            case 2 -> BubbleStatus.CASTING_SOUTH;
            case 3 -> BubbleStatus.CASTING_WEST;
            case 4 -> BubbleStatus.CASTING_NORTH;
            case 5 -> BubbleStatus.CASTING_EAST;
            case 6 -> BubbleStatus.RELEASED;
            default -> BubbleStatus.PREPARING;
        };
    }

    public int horizontalID(BubbleStatus status){
        return  status.ordinal() - 2;
    }

    enum BubbleStatus{
        PREPARING,
        CASTING_UP,
        CASTING_SOUTH,
        CASTING_WEST,
        CASTING_NORTH,
        CASTING_EAST,
        RELEASED
    }

    enum BubbleAbilities{
        MOVE_WITH_GRAVITY
    }

    static {
        POWER = DataTracker.registerData(CastingBubbleEntity.class, TrackedDataHandlerRegistry.BYTE);
        AVAILABLE_POWER = DataTracker.registerData(CastingBubbleEntity.class, TrackedDataHandlerRegistry.BYTE);
        INTERNAL_TICKS = DataTracker.registerData(CastingBubbleEntity.class, TrackedDataHandlerRegistry.BYTE);
        STATUS_BYTE = DataTracker.registerData(CastingBubbleEntity.class, TrackedDataHandlerRegistry.BYTE);
    }
}
