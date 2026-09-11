#pragma once

#include <random>
#include <string>

struct Weather
{
    std::string name;
    double factor = 1.0;
};

// Rolls today's weather, weighted towards the middling sorts.
Weather rollWeather(std::mt19937 &rng);

// How busy a given weekday is, relative to an average day.
double dayFactor(const std::string &weekday);

// "Monday" .. "Sunday" for a 1-based day number.
std::string weekdayName(int day);
