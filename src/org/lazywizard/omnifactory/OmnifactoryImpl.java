package org.lazywizard.omnifactory;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;
import org.lwjgl.util.vector.Vector2f;

// TODO: Rewrite this to handle static Omnifactory data
public class OmnifactoryImpl implements CustomCampaignEntityPlugin
{
    private transient SpriteAPI sprite;
    private SectorEntityToken omnifactory;
    private SubmarketAPI submarket;
    private long lastHeartbeat;
    private int numHeartbeats;

    private static MarketAPI createMarket(SectorEntityToken omnifactory)
    {
        MarketAPI market = Global.getFactory().createMarket(
                "omnifactory", "Omnifactory", 0);
        SharedData.getData().getMarketsWithoutPatrolSpawn().add("omnifactory");
        SharedData.getData().getMarketsWithoutTradeFleetSpawn().add("omnifactory");
        market.setPrimaryEntity(omnifactory);
        market.setFactionId("player");
        market.addCondition(Conditions.ABANDONED_STATION);
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        ((StoragePlugin) market.getSubmarket(Submarkets.SUBMARKET_STORAGE)
                .getPlugin()).setPlayerPaidToUnlock(true);
        Global.getSector().getEconomy().addMarket(market);
        return market;
    }

    @Override
    public void init(SectorEntityToken omnifactory)
    {
        this.omnifactory = omnifactory;

        // TODO: Add multi-Omnifactory market support
        MarketAPI market = Global.getSector().getEconomy().getMarket("omnifactory");
        if (market == null)
        {
            market = createMarket(omnifactory);
        }

        omnifactory.setMarket(market);
        this.submarket = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);

        // Synchronize factory heartbeat to the start of the next day
        final CampaignClockAPI clock = Global.getSector().getClock();
        lastHeartbeat = new GregorianCalendar(clock.getCycle(),
                clock.getMonth() - 1, clock.getDay()).getTimeInMillis();
        numHeartbeats = 0;
        readResolve();
    }

    public Object readResolve()
    {
        sprite = Global.getSettings().getSprite("stations", "omnifactory");
        return this;
    }

    /*private void checkBlueprint(Blueprint.BlueprintType type, String id,
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
     "Created: " + id + " (" + status.totalCreated
     + " lifetime total)");
     }
     else
     {
     status.lastHeartbeatUpdated = numHeartbeats;
     Global.getSector().getCampaignUI().addMessage(
     "Limit reached: " + id);
     }
     }
     }
     }*/
    private static void stripShip(FleetMemberAPI ship, CargoAPI cargo)
    {
        for (String slot : ship.getVariant().getNonBuiltInWeaponSlots())
        {
            cargo.addWeapons(ship.getVariant().getWeaponId(slot), 1);
        }
    }

    private void heartbeat()
    {
        lastHeartbeat = Global.getSector().getClock().getTimestamp();
        numHeartbeats++;
        Global.getSector().getCampaignUI().addMessage(
                "Heartbeat " + numHeartbeats);

        CargoAPI cargo = submarket.getCargo();
        if (cargo.getMothballedShips() == null)
        {
            cargo.initMothballedShips(submarket.getFaction().getId());
        }

        // Check production for all already known blueprints
        // TODO: Implement bay system
        /*for (Map.Entry<String, BlueprintStatus> entry : knownShips.entrySet())
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
         }*/
        // Check for new ship/wing blueprints
        List<FleetMemberAPI> toRemove = new ArrayList<>();
        for (FleetMemberAPI member : cargo.getMothballedShips().getMembersListCopy())
        {
            if (member.isFighterWing() && !Omnifactory.isKnownBlueprint(
                    BlueprintType.WING, member.getSpecId()))
            {
                toRemove.add(member);
                Omnifactory.addBlueprint(BlueprintType.WING, member.getSpecId(), false);
                Global.getSector().getCampaignUI().addMessage(
                        "New wing: " + member.getSpecId());
            }
            else if (!member.isFighterWing() && !Omnifactory.isKnownBlueprint(
                    BlueprintType.SHIP, member.getHullId()))
            {
                // Strip weapons from ship
                stripShip(member, cargo);
                toRemove.add(member);
                Omnifactory.addBlueprint(BlueprintType.SHIP, member.getHullId(), false);
                Global.getSector().getCampaignUI().addMessage(
                        "New hull: " + member.getHullId());
            }
        }
        for (FleetMemberAPI member : toRemove)
        {
            cargo.getMothballedShips().removeFleetMember(member);
        }

        // Check for new weapon blueprints
        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (stack.isNull() || !stack.isWeaponStack())
            {
                continue;
            }

            String id = stack.getWeaponSpecIfWeapon().getWeaponId();
            if (!Omnifactory.isKnownBlueprint(BlueprintType.WEAPON, id))
            {
                Omnifactory.addBlueprint(BlueprintType.WEAPON, id, false);
                cargo.removeWeapons((String) stack.getData(), 1);
                Global.getSector().getCampaignUI().addMessage(
                        "New weapon: " + id);
            }
        }
    }

    @Override
    public void advance(float amount)
    {
        if (Global.getSector().getClock().getElapsedDaysSince(lastHeartbeat) >= 1f)
        {
            heartbeat();
        }
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport)
    {
        Vector2f loc = omnifactory.getLocation();
        sprite.setSize(128, 128);
        sprite.setAngle(omnifactory.getFacing());
        sprite.setAlphaMult(viewport.getAlphaMult());
        sprite.setNormalBlend();
        //sprite.setAdditiveBlend();
        sprite.renderAtCenter(loc.x, loc.y);
    }

    @Override
    public float getRenderRange()
    {
        return omnifactory.getRadius() + 100f;
    }

    /*private class BlueprintStatus
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
     }*/
}
