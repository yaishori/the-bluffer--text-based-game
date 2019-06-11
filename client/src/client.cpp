#include <stdlib.h>
#include <boost/locale.hpp>
#include <boost/thread.hpp>
#include <boost/thread/mutex.hpp>
#include "../include/connectionHandler.h"
#include "../include/encoder/utf8.h"
#include "../include/encoder/encoder.h"

boost::mutex mtx;
boost::condition_variable condition;
bool isDone = false;

class UserInputTask {
private:
    ConnectionHandler *connectionHandler;
public:
    UserInputTask (ConnectionHandler *connectionHandler) : connectionHandler(connectionHandler) {}

    void run(){
    	while(!isDone) {
    		const short bufsize = 1024;
			char buf[bufsize];
			std::cin.getline(buf, bufsize);
			std::string line(buf);
			if (!connectionHandler->sendLine(line)) {
				std::cout << "Disconnected. Exiting...\n" << std::endl;
				break;
			}

			boost::mutex::scoped_lock lock(mtx);

			condition.wait(lock);
    	}

        boost::this_thread::yield(); //Gives up the remainder of the current thread's time slice, to allow other threads to run.
    }
};

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    UserInputTask userInputTask(&connectionHandler);

	boost::thread userInputThread(&UserInputTask::run, &userInputTask);

    while (!isDone) {
        // We can use one of three options to read data from the server:
        // 1. Read a fixed number of characters
        // 2. Read a line (up to the newline character using the getline() buffered reader
        // 3. Read up to the null character
        std::string answer;
        // Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
        // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
        if (!connectionHandler.getLine(answer)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        
		int len=answer.length();
		// A C string must end with a 0 char delimiter.  When we filled the answer buffer from the socket
		// we filled up to the \n char - we must make sure now that a 0 char is also present. So we truncate last character.
        answer.resize(len-1);
        std::cout << answer << std::endl;
        if (answer == "SYSMSG QUIT ACCEPTED") {
        	isDone = true;
        }

        condition.notify_all();
    }

    userInputThread.join();
    return 0;
}
