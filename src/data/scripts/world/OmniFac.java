package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpawnPointPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import java.util.*;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.MessageUtils;

@SuppressWarnings("unchecked")
public class OmniFac implements SpawnPointPlugin
{
    private static final Map<SectorEntityToken, OmniFac> allFactories = new HashMap();
    private boolean SHOW_ADDED_CARGO = false;
    private boolean SHOW_ANALYSIS_COMPLETE = true;
    private boolean SHOW_LIMIT_REACHED = true;
    private boolean REMOVE_BROKEN_GOODS = false;
    private float SHIP_ANALYSIS_TIME_MOD = 0.5f;
    private float WEAPON_ANALYSIS_TIME_MOD = 0.5f;
    private float SHIP_PRODUCTION_TIME_MOD = 1.0f;
    private float WEAPON_PRODUCTION_TIME_MOD = 1.0f;
    private int REQUIRED_CREW = 0;
    private float REQUIRED_SUPPLIES_PER_DAY = 0f;
    private float REQUIRED_FUEL_PER_DAY = 0f;
    private int MAX_HULLS_PER_FIGHTER = 3;
    private int MAX_HULLS_PER_FRIGATE = 3;
    private int MAX_HULLS_PER_DESTROYER = 2;
    private int MAX_HULLS_PER_CRUISER = 2;
    private int MAX_HULLS_PER_CAPITAL = 1;
    private float MAX_STACKS_PER_WEAPON = 0.5f;
    private Map<String, ShipData> shipData = new HashMap();
    private Map<String, WeaponData> wepData = new HashMap();
    private Map<String, Boolean> restrictedShips = new HashMap();
    private Map<String, Boolean> restrictedWeps = new HashMap();
    private SectorEntityToken station;
    private long lastHeartbeat;
    private int numHeartbeats = 0;
    private boolean warnedRequirements = true;

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    public OmniFac(SectorEntityToken station)
    {
        this.station = station;
        lastHeartbeat = Global.getSector().getClock().getTimestamp();
        allFactories.put(station, this);
    }

    public Object readResolve()
    {
        allFactories.put(this.station, this);
        return this;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Static methods">
    public static boolean isFactory(SectorEntityToken station)
    {
        return allFactories.keySet().contains(station);
    }

    public static OmniFac getFactory(SectorEntityToken station)
    {
        if (!isFactory(station))
        {
            return null;
        }

        return allFactories.get(station);
    }

    public static List<SectorEntityToken> getFactories()
    {
        return new ArrayList(allFactories.keySet());
    }

    public static String parseHullName(FleetMemberAPI ship)
    {
        if (ship.isFighterWing())
        {
            return ship.getSpecId();
        }

        return ship.getHullId();
    }

    public static String getDisplayName(FleetMemberAPI ship)
    {
        String displayName = parseHullName(ship);
        displayName = Character.toUpperCase(displayName.charAt(0))
                + displayName.substring(1).replace('_', ' ');
        return displayName;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Factory settings">
    public void setShowAddedCargo(boolean showAddedCargo)
    {
        SHOW_ADDED_CARGO = showAddedCargo;
    }

    public void setShowLimitReached(boolean showLimitReached)
    {
        SHOW_LIMIT_REACHED = showLimitReached;
    }

    public void setShowAnalysisComplete(boolean showAnalysisComplete)
    {
        SHOW_ANALYSIS_COMPLETE = showAnalysisComplete;
    }

    public void setRemoveBrokenGoods(boolean removeBrokenGoods)
    {
        REMOVE_BROKEN_GOODS = removeBrokenGoods;
    }

    public void setShipAnalysisTimeModifier(float modifier)
    {
        SHIP_ANALYSIS_TIME_MOD = modifier;
    }

    public void setWeaponAnalysisTimeModifier(float modifier)
    {
        WEAPON_ANALYSIS_TIME_MOD = modifier;
    }

    public void setShipProductionTimeModifier(float modifier)
    {
        SHIP_PRODUCTION_TIME_MOD = modifier;
    }

    public void setWeaponProductionTimeModifier(float modifier)
    {
        WEAPON_PRODUCTION_TIME_MOD = modifier;
    }

    public void setRequiredCrew(int requiredCrew)
    {
        REQUIRED_CREW = requiredCrew;
    }

    public void setRequiredSuppliesPerDay(float suppliesPerDay)
    {
        REQUIRED_SUPPLIES_PER_DAY = suppliesPerDay;
    }

    public void setRequiredFuelPerDay(float fuelPerDay)
    {
        REQUIRED_FUEL_PER_DAY = fuelPerDay;
    }

    public void setMaxHullsPerFighter(int maxHulls)
    {
        MAX_HULLS_PER_FIGHTER = maxHulls;
    }

    public void setMaxHullsPerFrigate(int maxHulls)
    {
        MAX_HULLS_PER_FRIGATE = maxHulls;
    }

    public void setMaxHullsPerDestroyer(int maxHulls)
    {
        MAX_HULLS_PER_DESTROYER = maxHulls;
    }

    public void setMaxHullsPerCruiser(int maxHulls)
    {
        MAX_HULLS_PER_CRUISER = maxHulls;
    }

    public void setMaxHullsPerCapital(int maxHulls)
    {
        MAX_HULLS_PER_CAPITAL = maxHulls;
    }

    public void setMaxStacksPerWeapon(float maxStacks)
    {
        MAX_STACKS_PER_WEAPON = maxStacks;
    }

    public boolean addRestrictedShip(String hullId)
    {
        if (hullId.endsWith("_Hull"))
        {
            hullId = hullId.substring(0, hullId.lastIndexOf("_Hull"));
        }

        return (restrictedShips.put(hullId, false) != null);
    }

    public boolean addRestrictedWeapon(String weaponId)
    {
        return (restrictedWeps.put(weaponId, false) != null);
    }

    public boolean removeRestrictedShip(String hullId)
    {
        if (hullId.endsWith("_Hull"))
        {
            hullId = hullId.substring(0, hullId.lastIndexOf("_Hull"));
        }

        return (restrictedShips.remove(hullId) != null);
    }

    public boolean removeRestrictedWeapon(String weaponId)
    {
        return (restrictedWeps.remove(weaponId) != null);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Stack/ship analysis">
    public boolean isUnknownShip(FleetMemberAPI ship)
    {
        String id = parseHullName(ship);
        if (shipData.containsKey(id))
        {
            // Already queued for construction
            return false;
        }

        return true;
    }

    public boolean isUnknownWeapon(CargoStackAPI stack)
    {
        if (!stack.isWeaponStack())
        {
            // We only deal with weapons, not resources
            return false;
        }

        String id = (String) stack.getData();
        if (wepData.containsKey(id))
        {
            // Already queued for construction
            return false;
        }

        return true;
    }

    public boolean isRestrictedShip(FleetMemberAPI ship)
    {
        return restrictedShips.containsKey(parseHullName(ship));
    }

    public boolean isRestrictedWeapon(CargoStackAPI stack)
    {
        return restrictedWeps.containsKey((String) stack.getData());
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Heartbeat">
    private void heartbeat()
    {
        boolean metRequirements = true;
        CargoAPI cargo = station.getCargo();

        if (cargo.getTotalCrew() < REQUIRED_CREW)
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getFullName()
                        + " needs " + (REQUIRED_CREW - cargo.getTotalCrew())
                        + " more crew to function.");
            }

            metRequirements = false;
        }

        if (cargo.getFuel() < REQUIRED_FUEL_PER_DAY)
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getFullName()
                        + " is out of fuel. It requires " + REQUIRED_FUEL_PER_DAY
                        + " per day to function.");
            }

            metRequirements = false;
        }

        if (cargo.getSupplies() < REQUIRED_SUPPLIES_PER_DAY)
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getFullName()
                        + " is out of supplies. It requires " + REQUIRED_SUPPLIES_PER_DAY
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
        cargo.removeSupplies(REQUIRED_SUPPLIES_PER_DAY);
        cargo.removeFuel(REQUIRED_FUEL_PER_DAY);
        numHeartbeats++;

        SortedSet<String> addedShips = new TreeSet();
        SortedSet<String> addedWeps = new TreeSet();
        SortedSet<String> analyzedShips = new TreeSet();
        SortedSet<String> analyzedWeps = new TreeSet();
        SortedSet<String> hitLimit = new TreeSet();

        for (BaseData tmp : shipData.values())
        {
            if (!tmp.isAnalyzed())
            {
                if (numHeartbeats % tmp.getDaysToAnalyze() == tmp.getDaysOffset())
                {
                    tmp.setAnalyzed(true);

                    if (SHOW_ANALYSIS_COMPLETE)
                    {
                        analyzedShips.add(tmp.getName() + " ("
                                + tmp.getDaysToCreate() + "d)");
                    }
                }
            }
            else
            {
                if (numHeartbeats % tmp.getDaysToCreate() == tmp.getDaysOffset())
                {
                    try
                    {
                        if (tmp.create())
                        {
                            if (SHOW_ADDED_CARGO)
                            {
                                addedShips.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (SHOW_LIMIT_REACHED && !tmp.hasWarnedLimit())
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

                        if (REMOVE_BROKEN_GOODS)
                        {
                            Global.getSector().addMessage("Removed ship '"
                                    + tmp.getName() + "' from "
                                    + station.getFullName() + "'s memory banks.");
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
                if (numHeartbeats % tmp.getDaysToAnalyze() == tmp.getDaysOffset())
                {
                    tmp.setAnalyzed(true);

                    if (SHOW_ANALYSIS_COMPLETE)
                    {
                        analyzedWeps.add(tmp.getName() + " ("
                                + tmp.getDaysToCreate() + "d)");
                    }
                }
            }
            else
            {
                if (numHeartbeats % tmp.getDaysToCreate() == tmp.getDaysOffset())
                {
                    try
                    {
                        if (tmp.create())
                        {
                            if (SHOW_ADDED_CARGO)
                            {
                                addedWeps.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (SHOW_LIMIT_REACHED && !tmp.hasWarnedLimit())
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

                        if (REMOVE_BROKEN_GOODS)
                        {
                            Global.getSector().addMessage("Removed weapon '"
                                    + tmp.getName() + "' from "
                                    + station.getFullName() + "'s memory banks.");
                            wepData.remove(tmp.getId());
                        }
                    }
                }
            }
        }

        if (SHOW_ADDED_CARGO)
        {
            if (!addedShips.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getFullName()
                        + " has produced the following ships:",
                        CollectionUtils.implode(addedShips) + ".", true);
            }
            if (!addedWeps.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getFullName()
                        + " has produced the following weapons:",
                        CollectionUtils.implode(addedWeps) + ".", true);
            }
        }

        if (SHOW_LIMIT_REACHED && !hitLimit.isEmpty())
        {
            MessageUtils.showMessage("The " + station.getFullName()
                    + " has reached its limit for the following goods:",
                    CollectionUtils.implode(hitLimit) + ".", true);
        }

        if (SHOW_ANALYSIS_COMPLETE)
        {
            if (!analyzedShips.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getFullName()
                        + " has started production for the following ships:",
                        CollectionUtils.implode(analyzedShips) + ".", true);
            }
            if (!analyzedWeps.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getFullName()
                        + " has started production for the following weapons:",
                        CollectionUtils.implode(analyzedWeps) + ".", true);
            }
        }
    }

    public boolean checkCargo()
    {
        boolean newItem = false;
        CargoAPI cargo = station.getCargo();
        SortedSet<String> newShips = new TreeSet();
        SortedSet<String> blockedShips = new TreeSet();
        SortedSet<String> newWeps = new TreeSet();
        SortedSet<String> blockedWeps = new TreeSet();

        for (FleetMemberAPI ship : cargo.getMothballedShips().getMembersListCopy())
        {
            if (isRestrictedShip(ship))
            {
                if (!restrictedShips.get(parseHullName(ship)))
                {
                    blockedShips.add(getDisplayName(ship));
                    restrictedShips.put(parseHullName(ship), true);
                }
            }
            else if (isUnknownShip(ship))
            {
                newItem = true;
                String id = parseHullName(ship);
                ShipData tmp = new ShipData(ship, this);

                if (SHIP_ANALYSIS_TIME_MOD == 0f)
                {
                    tmp.setAnalyzed(true);
                    newShips.add(tmp.getName() + " ("
                            + tmp.getDaysToAnalyze() + "d)");
                }
                else
                {
                    newShips.add(tmp.getName() + " ("
                            + tmp.getDaysToCreate() + "d)");
                }

                shipData.put(id, tmp);
                cargo.getMothballedShips().removeFleetMember(ship);
            }
        }

        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (isRestrictedWeapon(stack))
            {
                if (!restrictedWeps.get((String) stack.getData()))
                {
                    blockedWeps.add(stack.getDisplayName());
                    restrictedWeps.put((String) stack.getData(), true);
                }
            }
            else if (isUnknownWeapon(stack))
            {
                newItem = true;
                WeaponData tmp = new WeaponData(stack, this);

                if (WEAPON_ANALYSIS_TIME_MOD == 0f)
                {
                    tmp.setAnalyzed(true);
                    newWeps.add(tmp.getName() + " ("
                            + tmp.getDaysToAnalyze() + "d)");
                }
                else
                {
                    newWeps.add(tmp.getName() + " ("
                            + tmp.getDaysToCreate() + "d)");
                }

                wepData.put((String) stack.getData(), tmp);
                cargo.removeWeapons((String) stack.getData(), 1);
            }
        }

        if (!newShips.isEmpty())
        {
            if (SHIP_ANALYSIS_TIME_MOD == 0f)
            {
                MessageUtils.showMessage("New ship blueprints added to the "
                        + station.getFullName() + ":",
                        CollectionUtils.implode(newShips) + ".", true);
            }
            else
            {
                MessageUtils.showMessage("The following ships are being"
                        + " disassembled and analyzed by the "
                        + station.getFullName() + ":",
                        CollectionUtils.implode(newShips) + ".", true);
            }
        }

        if (!newWeps.isEmpty())
        {
            if (WEAPON_ANALYSIS_TIME_MOD == 0f)
            {
                MessageUtils.showMessage("New weapon blueprints added to the "
                        + station.getFullName() + ":",
                        CollectionUtils.implode(newWeps) + ".", true);
            }
            else
            {
                MessageUtils.showMessage("The following weapons are being"
                        + " disassembled and analyzed by the "
                        + station.getFullName() + ":",
                        CollectionUtils.implode(newWeps) + ".", true);
            }
        }

        if (!blockedShips.isEmpty())
        {
            MessageUtils.showMessage("The " + station.getFullName()
                    + " is unable to replicate the following ships:",
                    CollectionUtils.implode(blockedShips) + ".", true);
        }

        if (!blockedWeps.isEmpty())
        {
            MessageUtils.showMessage("The " + station.getFullName()
                    + " is unable to replicate the following weapons:",
                    CollectionUtils.implode(blockedWeps) + ".", true);
        }

        return newItem;
    }

    @Override
    public void advance(SectorAPI sector, LocationAPI location)
    {
        CampaignClockAPI clock = sector.getClock();

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

    //<editor-fold defaultstate="collapsed" desc="Internal data types">
    private static interface BaseData
    {
        public int getDaysToAnalyze();

        public int getDaysToCreate();

        public int getDaysOffset();

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

    private static final class ShipData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        FleetMemberType type;
        int fp, size, daysOffset;
        boolean warnedLimit = false, isAnalyzed = false;

        public ShipData(FleetMemberAPI ship, OmniFac factory)
        {
            fac = factory;
            id = parseHullName(ship);
            displayName = getDisplayName(ship);
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

            daysOffset = fac.numHeartbeats % getDaysToAnalyze();
        }

        @Override
        public int getDaysToAnalyze()
        {
            return (int) (getDaysToCreate() * fac.SHIP_ANALYSIS_TIME_MOD);
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(((fp * size) / 2f) * fac.SHIP_PRODUCTION_TIME_MOD,
                    size * 3f);
        }

        @Override
        public int getDaysOffset()
        {
            return daysOffset;
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

            for (FleetMemberAPI tmp : fac.station.getCargo().getMothballedShips().getMembersListCopy())
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
                    return fac.MAX_HULLS_PER_FIGHTER;
                case 2:
                    return fac.MAX_HULLS_PER_FRIGATE;
                case 3:
                    return fac.MAX_HULLS_PER_DESTROYER;
                case 4:
                    return fac.MAX_HULLS_PER_CRUISER;
                case 5:
                    return fac.MAX_HULLS_PER_CAPITAL;
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

            if (isAnalyzed)
            {
                daysOffset = fac.numHeartbeats % getDaysToCreate();
            }
            else
            {
                daysOffset = fac.numHeartbeats % getDaysToAnalyze();
            }
        }

        @Override
        public boolean create()
        {
            if (getTotal() >= getLimit())
            {
                daysOffset = fac.numHeartbeats % getDaysToCreate();
                return false;
            }

            warnedLimit = false;
            fac.station.getCargo().addMothballedShip(type, id
                    + (type.equals(FleetMemberType.FIGHTER_WING) ? "" : "_Hull"), null);
            return true;
        }
    }

    private static final class WeaponData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        float size;
        int daysOffset, stackSize;
        boolean warnedLimit = false, isAnalyzed = false;

        public WeaponData(CargoStackAPI stack, OmniFac factory)
        {
            fac = factory;
            id = (String) stack.getData();
            displayName = stack.getDisplayName();
            size = stack.getCargoSpacePerUnit();
            stackSize = (int) stack.getMaxSize();
            daysOffset = fac.numHeartbeats % getDaysToAnalyze();
        }

        @Override
        public int getDaysToAnalyze()
        {
            return (int) (getDaysToCreate() * fac.WEAPON_ANALYSIS_TIME_MOD);
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(size * fac.WEAPON_PRODUCTION_TIME_MOD, 1f);
        }

        @Override
        public int getDaysOffset()
        {
            return daysOffset;
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
            return fac.station.getCargo().getNumWeapons(id);
        }

        @Override
        public int getLimit()
        {
            return (int) (stackSize * fac.MAX_STACKS_PER_WEAPON);
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

            if (isAnalyzed)
            {
                daysOffset = fac.numHeartbeats % getDaysToCreate();
            }
            else
            {
                daysOffset = fac.numHeartbeats % getDaysToAnalyze();
            }
        }

        @Override
        public boolean create()
        {
            if (getTotal() >= getLimit())
            {
                daysOffset = fac.numHeartbeats % getDaysToCreate();
                return false;
            }

            warnedLimit = false;
            fac.station.getCargo().addWeapons(id, 1);
            return true;
        }
    }
    //</editor-fold>
}
