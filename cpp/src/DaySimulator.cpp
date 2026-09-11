#include "DaySimulator.h"

#include "Market.h"
#include "Weather.h"

#include <algorithm>
#include <cmath>

DayResult tradeDay(const Config &config, Business &business,
                   const DayOutlook &outlook, std::mt19937 &rng)
{
    // A canopy halves the penalty from bad weather.
    double weatherFactor = outlook.weatherFactor;
    if (business.has("Forecourt canopy") && weatherFactor < 1)
        weatherFactor = 1 - (1 - weatherFactor) / 2;

    double sensitivity = std::max(4.0, config.sensitivity - business.sensitivityReduction());
    double share = market::customerShare(config.pumpPrice, config.referencePrice, sensitivity);

    std::uniform_real_distribution<double> noise(.90, 1.10);
    std::uniform_real_distribution<double> fill(22.0, 68.0);

    double expected = config.traffic * share * weatherFactor
                      * dayFactor(outlook.weekday) * outlook.event.demand
                      * (1 + business.demandBonus()) * noise(rng);

    int potential = std::max(0, static_cast<int>(std::lround(expected)));
    int capacity = std::max(0, business.pumps - outlook.event.pumpsOffline) * config.fillsPerPump;
    int attempts = std::min(potential, capacity);

    DayResult result;
    result.fixedCosts = config.fixedCosts;
    result.eventCost = outlook.event.cost;

    for (int i = 0; i < attempts && business.tank > 0; ++i)
    {
        double fillSize = std::round(fill(rng) * 100) / 100;
        double sold = std::min(fillSize, business.tank);
        business.tank -= sold;
        result.litresSold += sold;
        result.fuelRevenue += sold * config.pumpPrice;
        ++result.served;
    }

    result.shopRevenue = result.served * business.customerIncome();

    result.interest = business.debt * config.interestRate;
    business.debt += result.interest;
    business.cash += result.revenue() - result.costs();
    business.tank = std::max(0.0, business.tank - outlook.event.fuelLost);

    return result;
}
