package org.lazywizard.omnifac;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import java.util.Map;

public class OmniFacModPlugin extends BaseModPlugin
{
    private static final String OMNIFAC_ENABLED_FLAG = "lw_omnifac_enabled";

    private static void initOmniFac()
    {
        new AddOmniFac().generate(Global.getSector());
        Global.getSector().addScript(new OmniFacMaster());
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore)
    {
        Map data = Global.getSector().getPersistentData();
        if (!data.containsKey(OMNIFAC_ENABLED_FLAG))
        {
            data.put(OMNIFAC_ENABLED_FLAG, true);
            initOmniFac();
        }

        /*if (!wasEnabledBefore)
         {
         initOmniFac();
         }*/
    }
}
