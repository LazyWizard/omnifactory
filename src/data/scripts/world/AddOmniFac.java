package data.scripts.world;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;

@SuppressWarnings("unchecked")
public class AddOmniFac implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        StarSystemAPI system = sector.getStarSystem("Corvus");
        SectorEntityToken station = system.addOrbitalStation(
                system.getEntityByName("Corvus IV"), 315,
                300, 50, "Omnifactory", "neutral");
        system.addSpawnPoint(new OmniFac(station));
    }
}
