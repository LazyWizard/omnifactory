package data.scripts.world;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OmniFacModPlugin extends BaseModPlugin
{
    private static final String OMNIFAC_ENABLED_FLAG = "lw_omnifac_enabled";
    private static final String STATION_NAME = "Omnifactory";
    private static final String STATION_FACTION = "neutral";

    // Responsible for creating the spawnpoint and adjusting factory settings
    // If you wish to modify a default, uncomment and change the relevent line
    private static void initStation(SectorEntityToken station, StarSystemAPI system)
    {
        OmniFac factory = new OmniFac(station);

        // Should the player be able to freely take goods from the factory?
        //station.getCargo().setFreeTransfer(false);

        // Sets if 'X added to station' messages appear. Defaults to false.
        //factory.setShowAddedCargo(false);
        // Sets if the station broadcasts when good analysis is done. Defaults to true.
        //factory.setShowAnalysisComplete(true);
        // Sets if 'Limit for X reached' messages appear. Defaults to true.
        //factory.setShowLimitReached(true);
        // Sets if buggy goods should be removed from memory. Defaults to false.
        //factory.setRemoveBrokenGoods(false);

        // Adds a hull type to the restricted list (can't be replicated). Defaults: none.
        //factory.addRestrictedShip("shuttle");
        // Removes a hull type from the restricted list.
        //factory.removeRestrictedShip("shuttle");
        // Adds a weapon to the restricted list (can't be replicated). Defaults: none.
        //factory.addRestrictedWeapon("lightmg");
        // Removes a weapon from the restricted list.
        //removeRestrictedWeapon("lightmg");

        // Sets ship blueprint analysis time modifier. Defaults to 1.0.
        //factory.setShipAnalysisTimeModifier(1.0f);
        // Sets weapon blueprint analysis time modifier. Defaults to 1.0.
        //factory.setWeaponAnalysisTimeModifier(1.0f);
        // Sets ship production time modifier. Defaults to 1.0.
        //factory.setShipProductionTimeModifier(1.0f);
        // Sets weapon production time modifier. Defaults to 1.0.
        //factory.setWeaponProductionTimeModifier(1.0f);

        // Sets the minimum amount of crew for the factory to function. Defaults to 0.
        //factory.setRequiredCrew(0);
        // Sets factory supply consumption per day. Defaults to 0.
        //factory.setRequiredSuppliesPerDay(0f);
        // Sets factory fuel consumption per day. Defaults to 0.
        //factory.setRequiredFuelPerDay(0f);

        // Sets how many of each fighter to produce. Retroactive. Defaults to 3.
        //factory.setMaxHullsPerFighter(3);
        // Sets how many of each frigate to produce. Retroactive. Defaults to 3.
        //factory.setMaxHullsPerFrigate(3);
        // Sets how many of each destroyer to produce. Retroactive. Defaults to 2.
        //factory.setMaxHullsPerDestroyer(2);
        // Sets how many of each cruiser to produce. Retroactive. Defaults to 2.
        //factory.setMaxHullsPerCruiser(2);
        // Sets how many of each capital ship to produce. Retroactive. Defaults to 1.
        //factory.setMaxHullsPerCapital(1);
        // Sets how many stacks of each weapon to produce. Defaults to 0.5.
        //factory.setMaxStacksPerWeapon(0.5f);

        system.addScript(factory);
    }

    // This will try to create the station in the following places (in order):
    // 1. If the system is Corvus, at Corvus IV
    // 2. If not Corvus, a planet with no existing satellites
    // 3. If no free planets, orbit adjacent the Abandoned Storage Facility
    // 4. If no Abandoned Storage Facility, orbiting the system's star
    private static void createStation(StarSystemAPI system)
    {
        SectorEntityToken station;

        // Check if there is an uninitiated Omnifactory already
        station = system.getEntityByName(STATION_NAME);
        if (station == null || OmniFac.isFactory(station))
        {
            // We are in the Corvus system (not a TC), so use Corvus IV
            if ("Corvus Star System".equals(system.getName()))
            {
                station = system.addOrbitalStation(
                        system.getEntityByName("Corvus IV"), 315,
                        300, 50, STATION_NAME, STATION_FACTION);
            }
            // Not in Corvus, find a good candidate for station placement
            else
            {
                SectorEntityToken tmp;
                List planets = new ArrayList(system.getPlanets());
                List stations = system.getOrbitalStations();

                // Why is this even IN there?
                planets.remove(system.getStar());

                // Remove all planets that have stations around them
                for (int x = 0; x < stations.size(); x++)
                {
                    tmp = (SectorEntityToken) stations.get(x);

                    if (tmp.getOrbit() != null)
                    {
                        planets.remove(tmp.getOrbit().getFocus());
                    }
                }

                // Are there any free planets?
                if (!planets.isEmpty())
                {
                    station = system.addOrbitalStation(
                            (SectorEntityToken) planets.get(0), 315,
                            300, 50, STATION_NAME, STATION_FACTION);
                }
                else
                {
                    tmp = system.getEntityByName("Abandoned Storage Facility");

                    // Does the Abandoned Storage Facility exist?
                    if (tmp != null)
                    {
                        station = system.addOrbitalStation(tmp.getOrbit().getFocus(),
                                315, 300, 50, STATION_NAME, STATION_FACTION);
                    }
                    else
                    {
                        // I give up, just use the star
                        if (system.getStar() != null)
                        {
                            station = system.addOrbitalStation(system.getStar(), 315,
                                    7250, 50, STATION_NAME, STATION_FACTION);
                        }
                        // Just in case somehow there's no sun in this system
                        else
                        {
                            station = system.addOrbitalStation(system.createToken(0f, 0f),
                                    0, 0, 50, STATION_NAME, STATION_FACTION);
                        }
                    }
                }
            }
        }

        // Finally, create the factory
        initStation(station, system);
    }

    private static void initOmniFac()
    {
        StarSystemAPI system = Global.getSector().getStarSystems().get(0);
        createStation(system);
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
    }
}
