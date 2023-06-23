package zergnewbee.xiancraft.server.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import zergnewbee.xiancraft.server.block.XianCoreBlock;
import zergnewbee.xiancraft.server.block.entity.XianCoreBlockEntity;
import zergnewbee.xiancraft.server.entity.CastingPortalEntity;

import java.util.List;

import static zergnewbee.xiancraft.server.block.BlockRegisterFactory.XIAN_CORE_BLOCK;

public class BubbleCasterItem extends Item {

    public BubbleCasterItem(Settings settings) {
        super(settings);
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        this.onStoppedUsing(stack, user.world, user, 0);
        if (user instanceof PlayerEntity playerEntity) playerEntity.getItemCooldownManager().set(this, 20);
        return stack;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {

        if (user instanceof PlayerEntity playerEntity) {
            if (!checkXianCore(world, playerEntity, stack)) {
                playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
                return;
            }
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5f, 1.0f);

            playerEntity.getItemCooldownManager().set(this, 5);

            NbtCompound nbtCompound = stack.getNbt();
            if (nbtCompound != null) {
                float initPitch = nbtCompound.getFloat("InitPitch");
                float initYaw = nbtCompound.getFloat("InitYaw");

                int castingPos = CastingPortalEntity.getCastingPos(user, initPitch, initYaw, null);


                MutableText text;
                if (castingPos != 0) text = Text.translatable("item.xiancraft.bubble_caster.confirmed", castingPos);
                else text = Text.translatable("item.xiancraft.bubble_caster.canceled");
                sendHint(playerEntity, text);


                if (castingPos == 0) {
                    onCancelCasting(world, playerEntity);
                } else {
                    onConfirmCasting(world, playerEntity, castingPos, nbtCompound);
                }
            }
            // Tell the server what happened
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack itemStack = user.getStackInHand(hand);
        if (user.isSneaking()) {
            sendHint(user, Text.translatable("item.xiancraft.bubble_caster.check_core_position"));
            if(checkXianCore(world, user, itemStack))
                return TypedActionResult.success(itemStack, true);
        } else {
            user.playSound(SoundEvents.BLOCK_WOOD_BREAK, 1.0f, 1.0f );

            // Need to bind a coreBlock to work
            if (checkXianCore(world, user, itemStack)) {
                if (summonXianPortal(world, itemStack, user)) {
                    user.setCurrentHand(hand);
                    return TypedActionResult.consume(itemStack);
                }
            }
        }
        user.getItemCooldownManager().set(this, 20);
        return TypedActionResult.success(itemStack,true);
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();
        if (blockState.isOf(XIAN_CORE_BLOCK)) {
            return tryToBindCoreBlock( world, blockState, blockPos, player, hand) ? ActionResult.success(world.isClient) : ActionResult.PASS;
        } else {
            return ActionResult.PASS;
        }
    }

    public boolean tryToBindCoreBlock(World world, BlockState blockState, BlockPos blockPos, PlayerEntity player, Hand hand) {

        BlockEntity blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity instanceof XianCoreBlockEntity) {
            XianCoreBlockEntity.onBoot(world, blockPos, blockState, (XianCoreBlockEntity) blockEntity);

            ItemStack itemStack = player.getStackInHand(hand);
            NbtCompound nbtCompound = itemStack.getNbt();
            if (nbtCompound != null) {
                nbtCompound.putIntArray("CorePos", new int[]{blockPos.getX(), blockPos.getY(), blockPos.getZ()});
                sendHint(player, Text.translatable("item.xiancraft.bubble_caster.bound"));
                return true;
            }

        }
        return false;
    }

    public boolean checkXianCore(World world, PlayerEntity player, ItemStack itemStack) {
        NbtCompound nbtCompound = itemStack.getNbt();
        if (nbtCompound != null && nbtCompound.contains("CorePos", 11)) {
            int[] summonPos = nbtCompound.getIntArray("CorePos" );
            if (summonPos.length > 2) {
                BlockState blockState = world.getBlockState(new BlockPos(summonPos[0], summonPos[1], summonPos[2]));
                // Can cast only when the core is charged
                if (blockState.isOf(XIAN_CORE_BLOCK) ) {
                    if (!blockState.get(XianCoreBlock.CHARGED)) {
                        sendHint(player, Text.translatable("item.xiancraft.bubble_caster.uncharged"));
                        return false;
                    }
                    if (world.isClient()) {
                        Vec3d pos = Vec3d.ofCenter(new Vec3i(summonPos[0], summonPos[1] + 2, summonPos[2]));
                        world.addParticle(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z,
                                0.2 * (Math.random() - 0.5), 0.4 * (Math.random() - 0.5), 0.2 * (Math.random() - 0.5));
                    }
                    return true;
                }
            }
        }
        sendHint(player, Text.translatable("item.xiancraft.bubble_caster.need_to_bind_core"));

        return false;
    }

    public boolean summonXianPortal(World world, ItemStack itemStack, PlayerEntity user) {

        Vec3d direction = getAimVector(user.getPitch(), user.getYaw(), 0.0F, 1.0F);
        Vec3d startPoint = user.getEyePos();
        Vec3d endPoint = startPoint.add(direction);

        NbtCompound nbtCompound = itemStack.getNbt();
        if (nbtCompound != null) {
            float curYaw =  MathHelper.wrapDegrees(user.getYaw());
            float curPitch =  MathHelper.wrapDegrees(user.getPitch());

            nbtCompound.putFloat("InitPitch", curPitch);
            nbtCompound.putFloat("InitYaw", curYaw);
        }
        // Avoid creating a fake(ghost) entity
        if (!world.isClient) {
            CastingPortalEntity portalEntity = new CastingPortalEntity(world, endPoint, user);
            world.spawnEntity(portalEntity);
        }
        return true;

    }

    public void onCancelCasting(World world, PlayerEntity player){

        if (world.isClient()) {
            Vec3d direction = getAimVector(player.getPitch(), player.getYaw(), 0.0F, 1.0F);
            Vec3d startPoint = player.getEyePos();
            Vec3d endPoint = startPoint.add(direction);
            world.addImportantParticle(ParticleTypes.FLASH, endPoint.x, endPoint.y, endPoint.z, 2.2 * (Math.random() - 0.5), 2.4 * (Math.random() - 0.5), 2.2 * (Math.random() - 0.5));
        }
        List<CastingPortalEntity> list = player.world.getNonSpectatingEntities(CastingPortalEntity.class, new Box(player.getBlockPos()).expand(5));

        for (CastingPortalEntity portal:
                list) {
            // Not easy to interrupt a finished portal
            if (portal.getStatusFromDataChecker() == CastingPortalEntity.PortalStatus.FINISHED)
                portal.setPower((byte) (portal.getPower() - 30));
            else
                portal.setPower((byte) (portal.getPower() - 100));
        }
    }

    public void onConfirmCasting(World world, PlayerEntity player, int castingPos, NbtCompound nbt){

        int[] intArray = nbt.getIntArray("CorePos" );
        if (intArray.length > 2) {
            Vec3i corePos = new Vec3i(intArray[0], intArray[1], intArray[2]);
            List<CastingPortalEntity> list = player.world.getNonSpectatingEntities(CastingPortalEntity.class, new Box(player.getBlockPos()).expand(5));

            for (CastingPortalEntity portal :
                    list) {
                portal.onConfirmCasting(player, castingPos, corePos);
                world.addImportantParticle(ParticleTypes.SONIC_BOOM, portal.getX(), portal.getY(), portal.getZ(), 0, 0, 0);

            }
        }
    }


    public Vec3d getAimVector(float pitch, float yaw, float roll, float length) {
        float x = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float y = -MathHelper.sin((pitch + roll) * 0.017453292F);
        float z = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        return new Vec3d(length * x, length * y, length * z);
    }

    public int getMaxUseTime(ItemStack stack) {
        return 1198;
    }

    private void sendHint(PlayerEntity playerEntity, MutableText text) {
        if (playerEntity instanceof ServerPlayerEntity serverPlayerEntity) {
            serverPlayerEntity.sendMessage(text, true);
        }
    }

}
