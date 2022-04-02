package cn.promptness.rpt.desktop.controller;

import cn.promptness.rpt.base.config.RemoteConfig;
import cn.promptness.rpt.base.protocol.ProxyType;
import cn.promptness.rpt.base.utils.Constants;
import cn.promptness.rpt.base.utils.StringUtils;
import cn.promptness.rpt.desktop.utils.SystemTrayUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.regex.Pattern;

public class ConfigController {

    private static final Pattern CHECK_REGEX = Pattern.compile("\\d*");
    private static final Pattern REPLACE_REGEX = Pattern.compile("[^\\d]");

    public static RemoteConfig buildDialog(String confirm, String headerTex, RemoteConfig remoteConfig) {
        ButtonType confirmButton = new ButtonType(confirm);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Constants.TITLE);
        dialog.setHeaderText(headerTex);
        dialog.initOwner(SystemTrayUtil.getPrimaryStage());
        dialog.getDialogPane().getButtonTypes().add(confirmButton);

        ComboBox<ProxyType> proxyType = new ComboBox<>(FXCollections.observableArrayList(ProxyType.values()));
        proxyType.setValue(remoteConfig.getProxyType());

        TextField localIp = new TextField(remoteConfig.getLocalIp());
        TextField localPort = new TextField(String.valueOf(remoteConfig.getLocalPort()));
        localPort.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!CHECK_REGEX.matcher(newValue).matches()) {
                localPort.setText(REPLACE_REGEX.matcher(newValue).replaceAll(""));
            }
        });

        TextField domain = new TextField(remoteConfig.getDomain());

        TextField remotePort = new TextField(String.valueOf(remoteConfig.getRemotePort()));
        remotePort.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!CHECK_REGEX.matcher(newValue).matches()) {
                remotePort.setText(REPLACE_REGEX.matcher(newValue).replaceAll(""));
            }
        });

        TextField description = new TextField(remoteConfig.getDescription());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Text("传输类型"), 0, 0);
        grid.add(proxyType, 1, 0);

        grid.add(new Text("本地地址"), 0, 1);
        grid.add(localIp, 1, 1);

        grid.add(new Text("本地端口"), 0, 2);
        grid.add(localPort, 1, 2);

        grid.add(new Text("暴露域名"), 0, 3);
        grid.add(domain, 1, 3);

        grid.add(new Text("暴露端口"), 0, 4);
        grid.add(remotePort, 1, 4);

        grid.add(new Text("备注"), 0, 5);
        grid.add(description, 1, 5);

        dialog.getDialogPane().setContent(grid);

        ButtonType buttonType = dialog.showAndWait().orElse(null);
        if (Objects.equals(buttonType, confirmButton)) {
            remoteConfig.setProxyType(proxyType.getValue());
            remoteConfig.setLocalIp(localIp.getText());
            remoteConfig.setLocalPort(StringUtils.hasText(localPort.getText()) ? Integer.parseInt(localPort.getText()) : 0);
            remoteConfig.setDomain(domain.getText());
            remoteConfig.setRemotePort(StringUtils.hasText(remotePort.getText()) ? Integer.parseInt(remotePort.getText()) : 0);
            remoteConfig.setDescription(description.getText());
            return remoteConfig;
        }
        return null;
    }

}
