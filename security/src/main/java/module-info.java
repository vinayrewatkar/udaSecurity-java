module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires miglayout.swing;

    // Required for Guava
    requires jdk.unsupported;
    requires java.prefs;
    opens com.udacity.catpoint.data to com.google.gson;
}