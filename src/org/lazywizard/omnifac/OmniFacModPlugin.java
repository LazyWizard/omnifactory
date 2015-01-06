package org.lazywizard.omnifac;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;

public class OmniFacModPlugin extends BaseModPlugin
{
    private static void initOmniFac()
    {
        SectorAPI sector = Global.getSector();
        StarSystemAPI system = sector.getStarSystem("corvus");
        if (system == null)
        {
            // TODO: Change spawning logic to support total conversions again
            throw new RuntimeException("Omnifactory could not find Corvus!");
        }

        // By default the Omnifactory orbits the planet Somnus in the Corvus system
        SectorEntityToken station = system.addOrbitalStation(Constants.STATION_ID,
                system.getEntityById("corvus_IV"), 315f,
                300f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);

        // Set up market data for the Omnifactory
        MarketAPI market = Global.getFactory().createMarket(
                Constants.STATION_ID, Constants.STATION_NAME, 0);
        SharedData.getData().getMarketsWithoutPatrolSpawn().add(Constants.STATION_ID);
        SharedData.getData().getMarketsWithoutTradeFleetSpawn().add(Constants.STATION_ID);
        market.setPrimaryEntity(station);
        market.setFactionId(Constants.STATION_FACTION);
        market.addCondition(Conditions.ABANDONED_STATION);
        market.addSubmarket(Constants.SUBMARKET_ID);
        station.setMarket(market);

        // Add the Omnifactory controller script to the station
        OmniFac factory = new OmniFac(station);
        system.addScript(factory);
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore)
    {
        if (!wasEnabledBefore)
        {
            initOmniFac();
        }
    }
}
