package org.lazywizard.omnifactory;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

public class OmnifactoryModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        OmnifactorySettings.reloadSettings();
    }

    @Override
    public void onGameLoad()
    {
        // TODO: Implement this properly
        Omnifactory fakeFactory = new Omnifactory(
                Global.getSector().getStarSystem("corvus")
                .getEntityById("corvus_abandoned_station")
                .getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE));
        fakeFactory.addShipBlueprint("hound");
        fakeFactory.addWingBlueprint("talon_wing");
        fakeFactory.addWeaponBlueprint("lightmg");
        Global.getSector().addTransientScript(fakeFactory);
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore)
    {
        if (!wasEnabledBefore)
        {
            // TODO
        }
    }
}
