package zergnewbee.xiancraft.server.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import zergnewbee.xiancraft.server.block.entity.projectile.CastingBubbleEntity;

public class BubbleCasterItem extends Item {
    public BubbleCasterItem(Settings settings) {
        super(settings);
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack itemStack = user.getStackInHand(hand);
        world.playSound(null,user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5f, 1.0f);

        user.getItemCooldownManager().set(this, 5);

        if(!world.isClient){
            CastingBubbleEntity castingBubbleEntity = new CastingBubbleEntity(world, user);

            castingBubbleEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.5f, 1.0f);

            world.spawnEntity(castingBubbleEntity);
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if(!user.getAbilities().creativeMode){
            itemStack.decrement(1);
        }

        return TypedActionResult.success(itemStack, world.isClient());
    }
}
