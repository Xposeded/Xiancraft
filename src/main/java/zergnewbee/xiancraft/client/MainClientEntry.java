package zergnewbee.xiancraft.client;

import net.fabricmc.api.ClientModInitializer;

import static zergnewbee.xiancraft.server.item.ItemFactory.registerAll_Client;

public class MainClientEntry implements ClientModInitializer {

    public void onInitializeClient() {
        registerAll_Client();
    }
}
