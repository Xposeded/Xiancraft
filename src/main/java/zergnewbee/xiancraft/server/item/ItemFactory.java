package zergnewbee.xiancraft.server.item;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import zergnewbee.xiancraft.client.render.entity.CastingBubbleEntityRenderer;
import zergnewbee.xiancraft.server.block.entity.projectile.CastingBubbleEntity;

import static zergnewbee.xiancraft.server.MainServerEntry.ModID;


public class ItemFactory {


    public static final AdminItem ADMIN_ITEM  = new AdminItem(new FabricItemSettings());
    public static ItemGroup ITEM_GROUP = FabricItemGroup.builder(
            new Identifier(ModID,"test_group")).icon(()->new ItemStack(ADMIN_ITEM)).build();

    public static final BubbleCasterItem BUBBLE_CASTER_ITEM = new BubbleCasterItem(new Item.Settings().maxCount(16));
    public static final ExampleItem EXAMPLE_ITEM = new ExampleItem(new FabricItemSettings().maxCount(16));


    public static final EntityType<CastingBubbleEntity> CASTING_BUBBLE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(ModID, "casting_bubble"),
            FabricEntityTypeBuilder.<CastingBubbleEntity>create(SpawnGroup.MISC, CastingBubbleEntity::new).
                    dimensions(EntityDimensions.fixed(0.25f, 0.25f)).trackRangeBlocks(128).trackedUpdateRate(10).build()
    );

    public static void registerAll() {

        // Register  items in here
        register(ADMIN_ITEM,"admin_item");
        register(BUBBLE_CASTER_ITEM, "bubble_caster");
        register(EXAMPLE_ITEM,"example_item");

    }

    public static void registerAll_Client() {
        EntityRendererRegistry.register(CASTING_BUBBLE_ENTITY, CastingBubbleEntityRenderer::new);

    }

        public static void register(Item item, String string) {

        Registry.register(Registries.ITEM, new Identifier(ModID,string), item);
        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> content.add(item));
    }


}
