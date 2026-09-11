#include "RandomEvent.h"

Event noEvent()
{
    return Event{};
}

const std::vector<Event> &eventCatalogue()
{
    static const std::vector<Event> catalogue = {
        {"Tanker drivers' strike", "No deliveries today.", 1.00, 0.00, 0, 0, 0, 7, true},
        {"Roadworks", "Traffic is crawling past you.", 0.55, 0.00, 0, 0, 0, 9, false},
        {"Price war", "The station across the road slashed its price.", 0.70, 0.00, 0, 0, 0, 10, false},
        {"Card machines down", "Your card terminals were dead until mid-afternoon.", 0.78, 0.00, 0, 0, 0, 8, false},
        {"Pump breakdown", "A meter jammed solid; one pump is out all day.", 1.00, 0.00, 450, 0, 1, 9, false},
        {"Leaking seal", "A tank seal failed; fuel was lost and cleanup cost you.", 0.92, 0.00, 1200, 400, 0, 5, false},
        {"Trading standards", "An inspector found a mislabelled pump.", 1.00, 0.00, 600, 0, 0, 6, false},
        {"Refinery fire", "Wholesale prices jumped.", 1.00, 0.14, 0, 0, 0, 8, false},
        {"Supply glut", "Wholesale prices dropped.", 1.00, -0.11, 0, 0, 0, 8, false},
        {"Cup final", "Thousands are driving to the stadium.", 1.45, 0.00, 0, 0, 0, 9, false},
        {"Long weekend", "Everyone is heading out of town.", 1.35, 0.00, 0, 0, 0, 10, false},
        {"Good press", "The local paper called you the friendliest forecourt.", 1.22, 0.00, 0, 0, 0, 7, false},
        {"Haulage contract", "A delivery firm signed up its vans.", 1.28, 0.00, 0, 0, 0, 6, false}};
    return catalogue;
}

Event rollEvent(std::mt19937 &rng)
{
    const std::vector<Event> &available = eventCatalogue();

    std::vector<int> weights;
    weights.reserve(available.size());
    for (const Event &item : available)
        weights.push_back(item.weight);

    std::discrete_distribution<int> roll(weights.begin(), weights.end());
    return available[roll(rng)];
}
