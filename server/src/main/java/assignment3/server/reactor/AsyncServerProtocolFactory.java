package assignment3.server.reactor;

import assignment3.server.TBGPData;

interface AsyncServerProtocolFactory<TBGPMessage> {
   AsyncServerProtocol<TBGPMessage> create(TBGPData tbgpData);
}