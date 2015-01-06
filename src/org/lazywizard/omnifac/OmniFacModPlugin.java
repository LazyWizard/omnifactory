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
        SectorEntityToken station = null;
        SectorAPI sector = Global.getSector();
        StarSystemAPI system = sector.getStarSystem("corvus");
        if (system != null)
        {
            // By default the Omnifactory orbits the planet Somnus in the Corvus system
            station = system.addOrbitalStation(Constants.STATION_ID,
                    system.getEntityById("corvus_IV"), 315f,
                    300f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
        }
        else
        {
            // For total conversions, orbit the first available star
            for (StarSystemAPI tmp : sector.getStarSystems())
            {
                if (tmp.getStar() != null)
                {
                    system = tmp;
                    station = system.addOrbitalStation(Constants.STATION_ID,
                            system.getStar(), 315f, system.getStar().getRadius() * 1.5f,
                            50f, Constants.STATION_NAME, Constants.STATION_FACTION);
                    break;
                }
            }
        }

        // In the unlikely situation where all stars in the universe somehow vanish...
        if (system == null || station == null)
        {
            throw new RuntimeException("Could not find a valid Omnifactory location!");
        }

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
    public void onApplicationLoad() throws Exception
    {
        OmniFacSettings.reloadSettings();
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
