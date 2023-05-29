package zergnewbee.xiancraft.server.item;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import zergnewbee.xiancraft.client.particle.CastingPortalParticle;
import zergnewbee.xiancraft.client.render.entity.CastingBubbleEntityRenderer;
import zergnewbee.xiancraft.client.render.entity.CastingPortalEntityRenderer;
import zergnewbee.xiancraft.server.entity.CastingPortalEntity;
import zergnewbee.xiancraft.server.entity.projectile.CastingBubbleEntity;

import static zergnewbee.xiancraft.server.MainServerEntry.ModID;


/**
 * Register items, particles, entities, renderers.
 */
public class NonBlockRegisterFactory {

    // Items
    public static final AdminItem ADMIN_ITEM  = new AdminItem(new FabricItemSettings());
    public static ItemGroup ITEM_GROUP = FabricItemGroup.builder(
            new Identifier(ModID,"xiancraft")).icon(()->new ItemStack(ADMIN_ITEM)).build();

    public static final BubbleCasterItem BUBBLE_CASTER_ITEM = new BubbleCasterItem(new Item.Settings().maxCount(1).maxDamage(100));
    public static final ExampleItem EXAMPLE_ITEM = new ExampleItem(new FabricItemSettings().maxCount(16));

    // Particles
    public static final DefaultParticleType CASTING_PORTAL_PARTICLE = FabricParticleTypes.simple();

    // Entities
    public static final EntityType<CastingBubbleEntity> CASTING_BUBBLE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(ModID, "casting_bubble"),
            FabricEntityTypeBuilder.<CastingBubbleEntity>create(SpawnGroup.MISC, CastingBubbleEntity::new).
                    dimensions(EntityDimensions.fixed(0.25f, 0.25f)).trackRangeBlocks(256).trackedUpdateRate(10).build()
    );

    public static final EntityType<CastingPortalEntity> CASTING_PORTAL_ENTITY = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(ModID, "casting_portal"),
            FabricEntityTypeBuilder.<CastingPortalEntity>create(SpawnGroup.MISC, CastingPortalEntity::new).
                    dimensions(EntityDimensions.fixed(0.25f, 0.25f)).trackRangeBlocks(256).trackedUpdateRate(20).build()
    );

    public static void registerAll() {

        // Register items
        register(ADMIN_ITEM,"admin_item");
        register(BUBBLE_CASTER_ITEM, "bubble_caster");
        register(EXAMPLE_ITEM,"example_item");

        // Register particles
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(ModID, "casting_portal_particle"), CASTING_PORTAL_PARTICLE);

    }

    public static void registerAll_Client() {
        // Register entity renderers
        EntityRendererRegistry.register(CASTING_BUBBLE_ENTITY, CastingBubbleEntityRenderer::new);
        EntityRendererRegistry.register(CASTING_PORTAL_ENTITY, CastingPortalEntityRenderer::new);

        // Register particles
        ParticleFactoryRegistry.getInstance().register(CASTING_PORTAL_PARTICLE, CastingPortalParticle.Factory::new);
    }

    public static void register(Item item, String string) {

        Registry.register(Registries.ITEM, new Identifier(ModID, string), item);
        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> content.add(item));
    }


}
