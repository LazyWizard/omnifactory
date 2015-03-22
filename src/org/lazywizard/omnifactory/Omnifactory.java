package org.lazywizard.omnifactory;

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;

public class Omnifactory implements EveryFrameScript
{
    private final Map<String, BlueprintStatus> knownShips, knownWings, knownWeapons;
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
        if (!BlueprintMaster.getShipBlueprints().containsKey(hullId))
        {
            throw new RuntimeException("No such hull: " + hullId);
        }

        // Blueprint already known
        if (knownShips.containsKey(hullId))
        {
            return false;
        }

        // Register blueprint with the Omnifactory
        knownShips.put(hullId, new BlueprintStatus(false));
        return true;
    }

    public boolean removeShipBlueprint(String hullId)
    {
        return (knownShips.remove(hullId) != null);
    }

    public boolean addWingBlueprint(String wingId)
    {
        // Not a valid id
        if (!BlueprintMaster.getWingBlueprints().containsKey(wingId))
        {
            throw new RuntimeException("No such wing: " + wingId);
        }

        // Blueprint already known
        if (knownWings.containsKey(wingId))
        {
            return false;
        }

        // Register blueprint with the Omnifactory
        knownWings.put(wingId, new BlueprintStatus(false));
        return true;
    }

    public boolean removeWingBlueprint(String wingId)
    {
        return (knownWings.remove(wingId) != null);
    }

    public boolean addWeaponBlueprint(String weaponId)
    {
        // Not a valid id
        if (!BlueprintMaster.getWeaponBlueprints().containsKey(weaponId))
        {
            throw new RuntimeException("No such weapon: " + weaponId);
        }

        // Blueprint already known
        if (knownWeapons.containsKey(weaponId))
        {
            return false;
        }

        // Register blueprint with the Omnifactory
        knownWeapons.put(weaponId, new BlueprintStatus(false));
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

    private void checkBlueprint(Blueprint.BlueprintType type, String id,
            BlueprintStatus status, CargoAPI cargo)
    {
        Blueprint blueprint = BlueprintMaster.getBlueprint(type, id);
        if (!status.isAnalyzed)
        {
            if (numHeartbeats - status.lastHeartbeatUpdated >= blueprint.getDaysToAnalyze())
            {
                status.isAnalyzed = true;
                status.lastHeartbeatUpdated = numHeartbeats;
                Global.getSector().getCampaignUI().addMessage(
                        "Analysis complete: " + id);
            }
        }
        else
        {
            if (numHeartbeats - status.lastHeartbeatUpdated
                    >= blueprint.getDaysToCreate())
            {
                if (blueprint.getTotalInCargo(cargo) < blueprint.getLimit())
                {
                    blueprint.create(cargo);
                    status.totalCreated++;
                    status.lastHeartbeatUpdated = numHeartbeats;
                    Global.getSector().getCampaignUI().addMessage(
                            "Created: " + id);
                }
                else
                {
                    status.lastHeartbeatUpdated = numHeartbeats;
                    Global.getSector().getCampaignUI().addMessage(
                            "Limit reached: " + id);
                }
            }
        }
    }

    private void heartbeat()
    {
        CargoAPI cargo = submarket.getCargo();
        if (cargo.getMothballedShips() == null)
        {
            cargo.initMothballedShips(submarket.getFaction().getId());
        }

        for (Map.Entry<String, BlueprintStatus> entry : knownShips.entrySet())
        {
            checkBlueprint(BlueprintType.SHIP, entry.getKey(), entry.getValue(), cargo);
        }

        for (Map.Entry<String, BlueprintStatus> entry : knownWings.entrySet())
        {
            checkBlueprint(BlueprintType.WING, entry.getKey(), entry.getValue(), cargo);
        }

        for (Map.Entry<String, BlueprintStatus> entry : knownWeapons.entrySet())
        {
            checkBlueprint(BlueprintType.WEAPON, entry.getKey(), entry.getValue(), cargo);
        }
    }

    @Override
    public void advance(float amount)
    {
        CampaignClockAPI clock = Global.getSector().getClock();

        if (clock.getElapsedDaysSince(lastHeartbeat) >= 1f)
        {
            lastHeartbeat = clock.getTimestamp();
            numHeartbeats++;
            Global.getSector().getCampaignUI().addMessage(
                    "Heartbeat " + numHeartbeats);
            heartbeat();

            // TODO: Check cargo for new blueprints
        }
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

    public class BlueprintStatus
    {
        private boolean isAnalyzed;
        private int lastHeartbeatUpdated, totalCreated;

        private BlueprintStatus(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
            lastHeartbeatUpdated = numHeartbeats;
            totalCreated = 0;
        }

        public boolean isAnalyzed()
        {
            return isAnalyzed;
        }

        public void setAnalyzed(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
        }

        public int getTotalCreated()
        {
            return totalCreated;
        }
    }
}
