package org.lazywizard.omnifactory;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;

class OmnifactoryCampaignPlugin extends BaseCampaignPlugin
{
    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(
            SectorEntityToken interactionTarget)
    {
        if (interactionTarget.hasTag(Constants.TAG_OMNIFACTORY))
        {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(), PickPriority.MOD_SPECIFIC);
        }

        return null;
    }

    @Override
    // TODO: Open Omnifactory remote dialog when comm notification is clicked
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(
            Object param, SectorEntityToken interactionTarget)
    {
        return null;
    }
}
