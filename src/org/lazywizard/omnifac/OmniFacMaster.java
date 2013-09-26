package org.lazywizard.omnifac;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OmniFacMaster implements EveryFrameScript
{
    private final Map<SectorEntityToken, OmniFac> allFactories = new HashMap<SectorEntityToken, OmniFac>();
    private long lastHeartbeat;

    public OmniFacMaster(SectorAPI sector)
    {
        lastHeartbeat = sector.getClock().getTimestamp();
    }

        public boolean isFactory(SectorEntityToken station)
    {
        return allFactories.keySet().contains(station);
    }

    public OmniFac getFactory(SectorEntityToken station)
    {
        if (!isFactory(station))
        {
            return null;
        }

        return allFactories.get(station);
    }

    public List<SectorEntityToken> getFactories()
    {
        return new ArrayList(allFactories.keySet());
    }

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return false;
    }

    @Override
    public void advance(float amount)
    {
        for (OmniFac fac : allFactories.values())
        {
            fac.advance(amount);
        }
    }
}
