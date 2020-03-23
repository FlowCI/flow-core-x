/*
 * Copyright 2019 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.store;

import io.minio.MinioClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Minio storage manager,
 * instance created on @see com.flowci.core.common.config.StorageConfig
 */
public class MinioFileManager implements FileManager {

    private static final String Separator = "/";

    private final String bucket;

    private final MinioClient minioClient;

    public MinioFileManager(MinioClient minioClient, String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /**
     * Create directories
     * First Pathable is bucket, rest of them is object with name "x/y/z/"
     */
    @Override
    public String create(Pathable... objs) throws IOException {
        //  create bucket only
        try {
            String bucketName = initBucket();
            String objectName = getObjectName(objs);
            return bucketName + Separator + objectName;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public boolean exist(Pathable... objs) {
        try {
            if (!minioClient.bucketExists(bucket)) {
                return false;
            }

            String objectName = getObjectName(objs);
            minioClient.statObject(bucket, objectName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean exist(String fileName, Pathable... objs) {
        try {
            if (!minioClient.bucketExists(bucket)) {
                return false;
            }

            String objectName = getObjectName(objs) + fileName;
            minioClient.statObject(bucket, objectName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String save(String fileName, InputStream data, Pathable... objs) throws IOException {
        try {
            String bucketName = initBucket();
            String objectName = getObjectName(objs) + fileName;
            minioClient.putObject(bucketName, objectName, data, null, null, null, null);
            return bucketName + Separator + objectName;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public InputStream read(String fileName, Pathable... objs) throws IOException {
        try {
            String objectName = getObjectName(objs) + fileName;
            return minioClient.getObject(bucket, objectName);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public String remove(String fileName, Pathable... objs) throws IOException {
        try {
            String objectName = getObjectName(objs) + fileName;
            minioClient.removeObject(bucket, objectName);
            return bucket + Separator + objectName;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public String remove(String filePath) throws IOException {
        try {
            minioClient.removeObject(bucket, filePath);
            return filePath;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    private String initBucket() throws Exception {
        if (!minioClient.bucketExists(bucket)) {
            minioClient.makeBucket(bucket);
        }
        return bucket;
    }

    private static String getObjectName(Pathable... objs) {
        StringBuilder builder = new StringBuilder();
        for (Pathable item : objs) {
            builder.append(item.pathName()).append(Separator);
        }
        return builder.toString();
    }
}
