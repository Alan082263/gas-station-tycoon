#pragma once

#include "Business.h"
#include "DayOutlook.h"
#include "DayResult.h"
#include "GameConfig.h"

#include <random>

// Trades one day: works out how many drivers stop, sells them fuel out of
// the tank, then settles the day's costs and interest. Mutates `business`.
DayResult tradeDay(const Config &config, Business &business,
                   const DayOutlook &outlook, std::mt19937 &rng);
