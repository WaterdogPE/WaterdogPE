/*
 * Copyright 2020 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package pe.waterdog.packs.types;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipResourcePack extends ResourcePack {

    private final ZipFile zipFile;
    private byte[] cachedHash;
    private byte[] cachedPack;

    public ZipResourcePack(Path file) throws IOException {
        super(file);
        try {
            this.zipFile = new ZipFile(this.packPath.toFile());
        } catch (IOException e) {
            throw new IOException("ResourcePack is not zip file!");
        }
    }

    public ZipEntry getZipEntry(Path path) {
        return this.zipFile.getEntry(path.toString());
    }

    @Override
    public InputStream getStream(Path path) throws IOException {
        ZipEntry entry = this.getZipEntry(path);
        if (entry == null) {
            return null;
        }
        return this.zipFile.getInputStream(entry);
    }

    @Override
    public void saveToCache() throws IOException {
        InputStream inputStream = Files.newInputStream(this.packPath);
        byte[] bytes = new byte[(int) Files.size(this.packPath)];
        DataInputStream dataStream = new DataInputStream(inputStream);
        dataStream.readFully(bytes);
        this.cachedPack = bytes;
    }

    @Override
    public InputStream getCachedPack() {
        return this.cachedPack == null ? null : new ByteArrayInputStream(this.cachedPack);
    }

    @Override
    public long getPackSize() {
        try {
            return Files.size(this.packPath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get size of pack", e);
        }
    }

    @Override
    public byte[] getHash() {
        if (this.cachedHash == null) {
            try {
                this.cachedHash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(this.packPath));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to get hash of pack", e);
            }
        }
        return this.cachedHash;
    }

    @Override
    public byte[] getChunk(int offset, int length) {
        byte[] chunkData = (this.getPackSize() - offset > length) ? new byte[length] : new byte[(int) (this.getPackSize() - offset)];
        InputStream inputStream = this.getCachedPack();

        try {
            if (inputStream == null) {
                inputStream = Files.newInputStream(this.packPath);
            }
            inputStream.skip(offset);
            inputStream.read(chunkData);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read pack chunk", e);
        }
        return chunkData;
    }
}
