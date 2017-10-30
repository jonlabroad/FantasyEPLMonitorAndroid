package com.jlabroad.eplfantasymatchtracker.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.jlabroad.eplfantasymatchtracker.config.GlobalConfig;

public class SimpleS3Provider {
    protected AmazonS3 _client;
    protected String _bucketName;

    public SimpleS3Provider() {
        init(GlobalConfig.S3Bucket);
    }

    public SimpleS3Provider(String bucketName) {
        init(bucketName);
    }

    private void init(String bucketName) {
        _bucketName = bucketName;
        _client = new AmazonS3Client(Credentials.instance().creds);
    }
}
