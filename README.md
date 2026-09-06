# Gas Station Tycoon

A turn-based pricing game in plain Java, built on top of an object-oriented gas
station model. No frameworks, no dependencies, no build tool — `javac` and `java`
are all you need.

You have inherited a two-pump forecourt and a loan you did not ask for. Every
morning you read the news, set your pump price, decide how much fuel to buy at
today's wholesale, and choose whether to spend on the place. Then you open up
and find out how many drivers liked your price.

```
================================================================
  DAY 12 of 40  -  Friday, Rain
================================================================
  Cash $9,420.11      Debt $14,204.55     Net worth $6,309.71
  Tank 6,240 / 10,000 L      Pumps 3 (114 fills/day)
  Your price $1.590/L      Wholesale $1.284/L      Margin $0.306/L

  NEWS: CUP FINAL
  Thousands are driving to the stadium and most of them are low on fuel.
```

## Running it

```bash
javac gasstation/*.java gasstation/game/*.java SelfTest.java

java gasstation.game.GasStationTycoon                  # play
java gasstation.game.GasStationTycoon --hard --seed=42 # a specific hard run
java gasstation.StationDemo                            # the underlying model, no game
java gasstation.game.BalanceCheck 300 normal           # 300 automated playthroughs
java gasstation.game.BalanceCheck sweep normal         # profit vs price curve
java SelfTest                                          # 25 checks on the model

./run-gui.sh                                           # the JavaFX desktop version
```

Flags: `--easy` / `--hard`, `--seed=N` (replay an exact run), `--days=N`,
`--name=TEXT`.

Requires JDK 8 or newer. No external libraries.

`run-gui.sh` needs a JDK with JavaFX available at run time (any JDK works — it
fetches the three `org.openjfx` jars it needs from Maven Central into `lib/`
the first time it runs, then points `javac`/`java` at them with
`--module-path`). No Maven/Gradle involved.

## How the game works

**Demand is a share of passing traffic.** Roughly 240 cars go past the door each
day, and each driver chooses between you and the station down the road. Your
share follows an S-curve centred on the going rate ($1.559): match it and you
take about half of them, undercut it and your share climbs fast, go over it and
it falls away just as fast.

**Your pumps cap the upside.** Each pump serves 38 cars a day. Cutting your price
below about $1.60 generates more demand than two pumps can physically serve, so
the cars queue and drive on. That is what makes the third pump worth $4,200 —
and it is also why buying it should change your pricing.

**Wholesale drifts.** The price you pay follows a mean-reverting random walk
around $1.30. Filling the tank when fuel is cheap is buying tomorrow's margin
today — if you have the space and the cash. Every delivery costs a $150 fee, so
lots of small top-ups quietly bleed you.

**Events happen.** Tanker strikes, roadworks, cup finals, refinery fires, a
pump breaking down. You see the day's news *before* you set your price, so each
one is a decision rather than a dice roll.

**You are scored on net worth** — cash, plus the fuel in the ground at wholesale,
minus the loan. Miss the target and you have merely survived. End any day with
the till in the red and the bank takes the keys.

| Difficulty | Cash | Debt | Target | Days |
|---|---|---|---|---|
| `--easy`   | $12,000 | $10,000 | $34,000 | 40 |
| (default)  |  $8,000 | $15,000 | $25,000 | 40 |
| `--hard`   |  $5,000 | $20,000 | $12,000 | 40 |

## Design

Two packages, and the split between them is the point.

**`gasstation`** — the domain model. It knows about fuel and nothing about money
beyond the price on the pump. It was written as a straight simulation before any
of the game existed, and it still runs as one (`StationDemo`).

| Class | Responsibility |
|---|---|
| `FuelTank` | Capacity and level. Refuses impossible states. `withdraw()` is package-private — fuel leaves only through a pump. |
| `Pump` | Meters fuel out of the tank for one customer at a time. |
| `GasStation` | Owns the tank and the pumps, sets the price, records sales, keeps the tank stocked. |
| `Customer` | Wants a quantity of fuel; collects receipts. |
| `Transaction` | Immutable sale record. |
| `OutOfFuelException`, `NoPumpAvailableException` | Checked — being out of fuel is a business condition, not a bug. |

**`gasstation.game`** — the game layer. It does not re-implement the station, it
*plays* it: every simulated customer is a real `Customer` served through
`GasStation.serve()`, producing a real `Transaction` and drawing real litres.

| Class | Responsibility |
|---|---|
| `GameConfig` | Every tunable number, in one place, with `easy()` / `normal()` / `hard()`. |
| `Market` | Wholesale random walk; how many drivers your price wins. |
| `Business` | Cash, debt, upgrades — the money the station model deliberately knows nothing about. |
| `Weather`, `RandomEvent`, `Upgrade` | Enums carrying their own data and, for upgrades, their own behaviour. |
| `DayOutlook`, `DayResult` | The morning's news, and what happened. |
| `DaySimulator` | Runs one day. No input or output, so it is testable. |
| `GasStationTycoon` | The console game. |
| `BalanceCheck` | Automated playtesting. |

**`gasstation.gui`** — a JavaFX desktop front end. It is a second orchestrator
over the exact same `GameConfig`/`Business`/`Market`/`GasStation`/`DaySimulator`
the console game uses — same rules, same balance, just buttons and dialogs
instead of a terminal prompt.

| Class | Responsibility |
|---|---|
| `GasStationTycoonFX` | The JavaFX app: intro screen, dashboard, day report, ending. |
| `ActionDialogs` | The five morning actions (price, fuel, upgrades, loan, help) as modal dialogs. |

Notes on a couple of decisions:

- **Money is `BigDecimal` throughout.** Pump prices carry three decimals
  ($1.729); totals round to cents once, HALF_UP, at the moment of sale. `double`
  for money is the classic bug in this kind of program.
- **`serve()` frees the pump in a `finally`**, so a refused sale never leaves a
  pump stuck occupied.
- **`DaySimulator` does no I/O.** That is what lets `BalanceCheck` run thousands
  of games in a second to check the balance.

## Balance

`BalanceCheck` plays four strategies over the same seeds. The game is balanced
so that naive play loses and thoughtful play usually wins:

```
300 runs per strategy, normal difficulty, target $25,000.00 in 40 days

STRATEGY                                     WIN%    BUST%       MEDIAN
Race to the bottom (wholesale + $0.10)       0.0%     0.0%   -$6,197.52
Match the going rate                         3.0%     0.0%   $14,591.66
Charge what you like (rate + $0.25)          0.0%     0.0%    $1,019.08
Play it properly                            63.7%     0.0%   $28,168.36
```

`BalanceCheck sweep` prints median net worth against a flat price, with and
without a third pump — the peak moves from $1.65 to $1.59 when you buy it, which
is the shape a pricing game wants.

## Ideas not built yet

- Multiple fuel grades (regular / premium / diesel), each with its own tank.
- A rival station that reprices against you instead of a fixed going rate.
- Staff, opening hours, and queue tolerance as separate levers.
- A save file, so a run can be resumed.
- A mobile or web front end over the same `DaySimulator` (there's a JavaFX desktop one now — see `gasstation.gui`).
