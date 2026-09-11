#pragma once

#include <random>
#include <string>
#include <vector>

struct Event
{
    std::string title;
    std::string description;
    double demand = 1.0;
    double wholesaleShift = 0.0;
    double cost = 0.0;
    double fuelLost = 0.0;
    int pumpsOffline = 0;
    int weight = 0;
    bool blocksDelivery = false;

    bool happened() const { return !title.empty(); }
};

// A quiet day: no headline, no effect.
Event noEvent();

// Everything that can go wrong (or right) on a forecourt.
const std::vector<Event> &eventCatalogue();

// Picks one event, weighted by its `weight`.
Event rollEvent(std::mt19937 &rng);
