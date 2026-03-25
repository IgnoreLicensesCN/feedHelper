package com.linearity.feedhelper.client.event;

import com.linearity.feedhelper.client.utils.ActiveDefenseRelated;
import com.linearity.feedhelper.client.utils.ClientShotPredictor;
import com.linearity.feedhelper.config.FeatureToggle;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;

import static com.linearity.feedhelper.client.FeedhelperClient.*;
import static com.linearity.feedhelper.client.utils.ElytraHelper.avoidElytraCollisionLoop;

public class ClientTickHandler implements IClientTickHandler {
    @Override
    public void onClientTick(Minecraft client) {

        if (FeatureToggle.FILL_LAVA.getBooleanValue()){
            fillLavaLoop(client);
        }
        if (FeatureToggle.PLACE_WATER_PREVENT_DAMAGE.getBooleanValue()) {
            checkIfPlaceWater(client);
        }
        if (FeatureToggle.AUTO_ATTACK.getBooleanValue()){
            autoAttackLoop(client);
        }
        if (FeatureToggle.ACTIVE_DEFENSE.getBooleanValue()) {
            ActiveDefenseRelated.activeDefenseLoop(client);
        }
        if (FeatureToggle.ACTIVE_DEFENSE_SHIELDING.getBooleanValue()) {
            ActiveDefenseRelated.activeDefenseShieldingLoop(client);
        }
        if (FeatureToggle.AUTO_TORCH.getBooleanValue()){
            autoTorchLoop(client);
        }
        if (FeatureToggle.AVOID_ELYTRA_COLLISION.getBooleanValue()){
            avoidElytraCollisionLoop(client);
        }
        while (!runOnNextTickQueue.isEmpty()) {
            Runnable r = runOnNextTickQueue.poll();
            if (r != null) {
                r.run();
            }
        }

        ClientShotPredictor.onClientTick(client);
    }
}
