package data.scripts.world;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

class OmniFacCampaignPlugin extends BaseCampaignPlugin
{
    @Override
    public boolean isTransient()
    {
       return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget)
    {
        if (interactionTarget instanceof OrbitalStationAPI
                && OmniFac.isFactory(interactionTarget))
        {
            // TODO: Add custom interaction dialog
            return null;
        }

        return null;
    }
}
