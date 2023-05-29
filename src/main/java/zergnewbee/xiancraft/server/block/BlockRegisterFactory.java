package zergnewbee.xiancraft.server.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import zergnewbee.xiancraft.server.block.entity.XianCoreBlockEntity;

import static zergnewbee.xiancraft.server.MainServerEntry.ModID;
import static zergnewbee.xiancraft.server.item.NonBlockRegisterFactory.ITEM_GROUP;

/**
 * Register blocks, blockEntities and item group.
 */
public class BlockRegisterFactory {

    public static final XianCoreBlock XIAN_CORE_BLOCK = new XianCoreBlock(FabricBlockSettings.of(Material.METAL).
            hardness(4.0f).nonOpaque().luminance(8));
    public static final BlockEntityType<XianCoreBlockEntity> XIAN_CORE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(ModID, "xian_core_block_entity"),
            FabricBlockEntityTypeBuilder.create(XianCoreBlockEntity::new, XIAN_CORE_BLOCK).build());

    // Register blocks
    public static void registerAll(){


        // Register normal blocks in here
        register(XIAN_CORE_BLOCK,"xian_core_block");


    }

    public static void register(Block block, String string) {

        Registry.register(Registries.BLOCK, new Identifier(ModID,string), block);
        Registry.register(Registries.ITEM, new Identifier(ModID,string), new BlockItem(block, new FabricItemSettings()));

        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> content.add(block));
    }
}
