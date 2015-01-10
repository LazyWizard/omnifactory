package org.lazywizard.omnifac;

import java.util.Collections;
import java.util.List;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import org.apache.log4j.Level;

public class OmniFacModPlugin extends BaseModPlugin
{
    public static void initOmnifactory(SectorEntityToken factory)
    {
        // Only one controller script per factory
        if (OmniFac.isFactory(factory))
        {
            throw new RuntimeException(factory.getFullName()
                    + " is already an Omnifactory!");
        }

        // Set up market data for the Omnifactory
        MarketAPI market = Global.getFactory().createMarket(
                Constants.STATION_ID, Constants.STATION_NAME, 0);
        SharedData.getData().getMarketsWithoutPatrolSpawn().add(Constants.STATION_ID);
        SharedData.getData().getMarketsWithoutTradeFleetSpawn().add(Constants.STATION_ID);
        market.setPrimaryEntity(factory);
        market.setFactionId(Constants.STATION_FACTION);
        market.addCondition(Conditions.ABANDONED_STATION);
        market.addSubmarket(Constants.SUBMARKET_ID);
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        ((StoragePlugin) market.getSubmarket(Submarkets.SUBMARKET_STORAGE)
                .getPlugin()).setPlayerPaidToUnlock(true);
        factory.setMarket(market);

        // Add the Omnifactory controller script
        OmniFac facScript = new OmniFac(factory);
        factory.getContainingLocation().addScript(facScript);
    }

    private static SectorEntityToken createOmnifactory()
    {
        SectorAPI sector = Global.getSector();
        StarSystemAPI corvus = sector.getStarSystem("corvus");
        if (corvus != null)
        {
            // By default the Omnifactory orbits the planet Somnus in the Corvus system
            return corvus.addOrbitalStation(Constants.STATION_ID,
                    corvus.getEntityById("corvus_IV"), 315f,
                    300f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
        }

        // For total conversions, orbit a random star
        List<StarSystemAPI> systems = sector.getStarSystems();
        Collections.shuffle(systems);
        for (StarSystemAPI system : systems)
        {
            if (system.getStar() != null)
            {
                Global.getLogger(OmniFacModPlugin.class).log(Level.INFO,
                        "Omnifactory random starting location: orbiting "
                        + system.getBaseName() + "'s star");
                return system.addOrbitalStation(Constants.STATION_ID,
                        system.getStar(), 315f, (system.getStar().getRadius() * 1.5f)
                        + 50f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
            }
        }

        // In the unlikely situation where all stars in the universe somehow vanish...
        throw new RuntimeException("Could not find a valid Omnifactory location!");
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
            initOmnifactory(createOmnifactory());
        }
    }
}
