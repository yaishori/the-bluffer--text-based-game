package assignment3.server;

interface ServerProtocolFactory<T> {
   ServerProtocol<T> create(TBGPData tbgpData);
}