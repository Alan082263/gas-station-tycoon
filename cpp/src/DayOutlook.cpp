#include "DayOutlook.h"

#include "Weather.h"

DayOutlook dawn(const Config &config, int day, std::mt19937 &rng)
{
    Weather today = rollWeather(rng);

    Event event = noEvent();
    std::uniform_real_distribution<double> chance(0.0, 1.0);
    if (chance(rng) < config.eventChance)
        event = rollEvent(rng);

    return DayOutlook{today.name, weekdayName(day), today.factor, event};
}
