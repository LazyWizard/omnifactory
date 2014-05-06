package data.scripts.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.MessageUtils;

@SuppressWarnings("unchecked")
public class OmniFac implements EveryFrameScript
{
    private static final String FACTORY_DATA_ID = "lw_omnifac_allfactories";
    private String settingsFile;
    private transient OmniFacSettings settings;
    // TODO: These are only here for backwards compatibility, remove after next SS update
    private transient boolean SHOW_ADDED_CARGO, SHOW_ANALYSIS_COMPLETE,
            SHOW_LIMIT_REACHED, REMOVE_BROKEN_GOODS;
    private transient float SHIP_ANALYSIS_TIME_MOD, WEAPON_ANALYSIS_TIME_MOD,
            SHIP_PRODUCTION_TIME_MOD, WEAPON_PRODUCTION_TIME_MOD;
    private transient int REQUIRED_CREW = 0;
    private transient float REQUIRED_SUPPLIES_PER_DAY, REQUIRED_FUEL_PER_DAY;
    private transient int MAX_HULLS_PER_FIGHTER, MAX_HULLS_PER_FRIGATE,
            MAX_HULLS_PER_DESTROYER, MAX_HULLS_PER_CRUISER, MAX_HULLS_PER_CAPITAL;
    private transient float MAX_STACKS_PER_WEAPON;
    private final Map<String, ShipData> shipData = new HashMap<>();
    private final Map<String, WeaponData> wepData = new HashMap<>();
    private final Map<String, Boolean> restrictedShips = new HashMap<>();
    private final Map<String, Boolean> restrictedWeps = new HashMap<>();
    private final SectorEntityToken station;
    private long lastHeartbeat;
    private int numHeartbeats = 0;
    private boolean warnedRequirements = true;

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    public OmniFac(SectorEntityToken station, String settingsFile)
    {
        this.station = station;
        lastHeartbeat = Global.getSector().getClock().getTimestamp();
        settings = new OmniFacSettings(settingsFile);
        getFactoryMap().put(station, this);
    }

    public Object readResolve()
    {
        if (settingsFile == null)
        {
            settingsFile = "data/config/omnifac_settings.json";
        }

        settings = new OmniFacSettings(settingsFile);
        return this;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Static methods">
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
        Map<SectorEntityToken, OmniFac> allFactories = (HashMap) Global.getSector()
                .getPersistentData().get(FACTORY_DATA_ID);

        if (allFactories == null)
        {
            allFactories = new HashMap<>();
            Global.getSector().getPersistentData()
                    .put(FACTORY_DATA_ID, allFactories);
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

    //<editor-fold defaultstate="collapsed" desc="Factory settings">
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

        if (cargo.getTotalCrew() < settings.REQUIRED_CREW)
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getName()
                        + " needs " + (settings.REQUIRED_CREW - cargo.getTotalCrew())
                        + " more crew to function.");
            }

            metRequirements = false;
        }

        if (cargo.getFuel() < settings.REQUIRED_FUEL_PER_DAY)
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getName()
                        + " is out of fuel. It requires " + settings.REQUIRED_FUEL_PER_DAY
                        + " per day to function.");
            }

            metRequirements = false;
        }

        if (cargo.getSupplies() < settings.REQUIRED_SUPPLIES_PER_DAY)
        {
            if (!warnedRequirements)
            {
                Global.getSector().addMessage("The " + station.getName()
                        + " is out of supplies. It requires " + settings.REQUIRED_SUPPLIES_PER_DAY
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
        cargo.removeSupplies(settings.REQUIRED_SUPPLIES_PER_DAY);
        cargo.removeFuel(settings.REQUIRED_FUEL_PER_DAY);
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

                    if (settings.SHOW_ANALYSIS_COMPLETE)
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
                            if (settings.SHOW_ADDED_CARGO)
                            {
                                addedShips.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (settings.SHOW_LIMIT_REACHED && !tmp.hasWarnedLimit())
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

                        if (settings.REMOVE_BROKEN_GOODS)
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
                if (numHeartbeats % tmp.getDaysToAnalyze() == tmp.getDaysOffset())
                {
                    tmp.setAnalyzed(true);

                    if (settings.SHOW_ANALYSIS_COMPLETE)
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
                            if (settings.SHOW_ADDED_CARGO)
                            {
                                addedWeps.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (settings.SHOW_LIMIT_REACHED && !tmp.hasWarnedLimit())
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

                        if (settings.REMOVE_BROKEN_GOODS)
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

        if (settings.SHOW_ADDED_CARGO)
        {
            if (!addedShips.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getName()
                        + " has produced the following ships:",
                        CollectionUtils.implode(addedShips) + ".", true);
            }
            if (!addedWeps.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getName()
                        + " has produced the following weapons:",
                        CollectionUtils.implode(addedWeps) + ".", true);
            }
        }

        if (settings.SHOW_LIMIT_REACHED && !hitLimit.isEmpty())
        {
            MessageUtils.showMessage("The " + station.getName()
                    + " has reached its limit for the following goods:",
                    CollectionUtils.implode(hitLimit) + ".", true);
        }

        if (settings.SHOW_ANALYSIS_COMPLETE)
        {
            if (!analyzedShips.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getName()
                        + " has started production for the following ships:",
                        CollectionUtils.implode(analyzedShips) + ".", true);
            }
            if (!analyzedWeps.isEmpty())
            {
                MessageUtils.showMessage("The " + station.getName()
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
                    blockedShips.add(ship.getHullSpec().getHullName());
                    restrictedShips.put(parseHullName(ship), true);
                }
            }
            else if (isUnknownShip(ship))
            {
                newItem = true;
                String id = parseHullName(ship);
                ShipData tmp = new ShipData(ship, this);

                if (settings.SHIP_ANALYSIS_TIME_MOD == 0f)
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

                if (settings.WEAPON_ANALYSIS_TIME_MOD == 0f)
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
            if (settings.SHIP_ANALYSIS_TIME_MOD == 0f)
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
            if (settings.WEAPON_ANALYSIS_TIME_MOD == 0f)
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
            MessageUtils.showMessage("The " + station.getName()
                    + " is unable to replicate the following ships:",
                    CollectionUtils.implode(blockedShips) + ".", true);
        }

        if (!blockedWeps.isEmpty())
        {
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

            daysOffset = fac.numHeartbeats % getDaysToAnalyze();
        }

        @Override
        public int getDaysToAnalyze()
        {
            return (int) Math.max(1f,
                    getDaysToCreate() * fac.settings.SHIP_ANALYSIS_TIME_MOD);
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(((fp * size) / 2f) * fac.settings.SHIP_PRODUCTION_TIME_MOD,
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
                    return fac.settings.MAX_HULLS_PER_FIGHTER;
                case 2:
                    return fac.settings.MAX_HULLS_PER_FRIGATE;
                case 3:
                    return fac.settings.MAX_HULLS_PER_DESTROYER;
                case 4:
                    return fac.settings.MAX_HULLS_PER_CRUISER;
                case 5:
                    return fac.settings.MAX_HULLS_PER_CAPITAL;
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
            return (int) Math.max(1f,
                    getDaysToCreate() * fac.settings.WEAPON_ANALYSIS_TIME_MOD);
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(size * fac.settings.WEAPON_PRODUCTION_TIME_MOD, 1f);
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
            return (int) (stackSize * fac.settings.MAX_STACKS_PER_WEAPON);
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
