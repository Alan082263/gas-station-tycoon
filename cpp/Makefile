CXX      := g++
CXXFLAGS := -std=c++20 -Wall -Wextra -O2 -Isrc -Iui

DOMAIN   := $(filter-out src/main.cpp,$(wildcard src/*.cpp))
UI       := $(wildcard ui/*.cpp)

all: build/tycoon

build/tycoon: src/main.cpp $(DOMAIN) $(UI)
	@mkdir -p build
	$(CXX) $(CXXFLAGS) -o $@ $^

build/selftest: tests/SelfTest.cpp $(DOMAIN)
	@mkdir -p build
	$(CXX) $(CXXFLAGS) -o $@ $^

run: build/tycoon
	./build/tycoon

test: build/selftest
	./build/selftest

clean:
	rm -rf build

.PHONY: all run test clean
