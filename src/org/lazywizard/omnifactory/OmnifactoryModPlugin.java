package org.lazywizard.omnifactory;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;

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
        StarSystemAPI corvus = Global.getSector().getStarSystem("corvus");
        SectorEntityToken omnifactory = corvus.addCustomEntity(
                "omnifactory", null, "omnifactory", "player");
        omnifactory.setCircularOrbit(
                corvus.getEntityById("corvus_abandoned_station"), 0f, 150f, 15f);
        Omnifactory.addBlueprint(BlueprintType.SHIP, "hound", false);
        Omnifactory.addBlueprint(BlueprintType.WING, "talon_wing", false);
        Omnifactory.addBlueprint(BlueprintType.WEAPON, "lightmg", false);
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
