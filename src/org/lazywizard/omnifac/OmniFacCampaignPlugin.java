package org.lazywizard.omnifac;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class OmniFacCampaignPlugin extends BaseCampaignPlugin
{
    @Override
    public String getId()
    {
        return "omnifac";
    }

    @Override
    public boolean isTransient()
    {
        return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget)
    {
        // The Omnifactory uses a special interaction dialog
        if (OmniFac.isFactory(interactionTarget))
        {
            // PluginPick is a wrapper that allows you to assign a priority to a plugin
            // The highest priority plugin returned by all mods' CampaignPlugins will be the one used
            return new PluginPick<InteractionDialogPlugin>(new OmniFacInteractionPlugin(),
                    PickPriority.MOD_SPECIFIC);
        }

        // Returning null means this mod doesn't have a specific interaction plugin for this entity
        // If all mods return null, the vanilla interaction behavior for that entity will be used
        return null;
    }
}
