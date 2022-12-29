/*
 * Copyright 2022 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.waterdogpe.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.waterdog.waterdogpe.packs.types.PackedVersion;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    public static final int INT_MEGABYTE = 1048576;
    public static final Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(PackedVersion.class, new PackedVersion.Serializer());
        builder.registerTypeAdapter(PackedVersion.class, new PackedVersion.Deserializer());
        GSON = builder.create();
    }

    public static void saveFromResources(String fileName, File targetFile) throws IOException {
        if (targetFile.exists()) {
            return;
        }
        targetFile.createNewFile();

        InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(fileName);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);

        OutputStream outputStream = new FileOutputStream(targetFile);
        outputStream.write(buffer);
        inputStream.close();
    }

    public static String readFile(File file) throws IOException {
        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException();
        }
        return readFile(new FileInputStream(file));
    }

    public static String readFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException();
        }
        return readFile(new FileInputStream(file));
    }

    public static String readFile(InputStream inputStream) throws IOException {
        return readFile(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    private static String readFile(Reader reader) throws IOException {
        try (BufferedReader br = new BufferedReader(reader)) {
            String temp;
            StringBuilder stringBuilder = new StringBuilder();
            temp = br.readLine();
            while (temp != null) {
                if (stringBuilder.length() != 0) {
                    stringBuilder.append("\n");
                }
                stringBuilder.append(temp);
                temp = br.readLine();
            }
            return stringBuilder.toString();
        }
    }

    public static void writeFile(String fileName, String content) throws IOException {
        writeFile(fileName, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    public static void writeFile(String fileName, InputStream content) throws IOException {
        writeFile(new File(fileName), content);
    }

    public static void writeFile(File file, String content) throws IOException {
        writeFile(file, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    public static void writeFile(File file, InputStream content) throws IOException {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null!");
        }

        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream stream = new FileOutputStream(file);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = content.read(buffer)) != -1) {
            stream.write(buffer, 0, length);
        }
        content.close();
        stream.close();
    }
}
