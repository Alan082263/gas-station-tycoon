#include "Weather.h"

#include <vector>

namespace
{
    const std::vector<Weather> &table()
    {
        static const std::vector<Weather> kinds = {
            {"Heatwave", 1.16}, {"Sunny", 1.06}, {"Cloudy", 1.00},
            {"Fog", 0.92}, {"Rain", 0.86}, {"Storm", 0.68}};
        return kinds;
    }
}

Weather rollWeather(std::mt19937 &rng)
{
    static const std::vector<int> weights = {8, 26, 28, 10, 20, 8};
    std::discrete_distribution<int> roll(weights.begin(), weights.end());
    return table()[roll(rng)];
}

double dayFactor(const std::string &weekday)
{
    if (weekday == "Monday")
        return .95;
    if (weekday == "Tuesday")
        return .93;
    if (weekday == "Thursday")
        return 1.06;
    if (weekday == "Friday")
        return 1.28;
    if (weekday == "Saturday")
        return 1.16;
    if (weekday == "Sunday")
        return .82;
    return 1.0;
}

std::string weekdayName(int day)
{
    static const std::string names[7] = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    return names[(day - 1) % 7];
}
