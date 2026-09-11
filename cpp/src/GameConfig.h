#pragma once

#include <string>
#include <vector>

// Every dial the game is tuned with. Difficulty presets just push these
// numbers around; nothing else in the game knows what "hard" means.
struct Config
{
    std::string name = "Riverside Fuels";
    int days = 40;

    double cash = 8000.0;
    double debt = 15000.0;
    double target = 25000.0;

    double tankCapacity = 10000.0;
    double fuel = 3000.0;
    double pumpPrice = 1.559;
    int pumps = 2;

    double fixedCosts = 220.0;
    double deliveryFee = 150.0;
    double interestRate = 0.0008;

    int traffic = 240;
    int fillsPerPump = 38;

    double referencePrice = 1.559;
    double sensitivity = 14.0;

    double wholesale = 1.28;
    double wholesaleMean = 1.30;
    double volatility = 0.035;
    double floor = 0.95;
    double ceiling = 1.85;
    double eventChance = 0.30;
};

void applyEasy(Config &config);
void applyHard(Config &config);

// Reads --easy / --hard / --seed=N / --days=N. Returns the seed to use,
// which is random unless --seed was given.
unsigned parseArguments(const std::vector<std::string> &arguments, Config &config);
