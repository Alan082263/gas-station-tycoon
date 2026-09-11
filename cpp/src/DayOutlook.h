#pragma once

#include "GameConfig.h"
#include "RandomEvent.h"

#include <random>
#include <string>

// What the player sees when the day starts, before deciding anything.
struct DayOutlook
{
    std::string weather;
    std::string weekday;
    double weatherFactor = 1.0;
    Event event;
};

// Rolls the weather and (sometimes) an event for a 1-based day number.
DayOutlook dawn(const Config &config, int day, std::mt19937 &rng);
