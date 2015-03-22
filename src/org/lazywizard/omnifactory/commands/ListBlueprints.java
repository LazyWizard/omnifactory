package org.lazywizard.omnifactory.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.omnifactory.Blueprint;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;
import org.lazywizard.omnifactory.BlueprintMaster;

public class ListBlueprints implements BaseCommand
{
    public static void main(String[] args)
    {
        String id = "lightdualmg", name = "Light Dual Machine Gun";
        BlueprintType type = BlueprintType.WEAPON;
        int daysToAnalyze = 5, daysToCreate = 10, limit = 20;

        System.out.println(String.format(
                "| %25s | %13s | %6s | %7s | %5s | %5s |",
                "Display name", "id", "type", "analyze", "build", "limit"));
        System.out.println(String.format(
                "| %25s | %13s | %6s | %7d | %5d | %5d |",
                name, id, type, daysToAnalyze, daysToCreate, limit));
        System.out.println(String.format("|%78s|", ""));
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        List<Blueprint> datas = new ArrayList<>(BlueprintMaster.getShipBlueprints().values());
        datas.addAll(BlueprintMaster.getWingBlueprints().values());
        datas.addAll(BlueprintMaster.getWeaponBlueprints().values());
        Collections.sort(datas, new BlueprintSorter());
        StringBuilder output = new StringBuilder(String.format(
                "\n| %-20s | %-18s | %-6s | %-7s | %-5s | %-5s |\n",
                "id", "display name", "type", "analyze", "build", "limit"));
        for (Blueprint data : datas)
        {
            output.append(String.format("| %-20.20s | %-18.18s | %-6.6s | %-7d | %-5d | %-5d |\n",
                    data.getId(), data.getDisplayName(), data.getType(),
                    data.getDaysToAnalyze(), data.getDaysToCreate(), data.getLimit()));
        }

        Console.showMessage(output.toString() + String.format("|%78s|", ""));
        return CommandResult.SUCCESS;
    }

    private static class BlueprintSorter implements Comparator<Blueprint>
    {
        @Override
        public int compare(Blueprint o1, Blueprint o2)
        {
            if (o1.getType() == o2.getType())
            {
                if (o1.getDaysToCreate() == o2.getDaysToCreate())
                {
                    return o1.getId().compareTo(o2.getId());
                }

                return Integer.compare(o1.getDaysToCreate(), o2.getDaysToCreate());
            }

            return o1.getType().compareTo(o2.getType());
        }
    }
}
