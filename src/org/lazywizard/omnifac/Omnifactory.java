package org.lazywizard.omnifac;

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

public class Omnifactory implements EveryFrameScript
{
    // String = id, Integer = last heartbeat when a good was created
    private final Map<String, Integer> knownShips, knownWings, knownWeapons;
    private final SubmarketAPI submarket;
    private long lastHeartbeat;
    private int numHeartbeats;

    public Omnifactory(SubmarketAPI submarket)
    {
        this.submarket = submarket;

        knownShips = new LinkedHashMap<>();
        knownWings = new LinkedHashMap<>();
        knownWeapons = new LinkedHashMap<>();

        // Synchronize factory heartbeat to the start of the next day
        final CampaignClockAPI clock = Global.getSector().getClock();
        lastHeartbeat = new GregorianCalendar(clock.getCycle(),
                clock.getMonth() - 1, clock.getDay()).getTimeInMillis();
        numHeartbeats = 0;
    }

    public boolean addShipBlueprint(String hullId)
    {
        // Not a valid id
        if (!GoodData.getShipData().containsKey(hullId))
        {
            return false;
        }

        // Blueprint already known
        if (knownShips.containsKey(hullId))
        {
            return false;
        }

        // Register blueprint with the Omnifactory
        knownShips.put(hullId, numHeartbeats);
        return true;
    }

    public boolean removeShipBlueprint(String hullId)
    {
        return (knownShips.remove(hullId) != null);
    }

    public boolean addWingBlueprint(String wingId)
    {
        // Not a valid id
        if (!GoodData.getWingData().containsKey(wingId))
        {
            return false;
        }

        // Blueprint already known
        if (knownWings.containsKey(wingId))
        {
            return false;
        }

        // Register blueprint with the Omnifactory
        knownWings.put(wingId, numHeartbeats);
        return true;
    }

    public boolean removeWingBlueprint(String wingId)
    {
        return (knownWings.remove(wingId) != null);
    }

    public boolean addWeaponBlueprint(String weaponId)
    {
        // Not a valid id
        if (!GoodData.getWeaponData().containsKey(weaponId))
        {
            return false;
        }

        // Blueprint already known
        if (knownWeapons.containsKey(weaponId))
        {
            return false;
        }

        // Register blueprint with the Omnifactory
        knownWeapons.put(weaponId, numHeartbeats);
        return true;
    }

    public boolean removeWeaponBlueprint(String weaponId)
    {
        return (knownWeapons.remove(weaponId) != null);
    }

    public SubmarketAPI getSubmarket()
    {
        return submarket;
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
        CampaignClockAPI clock = Global.getSector().getClock();

        if (clock.getElapsedDaysSince(lastHeartbeat) >= 1f)
        {
            lastHeartbeat = clock.getTimestamp();
            numHeartbeats++;
            /*heartbeat();

             if (checkCargo())
             {
             warnedRequirements = false;
             }*/
        }
    }
}
