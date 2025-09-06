module dev.eministar.starsftp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    requires com.hierynomus.sshj;

    opens dev.eministar.starsftp to javafx.fxml;
    opens dev.eministar.starsftp.ui to javafx.fxml;
    opens dev.eministar.starsftp.model to com.fasterxml.jackson.databind;

    exports dev.eministar.starsftp;
}
