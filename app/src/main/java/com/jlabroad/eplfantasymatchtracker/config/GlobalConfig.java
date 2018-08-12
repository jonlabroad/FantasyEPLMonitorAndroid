package com.jlabroad.eplfantasymatchtracker.config;

public class GlobalConfig {
    public static String S3Bucket = "fantasyeplmatchtracker";
    public static String PlatformApplicationId = "arn:aws:sns:us-east-1:796987500533:app/GCM/EPL_Fantasy_MatchTracker";

    public static String MatchInfoRootFmt = "data/Season2018/%d/%d/%d/MatchInfo";

    public static DeviceConfig deviceConfig;
    public static CloudAppConfig cloudAppConfig;
}
