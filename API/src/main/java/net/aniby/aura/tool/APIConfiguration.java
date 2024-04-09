package net.aniby.aura.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraAPI;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class APIConfiguration {
    public enum Destination {
        CURRENT, API_FOLDER, PLUGIN
    }

    File file;
    JSONObject data;

    public APIConfiguration(@Nonnull Destination destination, @Nonnull Path path) throws IOException, NullPointerException, ParseException {
        Path absolutePath = new File(".").toPath().toAbsolutePath();

        File file = null;
        switch (destination) {
            case CURRENT -> {
                file = absolutePath.resolve(path).toFile();
            }
            case API_FOLDER -> {
                file = absolutePath.resolve("api").resolve(path).toFile();
            }
            case PLUGIN -> {
                List<String> way = Arrays.stream(absolutePath.toString().split("/"))
                        .filter(c -> !c.isEmpty()).toList();
                if (!way.isEmpty()) {
                    switch (way.get(way.size() - 1)) {
                        case "AuraAPI" -> {
                            file = absolutePath.resolve(path).toFile();
                        }
                        case "plugins" -> {
                            file = absolutePath.resolve("AuraAPI").resolve(path).toFile();
                        }
                    }
                }
                if (file == null)
                    file = absolutePath.resolve("plugins").resolve("AuraAPI").resolve(path).toFile();
            }
        }

        if (file == null)
            throw new RuntimeException("Error on config creating!");

        try {
            Path cyclicFolder = file.toPath().toAbsolutePath().getParent();
            while (cyclicFolder != null) {
                File folderFile = cyclicFolder.toFile();
                if (folderFile.exists())
                    break;

                folderFile.mkdirs();
                cyclicFolder = cyclicFolder.getParent();
            }
        } catch (Exception ignored) {}

        this.file = file;
        if (!this.file.exists()) {
            InputStream initialStream = AuraAPI.class.getClassLoader().getResourceAsStream(path.toString());
            OutputStream outStream = new FileOutputStream(file);

            IOUtils.write(initialStream.readAllBytes(), outStream);
            outStream.flush();

            IOUtils.closeQuietly(initialStream);
            IOUtils.closeQuietly(outStream);
        }

        FileInputStream inputStream = new FileInputStream(this.file);

        String jsonString = String.join("", IOUtils.readLines(inputStream, StandardCharsets.UTF_8))
                .replace("\n", "");
        this.data = (JSONObject) parser.parse(jsonString);

        IOUtils.closeQuietly(inputStream);
    }

    private static final JSONParser parser = new JSONParser();
}
