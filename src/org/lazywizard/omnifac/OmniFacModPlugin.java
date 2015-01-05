package org.lazywizard.omnifac;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

// TODO: Rewrite spawning logic to use new tags system
public class OmniFacModPlugin extends BaseModPlugin
{
    private static void initStation(SectorEntityToken station, StarSystemAPI system)
    {
        OmniFac factory = new OmniFac(station, Constants.SETTINGS_FILE);
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
        station = system.getEntityByName(Constants.STATION_NAME);
        if (station == null || OmniFac.isFactory(station))
        {
            // We are in the Corvus system (not a TC), so use Corvus IV
            if ("Corvus Star System".equals(system.getName()))
            {
                station = system.addOrbitalStation(Constants.STATION_ID,
                        system.getEntityByName("Corvus IV"), 315f,
                        300f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
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
                    station = system.addOrbitalStation(Constants.STATION_ID,
                            (SectorEntityToken) planets.get(0), 315f,
                            300f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
                }
                else
                {
                    tmp = system.getEntityByName("Abandoned Storage Facility");

                    // Does the Abandoned Storage Facility exist?
                    if (tmp != null)
                    {
                        station = system.addOrbitalStation(Constants.STATION_ID, tmp.getOrbit().getFocus(),
                                315f, 300f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
                    }
                    else
                    {
                        // I give up, just use the star
                        if (system.getStar() != null)
                        {
                            station = system.addOrbitalStation(Constants.STATION_ID, system.getStar(), 315f,
                                    7250f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
                        }
                        // Just in case somehow there's no sun in this system
                        else
                        {
                            station = system.addOrbitalStation(Constants.STATION_ID, system.createToken(0f, 0f),
                                    0f, 0f, 50f, Constants.STATION_NAME, Constants.STATION_FACTION);
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
        if (!wasEnabledBefore)
        {
            initOmniFac();
        }
    }
}
