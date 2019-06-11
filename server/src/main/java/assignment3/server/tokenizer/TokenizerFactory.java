package assignment3.server.tokenizer;

public interface TokenizerFactory<T> {
   MessageTokenizer<T> create();
}
