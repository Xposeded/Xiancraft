package zergnewbee.xiancraft.server;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zergnewbee.xiancraft.server.block.BlockFactory;
import zergnewbee.xiancraft.server.item.ItemFactory;


public class MainServerEntry implements ModInitializer  {

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("xiancraft");
    public static final String ModID = "xiancraft";

    public static final boolean shouldLog = true;
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Xiancraft serverside initializing");

        // Register blocks and items

        ItemFactory.registerAll();
        BlockFactory.registerAll();

        LOGGER.info("Xiancraft serverside initialized successfully");
    }

}
