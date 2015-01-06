package org.lazywizard.omnifac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.MessageUtils;

// TODO: Make settings global instead of per-factory
// TODO: Make restricted goods/ships loaded from merged CSV
// TODO: Switch to one transient script that runs all factories
public class OmniFac implements EveryFrameScript
{
    private String settingsFile;
    private transient OmniFacSettings settings;
    private final Map<String, ShipData> shipData = new HashMap<>();
    private final Map<String, WeaponData> wepData = new HashMap<>();
    private final Map<String, Boolean> restrictedShips = new HashMap<>();
    private final Map<String, Boolean> restrictedWeps = new HashMap<>();
    private final SectorEntityToken station;
    private transient CargoAPI cargo;
    private long lastHeartbeat;
    private int numHeartbeats = 0;
    private boolean warnedRequirements = true;

    //<editor-fold desc="Constructor">
    public OmniFac(SectorEntityToken station, String settingsFile)
    {
        this.station = station;
        lastHeartbeat = Global.getSector().getClock().getTimestamp();
        setSettingsFile(settingsFile);
        getFactoryMap().put(station, this);
    }

    public OmniFac(SectorEntityToken station)
    {
        this(station, Constants.SETTINGS_FILE);
    }

    public Object readResolve()
    {
        if (settingsFile == null)
        {
            settingsFile = Constants.SETTINGS_FILE;
        }

        setSettingsFile(settingsFile);
        return this;
    }
    //</editor-fold>

    //<editor-fold desc="Static methods">
    public static boolean isFactory(SectorEntityToken station)
    {
        return getFactoryMap().keySet().contains(station);
    }

    public static OmniFac getFactory(SectorEntityToken station)
    {
        return getFactoryMap().get(station);
    }

    private static Map<SectorEntityToken, OmniFac> getFactoryMap()
    {
        Map<SectorEntityToken, OmniFac> allFactories
                = (Map<SectorEntityToken, OmniFac>) Global.getSector()
                .getPersistentData().get(Constants.FACTORY_DATA_ID);

        if (allFactories == null)
        {
            allFactories = new HashMap<>();
            Global.getSector().getPersistentData()
                    .put(Constants.FACTORY_DATA_ID, allFactories);
        }

        return allFactories;
    }

    public static List<SectorEntityToken> getFactories()
    {
        return new ArrayList(getFactoryMap().keySet());
    }

    public static String parseHullName(FleetMemberAPI ship)
    {
        if (ship.isFighterWing())
        {
            return ship.getSpecId();
        }

        return ship.getHullId();
    }
    //</editor-fold>

    //<editor-fold desc="Factory settings">
    public OmniFacSettings getSettings()
    {
        return settings;
    }

    public String getSettingsFile()
    {
        return settingsFile;
    }

    public void setSettingsFile(String settingsFile)
    {
        try
        {
            settings = new OmniFacSettings(settingsFile);
        }
        catch (IOException ex)
        {
            Global.getLogger(OmniFac.class).log(Level.ERROR,
                    "Failed to load settings file: " + settingsFile, ex);
            return;
        }
        catch (JSONException ex)
        {
            Global.getLogger(OmniFac.class).log(Level.ERROR,
                    "Failed to parse settings file: " + settingsFile, ex);
            return;
        }

        this.settingsFile = settingsFile;
        Global.getLogger(OmniFac.class).log(Level.INFO,
                "Loaded settings successfully");
    }

    public void addRestrictedShip(String hullId)
    {
        if (hullId.endsWith("_Hull"))
        {
            hullId = hullId.substring(0, hullId.lastIndexOf("_Hull"));
        }

        restrictedShips.put(hullId, false);
    }

    public void addRestrictedWeapon(String weaponId)
    {
        restrictedWeps.put(weaponId, false);
    }

    public void removeRestrictedShip(String hullId)
    {
        if (hullId.endsWith("_Hull"))
        {
            hullId = hullId.substring(0, hullId.lastIndexOf("_Hull"));
        }

        restrictedShips.remove(hullId);
    }

    public void removeRestrictedWeapon(String weaponId)
    {
        restrictedWeps.remove(weaponId);
    }
    //</editor-fold>

    //<editor-fold desc="Stack/ship analysis">
    public boolean isUnknownShip(FleetMemberAPI ship)
    {
        String id = parseHullName(ship);
        return !shipData.containsKey(id);
    }

    public boolean isUnknownWeapon(CargoStackAPI stack)
    {
        // We only deal with weapons, not resources
        String id = (String) stack.getData();
        return (stack.isWeaponStack() && !wepData.containsKey(id));
    }

    public boolean isRestrictedShip(FleetMemberAPI ship)
    {
        return restrictedShips.containsKey(parseHullName(ship));
    }

    public boolean isRestrictedWeapon(CargoStackAPI stack)
    {
        return restrictedWeps.containsKey(stack.getData());
    }
    //</editor-fold>

    //<editor-fold desc="Heartbeat">
    public CargoAPI getCargo()
    {
        if (cargo == null)
        {
            cargo = station.getMarket().getSubmarket(
                    Constants.SUBMARKET_ID).getCargo();
        }

        return cargo;
    }

    private void heartbeat()
    {
        boolean metRequirements = true;
        CargoAPI cargo = getCargo();

        if (cargo.getTotalCrew() < settings.getRequiredCrew())
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getName()
                        + " needs " + (settings.getRequiredCrew() - cargo.getTotalCrew())
                        + " more crew to function.");
            }

            metRequirements = false;
        }

        if (cargo.getFuel() < settings.getRequiredFuelPerDay())
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getName()
                        + " is out of fuel. It requires " + settings.getRequiredFuelPerDay()
                        + " per day to function.");
            }

            metRequirements = false;
        }

        if (cargo.getSupplies() < settings.getRequiredSuppliesPerDay())
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getName()
                        + " is out of supplies. It requires " + settings.getRequiredSuppliesPerDay()
                        + " per day to function.");
            }

            metRequirements = false;
        }

        if (!metRequirements)
        {
            warnedRequirements = true;
            return;
        }

        warnedRequirements = false;
        cargo.removeSupplies(settings.getRequiredSuppliesPerDay());
        cargo.removeFuel(settings.getRequiredFuelPerDay());
        numHeartbeats++;

        List<String> addedShips = new ArrayList();
        List<String> addedWeps = new ArrayList();
        List<String> analyzedShips = new ArrayList();
        List<String> analyzedWeps = new ArrayList();
        List<String> hitLimit = new ArrayList();

        for (BaseData tmp : shipData.values())
        {
            if (!tmp.isAnalyzed())
            {
                if (numHeartbeats - tmp.getDaysToAnalyze() >= tmp.getLastUpdate())
                {
                    tmp.setAnalyzed(true);

                    if (settings.shouldShowAnalysisComplete())
                    {
                        analyzedShips.add(tmp.getName() + " ("
                                + tmp.getDaysToCreate() + "d)");
                    }
                }
            }
            else
            {
                if (numHeartbeats - tmp.getDaysToCreate() >= tmp.getLastUpdate())
                {
                    try
                    {
                        if (tmp.create())
                        {
                            if (settings.shouldShowAddedCargo())
                            {
                                addedShips.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (settings.shouldShowLimitReached() && !tmp.hasWarnedLimit())
                        {
                            hitLimit.add(tmp.getName());
                            tmp.setWarnedLimit(true);
                        }
                    }
                    catch (RuntimeException ex)
                    {
                        Global.getSector().addMessage("Failed to create ship '"
                                + tmp.getName() + "' (" + tmp.getId()
                                + ")! Was a required mod disabled?");

                        if (settings.shouldRemoveBrokenGoods())
                        {
                            Global.getSector().addMessage("Removed ship '"
                                    + tmp.getName() + "' from "
                                    + station.getName() + "'s memory banks.");
                            shipData.remove(tmp.getId());
                        }
                    }
                }
            }
        }

        for (BaseData tmp : wepData.values())
        {
            if (!tmp.isAnalyzed())
            {
                if (numHeartbeats - tmp.getDaysToAnalyze() >= tmp.getLastUpdate())
                {
                    tmp.setAnalyzed(true);

                    if (settings.shouldShowAnalysisComplete())
                    {
                        analyzedWeps.add(tmp.getName() + " ("
                                + tmp.getDaysToCreate() + "d)");
                    }
                }
            }
            else
            {
                if (numHeartbeats - tmp.getDaysToCreate() >= tmp.getLastUpdate())
                {
                    try
                    {
                        if (tmp.create())
                        {
                            if (settings.shouldShowAddedCargo())
                            {
                                addedWeps.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (settings.shouldShowLimitReached() && !tmp.hasWarnedLimit())
                        {
                            hitLimit.add(tmp.getName());
                            tmp.setWarnedLimit(true);
                        }
                    }
                    catch (RuntimeException ex)
                    {
                        Global.getSector().addMessage("Failed to create weapon '"
                                + tmp.getName() + "' (" + tmp.getId()
                                + ")! Was a required mod disabled?");

                        if (settings.shouldRemoveBrokenGoods())
                        {
                            Global.getSector().addMessage("Removed weapon '"
                                    + tmp.getName() + "' from "
                                    + station.getName() + "'s memory banks.");
                            wepData.remove(tmp.getId());
                        }
                    }
                }
            }
        }

        if (settings.shouldShowAddedCargo())
        {
            if (!addedShips.isEmpty())
            {
                Collections.sort(addedShips);
                MessageUtils.showMessage("The " + station.getName()
                        + " has produced the following ships:",
                        CollectionUtils.implode(addedShips) + ".", true);
            }
            if (!addedWeps.isEmpty())
            {
                Collections.sort(addedWeps);
                MessageUtils.showMessage("The " + station.getName()
                        + " has produced the following weapons:",
                        CollectionUtils.implode(addedWeps) + ".", true);
            }
        }

        if (settings.shouldShowLimitReached() && !hitLimit.isEmpty())
        {
            Collections.sort(hitLimit);
            MessageUtils.showMessage("The " + station.getName()
                    + " has reached its limit for the following goods:",
                    CollectionUtils.implode(hitLimit) + ".", true);
        }

        if (settings.shouldShowAnalysisComplete())
        {
            if (!analyzedShips.isEmpty())
            {
                Collections.sort(analyzedShips);
                MessageUtils.showMessage("The " + station.getName()
                        + " has started production for the following ships:",
                        CollectionUtils.implode(analyzedShips) + ".", true);
            }
            if (!analyzedWeps.isEmpty())
            {
                Collections.sort(analyzedWeps);
                MessageUtils.showMessage("The " + station.getName()
                        + " has started production for the following weapons:",
                        CollectionUtils.implode(analyzedWeps) + ".", true);
            }
        }
    }

    public boolean checkCargo()
    {
        boolean newItem = false;
        CargoAPI cargo = getCargo();
        List<String> newShips = new ArrayList();
        List<String> blockedShips = new ArrayList();
        List<String> newWeps = new ArrayList();
        List<String> blockedWeps = new ArrayList();

        for (FleetMemberAPI ship : cargo.getMothballedShips().getMembersListCopy())
        {
            if (isRestrictedShip(ship))
            {
                if (!restrictedShips.get(parseHullName(ship)))
                {
                    blockedShips.add(ship.getHullSpec().getHullName());
                    restrictedShips.put(parseHullName(ship), true);
                }
            }
            else if (isUnknownShip(ship))
            {
                newItem = true;
                String id = parseHullName(ship);
                ShipData tmp = new ShipData(ship, this);

                if (settings.getShipAnalysisTimeMod() == 0f)
                {
                    tmp.setAnalyzed(true);
                    newShips.add(tmp.getName() + " ("
                            + tmp.getDaysToCreate() + "d)");
                }
                else
                {
                    newShips.add(tmp.getName() + " ("
                            + tmp.getDaysToAnalyze() + "d)");
                }

                shipData.put(id, tmp);

                // Add all weapons on this ship to the station's cargo
                if (!ship.isFighterWing())
                {
                    for (String slot : ship.getVariant().getNonBuiltInWeaponSlots())
                    {
                        cargo.addWeapons(ship.getVariant().getWeaponId(slot), 1);
                    }
                }

                cargo.getMothballedShips().removeFleetMember(ship);
            }
        }

        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (isRestrictedWeapon(stack))
            {
                if (!restrictedWeps.get(stack.getData()))
                {
                    blockedWeps.add(stack.getDisplayName());
                    restrictedWeps.put((String) stack.getData(), true);
                }
            }
            else if (isUnknownWeapon(stack))
            {
                newItem = true;
                WeaponData tmp = new WeaponData(stack, this);

                if (settings.getWeaponAnalysisTimeMod() == 0f)
                {
                    tmp.setAnalyzed(true);
                    newWeps.add(tmp.getName() + " ("
                            + tmp.getDaysToCreate() + "d)");
                }
                else
                {
                    newWeps.add(tmp.getName() + " ("
                            + tmp.getDaysToAnalyze() + "d)");
                }

                wepData.put((String) stack.getData(), tmp);
                cargo.removeWeapons((String) stack.getData(), 1);
            }
        }

        if (!newShips.isEmpty())
        {
            Collections.sort(newShips);
            if (settings.getShipAnalysisTimeMod() == 0f)
            {
                MessageUtils.showMessage("New ship blueprints added to the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newShips) + ".", true);
            }
            else
            {
                MessageUtils.showMessage("The following ships are being"
                        + " disassembled and analyzed by the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newShips) + ".", true);
            }
        }

        if (!newWeps.isEmpty())
        {
            Collections.sort(newWeps);
            if (settings.getWeaponAnalysisTimeMod() == 0f)
            {
                MessageUtils.showMessage("New weapon blueprints added to the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newWeps) + ".", true);
            }
            else
            {
                MessageUtils.showMessage("The following weapons are being"
                        + " disassembled and analyzed by the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newWeps) + ".", true);
            }
        }

        if (!blockedShips.isEmpty())
        {
            Collections.sort(blockedShips);
            MessageUtils.showMessage("The " + station.getName()
                    + " is unable to replicate the following ships:",
                    CollectionUtils.implode(blockedShips) + ".", true);
        }

        if (!blockedWeps.isEmpty())
        {
            Collections.sort(blockedWeps);
            MessageUtils.showMessage("The " + station.getName()
                    + " is unable to replicate the following weapons:",
                    CollectionUtils.implode(blockedWeps) + ".", true);
        }

        return newItem;
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
            heartbeat();

            if (checkCargo())
            {
                warnedRequirements = false;
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Internal data types">
    private static interface BaseData
    {
        public int getDaysToAnalyze();

        public int getDaysToCreate();

        public int getLastUpdate();

        public String getName();

        public String getId();

        public int getTotal();

        public int getLimit();

        public boolean hasWarnedLimit();

        public void setWarnedLimit(boolean hasWarned);

        public boolean isAnalyzed();

        public void setAnalyzed(boolean isAnalyzed);

        public boolean create();
    }

    private static class ShipData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        FleetMemberType type;
        int fp, size, lastUpdate;
        boolean warnedLimit = false, isAnalyzed = false;

        ShipData(FleetMemberAPI ship, OmniFac factory)
        {
            fac = factory;
            id = parseHullName(ship);
            displayName = ship.getHullSpec().getHullName();
            type = ship.getType();
            fp = ship.getFleetPointCost();

            if (ship.isFighterWing())
            {
                size = 1;
            }
            else if (ship.isFrigate())
            {
                size = 2;
                //displayName += " frigate";
            }
            else if (ship.isDestroyer())
            {
                size = 3;
                //displayName += " destroyer";
            }
            else if (ship.isCruiser())
            {
                size = 4;
                //displayName += " cruiser";
            }
            else
            {
                size = 5;
                //displayName += " capital";
            }

            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public int getDaysToAnalyze()
        {
            return (int) Math.max(1f,
                    getDaysToCreate() * fac.settings.getShipAnalysisTimeMod());
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(((fp * size) / 2f) * fac.settings.getShipProductionTimeMod(),
                    size * 3f);
        }

        @Override
        public int getLastUpdate()
        {
            return lastUpdate;
        }

        @Override
        public String getName()
        {
            return displayName;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public int getTotal()
        {
            int total = 0;

            for (FleetMemberAPI tmp : fac.getCargo().getMothballedShips().getMembersListCopy())
            {
                if (id.equals(parseHullName(tmp)))
                {
                    total++;
                }
            }

            return total;
        }

        @Override
        public int getLimit()
        {
            switch (size)
            {
                case 1:
                    return fac.settings.getMaxHullsPerFighter();
                case 2:
                    return fac.settings.getMaxHullsPerFrigate();
                case 3:
                    return fac.settings.getMaxHullsPerDestroyer();
                case 4:
                    return fac.settings.getMaxHullsPerCruiser();
                case 5:
                    return fac.settings.getMaxHullsPerCapital();
                default:
                    return 0;
            }
        }

        @Override
        public boolean hasWarnedLimit()
        {
            return warnedLimit;
        }

        @Override
        public void setWarnedLimit(boolean hasWarned)
        {
            warnedLimit = hasWarned;
        }

        @Override
        public boolean isAnalyzed()
        {
            return isAnalyzed;
        }

        @Override
        public void setAnalyzed(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public boolean create()
        {
            lastUpdate = fac.numHeartbeats;

            if (getTotal() >= getLimit())
            {
                return false;
            }

            warnedLimit = false;
            fac.getCargo().addMothballedShip(type, id
                    + (type.equals(FleetMemberType.FIGHTER_WING) ? "" : "_Hull"), null);
            return true;
        }
    }

    private static class WeaponData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        float size;
        int lastUpdate, stackSize;
        boolean warnedLimit = false, isAnalyzed = false;

        WeaponData(CargoStackAPI stack, OmniFac factory)
        {
            fac = factory;
            id = (String) stack.getData();
            displayName = stack.getDisplayName();
            size = stack.getCargoSpacePerUnit();
            stackSize = (int) stack.getMaxSize();
            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public int getDaysToAnalyze()
        {
            return (int) Math.max(1f,
                    getDaysToCreate() * fac.settings.getWeaponAnalysisTimeMod());
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(size * fac.settings.getWeaponProductionTimeMod(), 1f);
        }

        @Override
        public int getLastUpdate()
        {
            return lastUpdate;
        }

        @Override
        public String getName()
        {
            return displayName;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public int getTotal()
        {
            return fac.getCargo().getNumWeapons(id);
        }

        @Override
        public int getLimit()
        {
            return (int) (stackSize * fac.settings.getMaxStacksPerWeapon());
        }

        @Override
        public boolean hasWarnedLimit()
        {
            return warnedLimit;
        }

        @Override
        public void setWarnedLimit(boolean hasWarned)
        {
            warnedLimit = hasWarned;
        }

        @Override
        public boolean isAnalyzed()
        {
            return isAnalyzed;
        }

        @Override
        public void setAnalyzed(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public boolean create()
        {
            lastUpdate = fac.numHeartbeats;

            if (getTotal() >= getLimit())
            {
                return false;
            }

            warnedLimit = false;
            fac.getCargo().addWeapons(id, 1);
            return true;
        }
    }
    //</editor-fold>
}
