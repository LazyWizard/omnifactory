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

@SuppressWarnings("unchecked")
public class OmniFac implements SpawnPointPlugin
{
    private static final boolean SHOW_ADDED_CARGO = true;
    private static final boolean SHOW_LIMIT_REACHED = true;
    private SectorEntityToken station;
    private long lastHeartbeat;
    private int numHeartbeats = 0;
    private Map shipData = new HashMap(); // <String, ShipData>
    private Map wepData = new HashMap(); // <String, WeaponData>

    public OmniFac(SectorEntityToken station)
    {
        this.station = station;
        lastHeartbeat = Global.getSector().getClock().getTimestamp();
    }

    private void heartbeat()
    {
        numHeartbeats++;

        BaseData tmp;
        for (Iterator ships = shipData.values().iterator(); ships.hasNext();)
        {
            tmp = (BaseData) ships.next();

            if (numHeartbeats % tmp.getDaysToCreate() == tmp.getDaysOffset())
            {
                tmp.create();
            }
        }

        for (Iterator weps = wepData.values().iterator(); weps.hasNext();)
        {
            tmp = (BaseData) weps.next();

            if (numHeartbeats % tmp.getDaysToCreate() == tmp.getDaysOffset())
            {
                tmp.create();
            }
        }
    }

    private void checkCargo()
    {
        CargoAPI cargo = station.getCargo();
        CargoStackAPI stack;
        FleetMemberAPI ship;

        for (Iterator stacks = cargo.getStacksCopy().iterator();
                stacks.hasNext();)
        {
            stack = (CargoStackAPI) stacks.next();

            if (analyseStack(stack))
            {
                cargo.removeWeapons((String) stack.getData(), 1);
            }
        }

        for (Iterator ships = cargo.getMothballedShips().getMembersListCopy().iterator();
                ships.hasNext();)
        {
            ship = (FleetMemberAPI) ships.next();

            if (analyseShip(ship))
            {
                cargo.getMothballedShips().removeFleetMember(ship);
            }
        }
    }

    private boolean analyseStack(CargoStackAPI stack)
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

        WeaponData tmp = new WeaponData(stack, this);
        wepData.put((String) stack.getData(), tmp);
        Global.getSector().addMessage("A new " + tmp.getName()
                + " will be produced at the " + station.getFullName()
                + " every " + tmp.getDaysToCreate() + " days.");
        return true;
    }

    private boolean analyseShip(FleetMemberAPI ship)
    {
        String id = parseHullName(ship);

        if (shipData.containsKey(id))
        {
            // Already queued for construction
            return false;
        }

        ShipData tmp = new ShipData(ship, this);
        shipData.put(id, tmp);
        Global.getSector().addMessage("A new " + tmp.getName()
                + " will be produced at the " + station.getFullName()
                + " every " + tmp.getDaysToCreate() + " days.");
        return true;
    }

    private static String parseHullName(FleetMemberAPI ship)
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

    @Override
    public void advance(SectorAPI sector, LocationAPI location)
    {
        CampaignClockAPI clock = sector.getClock();

        if (clock.getElapsedDaysSince(lastHeartbeat) >= 1f)
        {
            lastHeartbeat = clock.getTimestamp();
            heartbeat();
            checkCargo();
        }
    }

    private static interface BaseData
    {
        public int getDaysToCreate();

        public int getDaysOffset();

        public String getName();

        public int getLimit();

        public void create();
    }

    private static final class ShipData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        FleetMemberType type;
        int fp, size, daysOffset, limit;
        boolean warnedLimit = false;

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
                limit = 6;
            }
            else if (ship.isFrigate())
            {
                size = 2;
                limit = 5;
            }
            else if (ship.isDestroyer())
            {
                size = 3;
                limit = 4;
            }
            else if (ship.isCruiser())
            {
                size = 4;
                limit = 3;
            }
            else if (ship.isCapital())
            {
                size = 5;
                limit = 2;
            }

            daysOffset = fac.numHeartbeats % getDaysToCreate();
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max((fp * size) / 2f, size * 3f);
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
        public int getLimit()
        {
            return limit;
        }

        @Override
        public void create()
        {
            int total = 0;
            FleetMemberAPI tmp;
            for (Iterator ships = fac.station.getCargo().getMothballedShips().getMembersListCopy().iterator();
                    ships.hasNext();)
            {
                tmp = (FleetMemberAPI) ships.next();

                if (id.equals(parseHullName(tmp)))
                {
                    total++;
                }
            }

            if (total >= limit)
            {
                if (SHOW_LIMIT_REACHED && !warnedLimit)
                {
                    warnedLimit = true;
                    Global.getSector().addMessage("Limit reached for " + displayName
                            + " at " + fac.station.getFullName() + ".");
                }

                daysOffset = fac.numHeartbeats % getDaysToCreate();
                return;
            }

            total++;
            warnedLimit = false;
            fac.station.getCargo().addMothballedShip(type, id
                    + (type.equals(FleetMemberType.FIGHTER_WING) ? "" : "_Hull"), null);

            if (SHOW_ADDED_CARGO)
            {
                Global.getSector().addMessage("Added " + displayName + " to "
                        + fac.station.getFullName() + " (total: " + total
                        + "/" + limit + ").");
            }
        }
    }

    private static final class WeaponData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        float size;
        int daysOffset, limit;
        boolean warnedLimit = false;

        public WeaponData(CargoStackAPI stack, OmniFac factory)
        {
            fac = factory;
            id = (String) stack.getData();
            displayName = stack.getDisplayName();
            size = stack.getCargoSpacePerUnit();
            limit = (int) stack.getMaxSize();
            daysOffset = fac.numHeartbeats % getDaysToCreate();
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(size, 1f);
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
        public int getLimit()
        {
            return limit;
        }

        @Override
        public void create()
        {
            int total = fac.station.getCargo().getNumWeapons(id);

            if (total >= limit)
            {
                if (SHOW_LIMIT_REACHED && !warnedLimit)
                {
                    warnedLimit = true;
                    Global.getSector().addMessage("Limit reached for " + displayName
                            + " at " + fac.station.getFullName() + ".");
                }

                daysOffset = fac.numHeartbeats % getDaysToCreate();
                return;
            }

            total++;
            warnedLimit = false;
            fac.station.getCargo().addWeapons(id, 1);

            if (SHOW_ADDED_CARGO)
            {
                Global.getSector().addMessage("Added " + displayName + " to "
                        + fac.station.getFullName() + " (total: " + total
                        + "/" + limit + ").");
            }
        }
    }
}
