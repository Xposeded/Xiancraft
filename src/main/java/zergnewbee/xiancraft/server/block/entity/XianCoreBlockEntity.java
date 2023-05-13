package zergnewbee.xiancraft.server.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import zergnewbee.xiancraft.server.block.BlockFactory;

import static zergnewbee.xiancraft.server.block.XianCoreBlock.CHARGED;

public class XianCoreBlockEntity extends BlockEntity {
    public XianCoreBlockEntity(BlockPos pos, BlockState state) {
        super(BlockFactory.XIAN_CORE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, XianCoreBlockEntity blockEntity) {
        if(world.getBlockState(pos).get(CHARGED)){
            LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
            if(lightningEntity != null) {
                lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos));
                world.spawnEntity(lightningEntity);
            }

            world.setBlockState(pos, state.with(CHARGED, false));
        }
    }


    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return super.toUpdatePacket();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return super.toInitialChunkDataNbt();
    }


}
