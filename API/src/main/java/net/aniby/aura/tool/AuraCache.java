package net.aniby.aura.tool;

import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuraCache {
    private final File file;
    @Getter
    private String data = null;

    public AuraCache(File file) {
        this.file = file;
    }

    public boolean cache(String value) throws IOException {
        if (this.data.contains(value))
            return false;
        this.data += "\n" + value;
        this.save();
        return true;
    }

    public void load() throws IOException {
        this.data = FileUtils.readFileToString(this.file, StandardCharsets.UTF_8);
    }

    public void save() throws IOException {
        FileUtils.writeStringToFile(this.file, this.data, StandardCharsets.UTF_8);
    }
}
