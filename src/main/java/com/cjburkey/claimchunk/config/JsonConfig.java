package com.cjburkey.claimchunk.config;

import com.cjburkey.claimchunk.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class JsonConfig<T> implements Iterable<T> {

    public final File file;
    private final HashSet<T> data = new HashSet<>();
    private final Class<T[]> referenceClass;
    private final boolean pretty;

    public JsonConfig(Class<T[]> referenceClass, File file, boolean pretty) {
        this.file = file;
        this.referenceClass = referenceClass;
        this.pretty = pretty;
    }

    public Collection<T> getData() {
        return Collections.unmodifiableCollection(data);
    }

    public void addData(T toAdd) {
        data.add(toAdd);
    }

    public void saveData() throws IOException {
        if (file == null) {
            return;
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            Utils.err("Failed to create parent directory for file: %s", file.getAbsolutePath());
            return;
        }
        if (file.exists() && !file.delete()) {
            Utils.err("Failed to clear old offline JSON data in file: %s", file.getAbsolutePath());
            return;
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(getGson().toJson(data));
        }
    }

    public void reloadData() throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
                json.append('\n');
            }
        }
        T[] out = getGson().fromJson(json.toString(), referenceClass);
        if (out != null) {
            data.clear();
            Collections.addAll(data, out);
        }
    }

    @SuppressWarnings("unused")
    public void clearData() {
        data.clear();
    }

    private Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        if (pretty) builder.setPrettyPrinting();
        return builder
                       .serializeNulls()
                       .create();
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public @Nonnull Iterator<T> iterator() {
        return getData().iterator();
    }
}