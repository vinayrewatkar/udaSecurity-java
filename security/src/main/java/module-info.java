module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires miglayout.swing;

    // Required for Guava
    requires jdk.unsupported;
    requires java.prefs;

    // Open packages for serialization
    opens com.udacity.catpoint.data to com.google.gson;

    // Open ALL packages unconditionally for testing
    opens com.udacity.catpoint.service;
    opens com.udacity.catpoint.application;
}