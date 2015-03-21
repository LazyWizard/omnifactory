package org.lazywizard.omnifactory;

import java.io.IOException;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.json.JSONException;

public class OmnifactoryModPlugin extends BaseModPlugin
{
    private static boolean HAS_LOADED = false;

    @Override
    public void onGameLoad()
    {
        if (!HAS_LOADED)
        {
            HAS_LOADED = true;

            try
            {
                OmnifactorySettings.reloadSettings();
                BlueprintMaster.reloadBlueprints();
            }
            catch (JSONException | IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }

        Global.getSector().addTransientScript(new Omnifactory(
                Global.getSector().getEconomy().getMarketsCopy().get(0).getSubmarketsCopy().get(0)));
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
