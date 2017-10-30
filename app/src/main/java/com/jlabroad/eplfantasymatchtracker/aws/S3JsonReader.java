package com.jlabroad.eplfantasymatchtracker.aws;

import com.amazonaws.services.s3.model.S3Object;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class S3JsonReader extends SimpleS3Provider {
    public <T> T read(String keyName, Class<T> cls) {
        if (_client.doesObjectExist(_bucketName, keyName)) {
            S3Object s3Obj = _client.getObject(_bucketName, keyName);
            String json = null;
            try {
                json = readObject(s3Obj);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new Gson().fromJson(json, cls);
        }
        return null;
    }

    public boolean doesObjectExist(String key) {
        return _client.doesObjectExist(_bucketName, key);
    }

    public String readObject(S3Object obj) throws IOException {
        String out = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getObjectContent()));
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;

            out += line;
        }
        return out;
    }
}
