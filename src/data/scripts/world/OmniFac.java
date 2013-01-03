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
import org.lazywizard.lazylib.campaign.CampaignUtils;

@SuppressWarnings("unchecked")
public class OmniFac implements SpawnPointPlugin
{
    private static final Map<SectorEntityToken, OmniFac> allFactories = new HashMap();
    private boolean SHOW_ADDED_CARGO = true;
    private boolean SHOW_LIMIT_REACHED = true;
    private boolean REMOVE_BROKEN_GOODS = false;
    private float SHIP_PRODUCTION_TIME_MOD = 1.0f;
    private float WEAPON_PRODUCTION_TIME_MOD = 1.0f;
    private int REQUIRED_CREW = 0;
    private float REQUIRED_SUPPLIES_PER_DAY = 0f;
    private float REQUIRED_FUEL_PER_DAY = 0f;
    private int MAX_HULLS_PER_FIGHTER = 6;
    private int MAX_HULLS_PER_FRIGATE = 5;
    private int MAX_HULLS_PER_DESTROYER = 4;
    private int MAX_HULLS_PER_CRUISER = 3;
    private int MAX_HULLS_PER_CAPITAL = 2;
    private float MAX_STACKS_PER_WEAPON = 1.0f;
    private Map<String, ShipData> shipData = new HashMap();
    private Map<String, WeaponData> wepData = new HashMap();
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

        return (OmniFac) allFactories.get(station);
    }

    public static List<SectorEntityToken> getFactories()
    {
        return new ArrayList<SectorEntityToken>(allFactories.keySet());
    }

    public static String parseHullName(FleetMemberAPI ship)
    {
        if (ship.isFighterWing())
        {
            return ship.getSpecId();
        }

        int lastIndex = ship.getSpecId().lastIndexOf("_");

        if (lastIndex > 0)
        {
            return ship.getSpecId().substring(0, lastIndex);
        }

        return ship.getSpecId();
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

    public void setRemoveBrokenGoods(boolean removeBrokenGoods)
    {
        REMOVE_BROKEN_GOODS = removeBrokenGoods;
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
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Stack/ship analysis">
    public boolean isUnknownWeapon(CargoStackAPI stack)
    {
        if (!stack.isWeaponStack())
        {
            // We only deal with weapons, not resources
            return false;
        }

        if (wepData.containsKey((String) stack.getData()))
        {
            // Already queued for construction
            return false;
        }

        return true;
    }

    public boolean isUnknownShip(FleetMemberAPI ship)
    {
        if (shipData.containsKey(parseHullName(ship)))
        {
            // Already queued for construction
            return false;
        }

        return true;
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

        StringBuilder reachedLimit = new StringBuilder();
        StringBuilder created = new StringBuilder();

        for (BaseData tmp : shipData.values())
        {
            if (numHeartbeats % tmp.getDaysToCreate() == tmp.getDaysOffset())
            {
                try
                {
                    if (tmp.create())
                    {
                        if (created.length() > 0)
                        {
                            created.append(", ");
                        }

                        created.append(tmp.getName()).append(" (").
                                append(tmp.getTotal()).append("/").
                                append(tmp.getLimit()).append(")");
                    }
                    else
                    {
                        if (reachedLimit.length() > 0)
                        {
                            reachedLimit.append(", ");
                        }

                        reachedLimit.append(tmp.getName());
                    }
                }
                catch (RuntimeException ex)
                {
                    Global.getSector().addMessage("Failed to create ship '"
                            + tmp.getName() + "'! Was a required mod disabled?");

                    if (REMOVE_BROKEN_GOODS)
                    {
                        Global.getSector().addMessage("Removing ship '" + tmp.getName()
                                + "' from " + station.getFullName() + "'s memory banks...");
                        shipData.remove(tmp.getId());
                    }
                }
            }
        }

        for (BaseData tmp : wepData.values())
        {
            if (numHeartbeats % tmp.getDaysToCreate() == tmp.getDaysOffset())
            {
                try
                {
                    if (tmp.create())
                    {
                        if (created.length() > 0)
                        {
                            created.append(", ");
                        }

                        created.append(tmp.getName()).append(" (").
                                append(tmp.getTotal()).append("/").
                                append(tmp.getLimit()).append(")");
                    }
                    else
                    {
                        if (reachedLimit.length() > 0)
                        {
                            reachedLimit.append(", ");
                        }

                        reachedLimit.append(tmp.getName());
                    }
                }
                catch (RuntimeException ex)
                {
                    Global.getSector().addMessage("Failed to create weapon '"
                            + tmp.getName() + "'! Was a required mod disabled?");

                    if (REMOVE_BROKEN_GOODS)
                    {
                        Global.getSector().addMessage("Removing weapon '" + tmp.getName()
                                + "' from " + station.getFullName() + "'s memory banks.,,");
                        wepData.remove(tmp.getId());
                    }
                }
            }
        }

        if (SHOW_ADDED_CARGO && created.length() > 0)
        {
            CampaignUtils.showMessage("The " + station.getFullName()
                    + " has produced the following goods:",
                    created.toString() + ".", true);
        }
        if (SHOW_LIMIT_REACHED && reachedLimit.length() > 0)
        {
            CampaignUtils.showMessage("The " + station.getFullName()
                    + " has reached its limit for the following goods:",
                    reachedLimit.toString() + ".", true);
        }
    }

    public boolean checkCargo()
    {
        boolean newItem = false;
        CargoAPI cargo = station.getCargo();

        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (isUnknownWeapon(stack))
            {
                newItem = true;
                WeaponData tmp = new WeaponData(stack, this);
                wepData.put((String) stack.getData(), tmp);
                Global.getSector().addMessage("A new " + tmp.getName()
                        + " will be produced at the " + station.getFullName()
                        + " every " + tmp.getDaysToCreate() + " days.");
                cargo.removeWeapons((String) stack.getData(), 1);
            }
        }

        for (FleetMemberAPI ship : cargo.getMothballedShips().getMembersListCopy())
        {
            if (isUnknownShip(ship))
            {
                newItem = true;
                String id = parseHullName(ship);
                ShipData tmp = new ShipData(ship, this);
                shipData.put(id, tmp);
                Global.getSector().addMessage("A new " + tmp.getName()
                        + " will be produced at the " + station.getFullName()
                        + " every " + tmp.getDaysToCreate() + " days.");
                cargo.getMothballedShips().removeFleetMember(ship);
            }
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
        public int getDaysToCreate();

        public int getDaysOffset();

        public String getName();

        public String getId();

        public int getTotal();

        public int getLimit();

        public boolean create();
    }

    private static final class ShipData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        FleetMemberType type;
        int fp, size, daysOffset;

        public ShipData(FleetMemberAPI ship, OmniFac factory)
        {
            fac = factory;
            id = parseHullName(ship);
            displayName = Character.toUpperCase(ship.getSpecId().charAt(0))
                    + parseHullName(ship).substring(1).replace('_', ' ');
            type = ship.getType();
            fp = ship.getFleetPointCost();

            if (ship.isFighterWing())
            {
                size = 1;
            }
            else if (ship.isFrigate())
            {
                size = 2;
            }
            else if (ship.isDestroyer())
            {
                size = 3;
            }
            else if (ship.isCruiser())
            {
                size = 4;
            }
            else if (ship.isCapital())
            {
                size = 5;
            }

            daysOffset = fac.numHeartbeats % getDaysToCreate();
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
        public boolean create()
        {
            if (getTotal() >= getLimit())
            {
                daysOffset = fac.numHeartbeats % getDaysToCreate();
                return false;
            }

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

        public WeaponData(CargoStackAPI stack, OmniFac factory)
        {
            fac = factory;
            id = (String) stack.getData();
            displayName = stack.getDisplayName();
            size = stack.getCargoSpacePerUnit();
            stackSize = (int) stack.getMaxSize();
            daysOffset = fac.numHeartbeats % getDaysToCreate();
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
        public boolean create()
        {
            if (getTotal() >= getLimit())
            {
                daysOffset = fac.numHeartbeats % getDaysToCreate();
                return false;
            }

            fac.station.getCargo().addWeapons(id, 1);
            return true;
        }
    }
    //</editor-fold>
}
