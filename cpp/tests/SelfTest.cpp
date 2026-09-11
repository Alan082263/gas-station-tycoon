// A few checks that the domain still behaves. Console only, no framework:
//   make test && ./build/selftest
#include "Business.h"
#include "DayOutlook.h"
#include "DaySimulator.h"
#include "GameConfig.h"
#include "Market.h"
#include "Money.h"
#include "Weather.h"

#include <cmath>
#include <iostream>
#include <random>
#include <string>

namespace
{
    int failures = 0;

    void check(bool passed, const std::string &what)
    {
        std::cout << (passed ? "  ok   " : "  FAIL ") << what << "\n";
        if (!passed)
            ++failures;
    }

    void checkEquals(const std::string &actual, const std::string &expected, const std::string &what)
    {
        bool passed = actual == expected;
        std::cout << (passed ? "  ok   " : "  FAIL ") << what;
        if (!passed)
            std::cout << "  (got \"" << actual << "\", wanted \"" << expected << "\")";
        std::cout << "\n";
        if (!passed)
            ++failures;
    }

    bool near(double a, double b, double tolerance = 1e-9)
    {
        return std::fabs(a - b) <= tolerance;
    }

    void moneyFormatting()
    {
        std::cout << "Money\n";
        checkEquals(money::cash(1234.5), "$1,234.50", "cash groups thousands and pads to 2dp");
        checkEquals(money::cash(-1234.5), "-$1,234.50", "negative cash puts the minus outside");
        checkEquals(money::cash(0), "$0.00", "zero cash");
        checkEquals(money::cash(999.999), "$1,000.00", "cash rounds up into a new thousand");
        checkEquals(money::perLitre(1.559), "$1.559", "fuel prices keep 3dp");
        checkEquals(money::litres(3000), "3,000 L", "litres group and drop decimals");
    }

    void upgrades()
    {
        std::cout << "Upgrades\n";
        Config config;
        Business business = Business::start(config);

        business.cash = 100;
        check(business.buyUpgrade(0, config) == Business::Refusal::NotEnoughCash,
              "cannot buy what you cannot afford");

        business.cash = 10000;
        double capacityBefore = config.tankCapacity;
        int pumpsBefore = business.pumps;

        check(business.buyUpgrade(0, config) == Business::Refusal::None, "extra pump bought");
        check(business.pumps == pumpsBefore + 1, "extra pump adds a pump");
        check(business.buyUpgrade(0, config) == Business::Refusal::AlreadyOwned,
              "cannot buy the same upgrade twice");

        check(business.buyUpgrade(1, config) == Business::Refusal::None, "bigger tank bought");
        check(near(config.tankCapacity, capacityBefore + 8000), "bigger tank adds 8,000 L");

        check(business.buyUpgrade(99, config) == Business::Refusal::BadAmount,
              "an index off the end is refused");

        check(business.has("Extra pump"), "has() sees an owned upgrade");
        check(!business.has("Car wash"), "has() does not see an unowned one");
    }

    void deliveries()
    {
        std::cout << "Deliveries\n";
        Config config;
        Business business = Business::start(config);
        double wholesale = 1.30;

        double room = config.tankCapacity - business.tank;
        double payable = (business.cash - config.deliveryFee) / wholesale;
        check(near(business.affordableLitres(config, wholesale), std::min(room, payable)),
              "affordable is the lesser of room and cash");

        double billed = 0;
        check(business.acceptDelivery(config, wholesale, -5, billed) == Business::Refusal::BadAmount,
              "a negative order is refused");
        check(business.acceptDelivery(config, wholesale, 1e9, billed) == Business::Refusal::BadAmount,
              "an order bigger than the tank is refused");

        double cashBefore = business.cash;
        double tankBefore = business.tank;
        check(business.acceptDelivery(config, wholesale, 1000, billed) == Business::Refusal::None,
              "a sensible order goes through");
        check(near(business.tank, tankBefore + 1000), "the fuel arrives");
        check(near(billed, 1000 * wholesale + config.deliveryFee), "the bill includes the delivery fee");
        check(near(business.cash, cashBefore - billed), "the bill comes out of cash");
    }

    void repayment()
    {
        std::cout << "Repayment\n";
        Config config;
        Business business = Business::start(config);

        business.cash = 500;
        business.debt = 200;
        business.repay(10000);
        check(near(business.debt, 0), "you cannot repay more than you owe");
        check(near(business.cash, 300), "and only what you owe leaves your pocket");

        business.repay(-50);
        check(near(business.cash, 300), "a negative repayment does nothing");
    }

    void marketBand()
    {
        std::cout << "Market\n";
        Config config;

        check(near(market::afterEvent(config, 1.80, 0.50), config.ceiling),
              "a spike is capped at the ceiling");
        check(near(market::afterEvent(config, 1.00, -0.50), config.floor),
              "a crash is held at the floor");

        std::mt19937 rng(42);
        for (int i = 0; i < 500; ++i)
        {
            double price = market::overnight(config, config.wholesale, rng);
            if (price < config.floor || price > config.ceiling)
            {
                check(false, "overnight drift stays inside the band");
                return;
            }
        }
        check(true, "overnight drift stays inside the band");

        double cheap = market::customerShare(1.20, 1.559, 14);
        double dear = market::customerShare(1.90, 1.559, 14);
        check(cheap > dear, "a lower price wins a bigger share");
        check(near(market::customerShare(1.559, 1.559, 14), 0.5),
              "pricing at the reference splits the traffic evenly");
    }

    void tradingIsRepeatable()
    {
        std::cout << "Day trading\n";
        Config config;

        auto runOneDay = [&config](unsigned seed) {
            std::mt19937 rng(seed);
            Business business = Business::start(config);
            DayOutlook outlook = dawn(config, 1, rng);
            return tradeDay(config, business, outlook, rng);
        };

        DayResult first = runOneDay(7);
        DayResult second = runOneDay(7);
        check(first.served == second.served && near(first.fuelRevenue, second.fuelRevenue),
              "the same seed trades the same day");

        std::mt19937 rng(11);
        Config quiet = config;
        Business business = Business::start(quiet);
        business.tank = 0;
        DayOutlook outlook = dawn(quiet, 1, rng);
        DayResult dry = tradeDay(quiet, business, outlook, rng);
        check(dry.served == 0 && near(dry.fuelRevenue, 0), "an empty tank serves nobody");
        check(dry.interest > 0, "interest still accrues on a dead day");
    }

    void weekdays()
    {
        std::cout << "Weather and weekdays\n";
        check(weekdayName(1) == "Monday", "day 1 is a Monday");
        check(weekdayName(8) == "Monday", "day 8 comes back round to Monday");
        check(dayFactor("Friday") > dayFactor("Sunday"), "Friday is busier than Sunday");
        check(near(dayFactor("Wednesday"), 1.0), "Wednesday is the average day");

        std::mt19937 rng(3);
        for (int i = 0; i < 200; ++i)
        {
            Weather weather = rollWeather(rng);
            if (weather.name.empty() || weather.factor <= 0)
            {
                check(false, "rolled weather is always named and positive");
                return;
            }
        }
        check(true, "rolled weather is always named and positive");
    }
}

int main()
{
    std::cout << "Gas Station Tycoon -- self test\n\n";

    moneyFormatting();
    upgrades();
    deliveries();
    repayment();
    marketBand();
    tradingIsRepeatable();
    weekdays();

    std::cout << "\n" << (failures == 0 ? "All checks passed.\n" : "FAILURES: ")
              << (failures == 0 ? "" : std::to_string(failures) + "\n");
    return failures == 0 ? 0 : 1;
}
