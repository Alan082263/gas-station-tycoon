#pragma once

// What a day's trading came to. Pure data -- the UI decides how to show it.
struct DayResult
{
    int served = 0;
    double litresSold = 0;
    double fuelRevenue = 0;
    double shopRevenue = 0;
    double interest = 0;
    double fixedCosts = 0;
    double eventCost = 0;

    double revenue() const { return fuelRevenue + shopRevenue; }
    double costs() const { return fixedCosts + eventCost; }
};
