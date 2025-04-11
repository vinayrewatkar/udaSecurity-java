module com.udacity.catpoint.security {
    requires transitive com.udacity.catpoint.image;
    requires transitive com.miglayout.swing;
    requires java.desktop;
    requires java.prefs;
    requires transitive com.google.gson;
    requires transitive dev.mccue.guava.collect;
    requires transitive dev.mccue.guava.reflect;
    requires com.google.common;

    opens com.udacity.catpoint.data;

}