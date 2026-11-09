package com.examprep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level settings: where uploaded files live and how processing behaves.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private int maxUploadSizeMb = 25;
    private final Storage storage = new Storage();
    private final Processing processing = new Processing();

    public int getMaxUploadSizeMb() {
        return maxUploadSizeMb;
    }

    public void setMaxUploadSizeMb(int maxUploadSizeMb) {
        this.maxUploadSizeMb = maxUploadSizeMb;
    }

    public Storage getStorage() {
        return storage;
    }

    public Processing getProcessing() {
        return processing;
    }

    public static class Storage {
        /** Filesystem directory for uploaded documents. Never inside the web root. */
        private String location = "./data/uploads";

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    public static class Processing {
        /** In-progress jobs older than this are considered interrupted by a restart. */
        private int staleJobMinutes = 30;

        public int getStaleJobMinutes() {
            return staleJobMinutes;
        }

        public void setStaleJobMinutes(int staleJobMinutes) {
            this.staleJobMinutes = staleJobMinutes;
        }
    }
}
