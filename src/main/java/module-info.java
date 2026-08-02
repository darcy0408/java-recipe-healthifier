module com.healthifier {
    requires java.net.http;
    requires jdk.httpserver;
    
    exports com.healthifier.ui;
    exports com.healthifier.domain;
    exports com.healthifier.application;
    exports com.healthifier.infrastructure;
}
