package tech.pegasys.teku.storage.store;

import org.apache.tuweni.bytes.Bytes;

import java.nio.file.Path;

public class FileKeyValueStoreFactory {

    private static volatile KeyValueStore<String, Bytes> keyValueStore;

    public static KeyValueStore<String, Bytes>  createStore(Path path) {
        keyValueStore = new FileKeyValueStore(path);
        return keyValueStore;
    }

    public static KeyValueStore<String, Bytes> getStore() {
        return keyValueStore;
    }

}
