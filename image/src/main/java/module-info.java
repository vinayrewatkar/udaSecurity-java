module com.udacity.catpoint.image {
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
    requires java.desktop;

    // For AWS SDK to work properly
    requires transitive java.logging;
    requires transitive java.net.http;
    requires transitive jdk.unsupported;
    requires org.slf4j;

    exports com.udacity.catpoint.image;
    opens com.udacity.catpoint.image to org.junit.platform.commons;
}