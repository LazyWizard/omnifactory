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
    public void onEnabled(boolean wasEnabledBefore)
    {
        if (!wasEnabledBefore)
        {
            // Set up global Omnifactory details
            // DEBUG: Added blueprints to test factory
            Omnifactory.addBlueprint(BlueprintType.SHIP, "hound", false);
            Omnifactory.addBlueprint(BlueprintType.WING, "talon_wing", false);
            Omnifactory.addBlueprint(BlueprintType.WEAPON, "lightmortar", false);
            Global.getSector().registerPlugin(new OmnifactoryCampaignPlugin());

            // Set up local Omnifactory details
            // TODO: Implement this properly
            StarSystemAPI corvus = Global.getSector().getStarSystem("corvus");
            SectorEntityToken omnifactory = corvus.addCustomEntity(
                    Constants.TAG_OMNIFACTORY, null, "omnifactory", null);
            SectorEntityToken focus = corvus.getStar();
            omnifactory.setCircularOrbitPointingDown(focus, 0f, focus.getRadius() + 150f, 15f);
        }
    }
}
