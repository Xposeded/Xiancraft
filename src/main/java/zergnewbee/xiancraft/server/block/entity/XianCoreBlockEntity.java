package zergnewbee.xiancraft.server.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import zergnewbee.xiancraft.server.block.BlockRegisterFactory;

import static net.minecraft.state.property.Properties.*;
import static zergnewbee.xiancraft.server.block.XianCoreBlock.CHARGED;

public class XianCoreBlockEntity extends BlockEntity {

    private byte power;
    private byte internalTicks;

    public XianCoreBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegisterFactory.XIAN_CORE_BLOCK_ENTITY, pos, state);
        power = 0;
        internalTicks = 0;
    }

    public static void tick(World world, BlockPos pos, BlockState state, XianCoreBlockEntity blockEntity) {


        if (world.getBlockState(pos).get(CHARGED)) { //
            // Try to boot device
            if (blockEntity.power < 32) {
                blockEntity.internalTicks--;
                emitPower(world, pos, state, blockEntity, (byte) 1);
                // Boot end if internalTicks < 1
                if (blockEntity.internalTicks < 1) {
                    // Boot failed
                    if (blockEntity.power < 1) {
                        world.setBlockState(pos, state.with(CHARGED, false));
                        playSound(world, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH);
                    }
                    // Boot success
                    else {
                        blockEntity.power = 64;
                        playSound(world, pos, SoundEvents.BLOCK_BEACON_ACTIVATE);

                    }
                }
            }
            // Running, will shut down if power is low
            else {
                if (waitTicks(blockEntity)) return;
                emitPower(world, pos, state, blockEntity, (byte) 8);
                // Something went wrong, try rebooting
                if (blockEntity.power < 48) {
                    blockEntity.power = 8;
                    blockEntity.internalTicks = 32;
                    playSound(world, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE);
                }
            }

        }

    }


    public static void onBoot(World world, BlockPos pos, BlockState state, XianCoreBlockEntity blockEntity){
        if (blockEntity.power < 1){
            blockEntity.power = 8;
            blockEntity.internalTicks = 32;
            world.setBlockState(pos, state.with(CHARGED, true));
            playSound(world, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE);
        }
    }

    public static boolean waitTicks(XianCoreBlockEntity blockEntity){
        if (blockEntity.internalTicks > 0){
            blockEntity.internalTicks--;
            return true;
        }

        blockEntity.internalTicks = 4;

        return false;
    }

    public static void emitPower(World world, BlockPos pos, BlockState state, XianCoreBlockEntity blockEntity, byte value) {
        if (blockEntity.power < value) return;

        blockEntity.power -= value;

        BlockPos nextPos;
        Vec3d velocity;
        switch (state.get(HORIZONTAL_FACING)) {
            case EAST -> {
                nextPos = pos.south(15);
                velocity = Vec3d.of(Direction.SOUTH.getVector());
            }
            case SOUTH -> {
                nextPos = pos.west(15);
                velocity = Vec3d.of(Direction.WEST.getVector());
            }
            case WEST -> {
                nextPos = pos.north(15);
                velocity = Vec3d.of(Direction.NORTH.getVector());
            }
            default -> {
                nextPos = pos.east(15);
                velocity = Vec3d.of(Direction.EAST.getVector());
            }
        }

        BlockEntity blockEntity2 = world.getBlockEntity(nextPos);
        BlockState blockState2 = world.getBlockState(nextPos);
        if (blockEntity2 instanceof XianCoreBlockEntity) {
            // if charged, convey 1 power
            if (blockState2.get(CHARGED)) {
                if (((XianCoreBlockEntity) blockEntity2).power < 128 - value)
                    ((XianCoreBlockEntity) blockEntity2).power += value;
            }
            // if not charged, activate it
            else {
                if (((XianCoreBlockEntity) blockEntity2).power < 128 - value) {

                    XianCoreBlockEntity.onBoot(world, nextPos, blockState2, (XianCoreBlockEntity) blockEntity2);
                }
            }
        }

        if (world.isClient()) {
            Vec3d centerPos = Vec3d.ofCenter(pos);
            float factor;
            factor = 1.35f;

            world.addParticle(ParticleTypes.END_ROD, centerPos.x, centerPos.y, centerPos.z,
                    factor * velocity.x, factor * velocity.y + 0.03, factor * velocity.z);
        }
        markDirty(world,pos,state);
        markDirty(world,nextPos,blockState2);
    }

    public static void playSound(World world, BlockPos pos, SoundEvent sound) {
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        power = nbt.getByte("Power");
        internalTicks = nbt.getByte("ITicks");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putByte("Power", power);
        nbt.putByte("ITicks", internalTicks);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        // Client should sync data from server
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }


}
