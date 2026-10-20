package gestion.service;

import java.nio.file.Path;
import gestion.repository.CsvStore;

final class ServiceChanges {
    private ServiceChanges() { }
    static void apply(Path directory, Runnable mutation, Runnable save, Runnable restore) {
        try {
            mutation.run();
            save.run();
            CsvStore.onRollback(directory, restore);
        } catch (RuntimeException failure) {
            restore.run();
            throw failure;
        }
    }
}
