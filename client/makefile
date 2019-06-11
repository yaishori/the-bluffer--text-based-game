CFLAGS:=-c -Wall -Weffc++ -g
LDFLAGS:=-lboost_system -lboost_locale -lboost_thread

all: bin/encoder.o client
	g++ -o bin/client bin/encoder.o bin/connectionHandler.o bin/client.o $(LDFLAGS) 

client: bin/connectionHandler.o bin/client.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/client.o: src/client.cpp
	g++ $(CFLAGS) -o bin/client.o src/client.cpp
	
bin/encoder.o: src/encoder.cpp
	g++ $(CFLAGS) -o bin/encoder.o src/encoder.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
