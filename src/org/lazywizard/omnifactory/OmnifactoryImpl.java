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
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
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
    private MemoryAPI memory;
    private SubmarketAPI storage;
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
        this.storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);

        memory = omnifactory.getMemory();
        memory.set(Constants.MEMKEY_MAX_BAYS, 1);
        memory.set(Constants.MEMKEY_MAX_FP, 3);

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

        CargoAPI cargo = storage.getCargo();
        FleetDataAPI fleetData = cargo.getMothballedShips();
        if (fleetData == null)
        {
            cargo.initMothballedShips(storage.getFaction().getId());
            fleetData = cargo.getMothballedShips();
        }

        // TODO: Check production for all already known blueprints
        // TODO: Implement bay system
        // TODO: Remove the following once the rules.csv behavior is in
        // Check for new ship/wing blueprints
        List<FleetMemberAPI> toRemove = new ArrayList<>();
        for (FleetMemberAPI member : fleetData.getMembersListCopy())
        {
            if (member.isFighterWing() && !Omnifactory.isBlueprintKnown(member))
            {
                toRemove.add(member);
                Omnifactory.addBlueprint(BlueprintType.WING, member.getSpecId(), false);
                Global.getSector().getCampaignUI().addMessage(
                        "New wing: " + member.getSpecId());
            }
            else if (!member.isFighterWing() && !Omnifactory.isBlueprintKnown(member))
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
            fleetData.removeFleetMember(member);
        }

        // Check for new weapon blueprints
        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (stack.isNull() || !stack.isWeaponStack())
            {
                continue;
            }

            String id = stack.getWeaponSpecIfWeapon().getWeaponId();
            if (!Omnifactory.isBlueprintKnown(BlueprintType.WEAPON, id))
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
        sprite.renderAtCenter(loc.x, loc.y);
    }

    @Override
    public float getRenderRange()
    {
        return omnifactory.getRadius() + 100f;
    }
}
