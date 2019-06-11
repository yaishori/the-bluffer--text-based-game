package assignment3.server.reactor;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import assignment3.server.TBGPMessage;
import assignment3.server.tokenizer.FixedSeparatorMessageTokenizer;
import assignment3.server.tokenizer.MessageTokenizer;

/**
 * This class supplies some data to the protocol, which then processes the data,
 * possibly returning a reply. This class is implemented as an executor task.
 * 
 */
public class ProtocolTask<T> implements Runnable {

	private final AsyncServerProtocol<T> _protocol;
	private final MessageTokenizer<T> _tokenizer;
	private final AsyncConnectionHandler<T> _handler;

	public ProtocolTask(final AsyncServerProtocol<T> protocol, final MessageTokenizer<T> tokenizer,
			final AsyncConnectionHandler<T> h) {
		this._protocol = protocol;
		this._tokenizer = tokenizer;
		this._handler = h;
	}

	// we synchronize on ourselves, in case we are executed by several threads
	// from the thread pool.
	public synchronized void run() {
		String clientAddress = _handler._sChannel.socket().getInetAddress() + ":" + _handler._sChannel.socket().getPort();
		
      // go over all complete messages and process them.
      while (_tokenizer.hasMessage()) {
         T msg = _tokenizer.nextMessage(clientAddress);
         String response = this._protocol.processMessage(msg, _handler.getBroadcastProtocolCallback());
         if (response != null) {
            try {
            	FixedSeparatorMessageTokenizer fixedTokenizer = (FixedSeparatorMessageTokenizer)_tokenizer;
               ByteBuffer bytes = fixedTokenizer.getBytesForMessage(new TBGPMessage(null, response, null));
               this._handler.addOutData(bytes);
            }
            catch (CharacterCodingException e) { e.printStackTrace(); }
         }
      }
	}

	public void addBytes(ByteBuffer b) {
		_tokenizer.addBytes(b);
	}
}
