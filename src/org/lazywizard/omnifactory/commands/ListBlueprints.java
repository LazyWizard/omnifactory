package org.lazywizard.omnifactory.commands;

import java.util.ArrayList;
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
                "| %-25s | %-13s | %-6s | %-7s | %-5s | %-5s |",
                "Display name", "id", "type", "analyze", "build", "limit"));
        System.out.println(String.format(
                "| %-25s | %-13s | %-6s | %-7d | %-5d | %-5d |",
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
        Console.showMessage(String.format(
                "| %-25s | %-13s | %-6s | %-7s | %-5s | %-5s |",
                "Display name", "id", "type", "analyze", "build", "limit"));
        for (Blueprint data : datas)
        {
            Console.showMessage(String.format(
                    "| %-25s | %-13s | %-6s | %-7d | %-5d | %-5d |",
                    data.getDisplayName(), data.getId(), data.getType(),
                    data.getDaysToAnalyze(), data.getDaysToCreate(), data.getLimit()));
        }
        return CommandResult.SUCCESS;
    }
}
