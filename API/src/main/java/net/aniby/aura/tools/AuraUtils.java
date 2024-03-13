package net.aniby.aura.tools;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuraUtils {
    public static final long hour = 3600000L;
    public static final long minute = 60000L;
    public static final long day = 86400000L;

    static String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    static Random rnd = new Random();
    public static String getRandomString(int size) {
        StringBuilder salt = new StringBuilder();
        while (salt.length() < size) { // length of the random string.
            int index = (int) (rnd.nextFloat() * characters.length());
            salt.append(characters.charAt(index));
        }
        return salt.toString();
    }

    public static @NotNull String extractDiscordId(@NotNull String string) {
        if (string.contains("@") && string.contains("<") && string.contains(">"))
            return string.substring(2, string.length() - 1);
        return string;
    }

    public static double roundDouble(double aura) {
        return Math.floor(aura * 100) / 100;
    }

    public static boolean onlyDigits(String str) {
        String regex = "[0-9]+";
        Pattern p = Pattern.compile(regex);
        if (str == null) {
            return false;
        }
        Matcher m = p.matcher(str);
        return m.matches();
    }

    public static void saveDefaultFile(File file, String path, Class<?> _class) throws IOException {
        if (!file.exists()) {
            File folder = file.toPath().toAbsolutePath().getParent().toFile();
            if (!folder.exists())
                folder.mkdirs();
            file.createNewFile();

            InputStream initialStream = _class.getClassLoader().getResourceAsStream(path);
            OutputStream outStream = new FileOutputStream(file);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = initialStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            IOUtils.closeQuietly(initialStream);
            IOUtils.closeQuietly(outStream);
        }
    }

    public static @Nullable BufferedInputStream downloadFile(String url) throws IOException {
        return url != null ? new BufferedInputStream(new URL(url).openStream()) : null;
    }

    public static <T> T[] concatWithCollection(T[] array1, T[] array2) {
        List<T> resultList = new ArrayList<>(array1.length + array2.length);
        Collections.addAll(resultList, array1);
        Collections.addAll(resultList, array2);

        @SuppressWarnings("unchecked")
        //the type cast is safe as the array1 has the type T[]
        T[] resultArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), 0);
        return resultList.toArray(resultArray);
    }
}
