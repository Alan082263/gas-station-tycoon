#include "ConsoleUI.h"

#include "ConsoleIO.h"
#include "Money.h"

#include <iostream>

namespace ui
{
    void printTitle(const Config &config, unsigned seed)
    {
        std::cout << "\nG A S   S T A T I O N   T Y C O O N\n";
        std::cout << "Take over " << config.name << " and reach " << money::cash(config.target)
                  << " net worth in " << config.days << " days.\n";
        std::cout << "Seed " << seed << " (replay with --seed=" << seed << ")\n";
    }

    void printDashboard(const Config &config, const Business &business,
                        const DayOutlook &outlook, int day)
    {
        std::cout << "\n================ DAY " << day << " / " << config.days << " ================\n";
        std::cout << outlook.weekday << " | " << outlook.weather
                  << " | Wholesale " << money::perLitre(config.wholesale) << "/L\n";

        if (outlook.event.happened())
            std::cout << "NEWS: " << outlook.event.title << " - " << outlook.event.description << "\n";

        std::cout << "Cash " << money::cash(business.cash)
                  << " | Debt " << money::cash(business.debt)
                  << " | Tank " << money::litres(business.tank) << " / " << money::litres(config.tankCapacity)
                  << " | Pumps " << business.pumps
                  << " | Price " << money::perLitre(config.pumpPrice) << "/L\n";
    }

    int askAction()
    {
        std::cout << "\n[1] Set price  [2] Order fuel  [3] Upgrades  [4] Repay loan  [5] Open  [0] Quit\n";
        return console::askInt("> ", 0, 5, 5);
    }

    void orderFuel(Config &config, Business &business, const DayOutlook &outlook)
    {
        if (outlook.event.blocksDelivery)
        {
            std::cout << "No tanker is coming today.\n";
            return;
        }

        double affordable = business.affordableLitres(config, config.wholesale);
        std::cout << "Room: " << money::litres(config.tankCapacity - business.tank)
                  << ", affordable: " << money::litres(affordable) << "\n";

        if (affordable < 1)
        {
            std::cout << "You cannot afford a delivery.\n";
            return;
        }

        double order = 0;
        if (!console::askLitres("Litres to order (0 to skip, -1 for max): ", order))
        {
            std::cout << "That is not a number.\n";
            return;
        }
        if (order < 0)
            order = affordable;

        double billed = 0;
        if (business.acceptDelivery(config, config.wholesale, order, billed) != Business::Refusal::None)
        {
            std::cout << "Order must be between 1 and " << money::litres(affordable) << ".\n";
            return;
        }

        std::cout << money::litres(order) << " delivered for " << money::cash(billed) << ".\n";
    }

    void chooseUpgrade(Config &config, Business &business)
    {
        std::cout << "\nUPGRADES (cash " << money::cash(business.cash) << ")\n";
        for (std::size_t i = 0; i < business.upgrades.size(); ++i)
        {
            const Upgrade &upgrade = business.upgrades[i];
            std::cout << "[" << i + 1 << "] " << upgrade.label << " - " << money::cash(upgrade.cost)
                      << ": " << upgrade.description << (upgrade.owned ? " [OWNED]" : "") << "\n";
        }

        int choice = console::askInt("Buy which? (0 to go back): ", 0,
                                     static_cast<int>(business.upgrades.size()), 0);
        if (choice == 0)
            return;

        const std::string label = business.upgrades[choice - 1].label;
        switch (business.buyUpgrade(static_cast<std::size_t>(choice - 1), config))
        {
        case Business::Refusal::None:
            std::cout << "Bought: " << label << ".\n";
            break;
        case Business::Refusal::AlreadyOwned:
            std::cout << "You already have that.\n";
            break;
        case Business::Refusal::NotEnoughCash:
            std::cout << "Not enough cash.\n";
            break;
        default:
            std::cout << "That is not one of the upgrades.\n";
            break;
        }
    }

    void repayLoan(Business &business)
    {
        double amount = console::askDouble("Repay how much? $", 0, 1000000, 0);
        business.repay(amount);
    }

    void printDayResult(const Config &config, const Business &business, const DayResult &result)
    {
        std::cout << "\n"
                  << result.served << " customers served, " << money::litres(result.litresSold)
                  << " sold. Revenue " << money::cash(result.revenue()) << ".\n";
        std::cout << "Costs: " << money::cash(result.costs()) << " plus "
                  << money::cash(result.interest) << " interest. Cash: "
                  << money::cash(business.cash) << "\n";

        if (business.cash >= 0)
            std::cout << "Net worth: " << money::cash(business.netWorth(config.wholesale))
                      << " | Tank: " << money::litres(business.tank) << "\n";
    }

    void printBankrupt(int day)
    {
        std::cout << "The bank takes the keys. You went bankrupt on day " << day << ".\n";
    }

    void printFinale(const Config &config, const Business &business)
    {
        double netWorth = business.netWorth(config.wholesale);
        std::cout << "\nFINAL NET WORTH: " << money::cash(netWorth) << "\n";
        std::cout << (netWorth >= config.target
                          ? "You built a thriving forecourt.\n"
                          : "The station survives, but the target got away.\n");
    }
}
