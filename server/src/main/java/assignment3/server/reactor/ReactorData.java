package assignment3.server.reactor;

import java.util.concurrent.ExecutorService;

import assignment3.server.TBGPData;
import assignment3.server.tokenizer.TokenizerFactory;

import java.nio.channels.Selector;

/**
 * a simple data structure that hold information about the reactor, including getter methods
 */
public class ReactorData<T> {

    private final ExecutorService _executor;
    private final Selector _selector;
    private final AsyncServerProtocolFactory<T> _protocolMaker;
    private final TokenizerFactory<T> _tokenizerMaker;
    private final TBGPData tbgpData;
    
    public ExecutorService getExecutor() {
        return _executor;
    }

    public Selector getSelector() {
        return _selector;
    }

	public ReactorData(ExecutorService _executor,
			           Selector _selector,
			           AsyncServerProtocolFactory<T> protocol,
			           TokenizerFactory<T> tokenizer,
			           TBGPData tbgpData) {
		this._executor = _executor;
		this._selector = _selector;
		this._protocolMaker = protocol;
		this._tokenizerMaker = tokenizer;
		this.tbgpData = tbgpData;
	}

	public AsyncServerProtocolFactory<T> getProtocolMaker() {
		return _protocolMaker;
	}

	public TokenizerFactory<T> getTokenizerMaker() {
		return _tokenizerMaker;
	}

	public TBGPData getTbgpData() {
		return tbgpData;
	}
}
