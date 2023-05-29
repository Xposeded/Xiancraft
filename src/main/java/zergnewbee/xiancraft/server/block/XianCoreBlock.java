package zergnewbee.xiancraft.server.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import zergnewbee.xiancraft.server.block.entity.XianCoreBlockEntity;

import java.util.Objects;

public class XianCoreBlock extends BlockWithEntity implements BlockEntityProvider {

    public static final BooleanProperty CHARGED = BooleanProperty.of("charged");
    public XianCoreBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(CHARGED, false).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
        builder.add(Properties.HORIZONTAL_FACING);
    }


    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {

        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new XianCoreBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, BlockRegisterFactory.XIAN_CORE_BLOCK_ENTITY, (XianCoreBlockEntity::tick));
    }
}
