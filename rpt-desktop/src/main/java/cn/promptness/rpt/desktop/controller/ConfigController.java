package cn.promptness.rpt.desktop.controller;

import cn.promptness.rpt.base.config.ClientConfig;
import cn.promptness.rpt.base.config.RemoteConfig;
import cn.promptness.rpt.base.config.ProxyType;
import cn.promptness.rpt.desktop.utils.Constants;
import cn.promptness.rpt.base.utils.StringUtils;
import cn.promptness.rpt.desktop.utils.SystemTrayUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.Objects;
import java.util.regex.Pattern;

public class ConfigController {

    private static final Pattern CHECK_REGEX = Pattern.compile("\\d*");
    private static final Pattern REPLACE_REGEX = Pattern.compile("[^\\d]");

    public static Pair<ButtonType, Dialog<ButtonType>> buildDialog(String confirm, String headerTex) {
        ButtonType buttonType = new ButtonType(confirm);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Constants.TITLE);
        dialog.setHeaderText(headerTex);
        dialog.initOwner(SystemTrayUtil.getPrimaryStage());
        dialog.getDialogPane().getButtonTypes().add(buttonType);
        return new Pair<>(buttonType, dialog);
    }

    public static RemoteConfig buildDialog(String confirm, String headerTex, RemoteConfig remoteConfig) {
        Pair<ButtonType, Dialog<ButtonType>> pair = buildDialog(confirm, headerTex);

        TextField localIp = new TextField(remoteConfig.getLocalIp());
        TextField localPort = new TextField(String.valueOf(remoteConfig.getLocalPort()));
        localPort.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!CHECK_REGEX.matcher(newValue).matches()) {
                localPort.setText(REPLACE_REGEX.matcher(newValue).replaceAll(""));
            }
        });

        TextField domain = new TextField(remoteConfig.getDomain());
        domain.setPrefWidth(300);
        TextField token = new TextField(remoteConfig.getToken());
        token.setPrefWidth(300);

        TextField remotePort = new TextField(String.valueOf(remoteConfig.getRemotePort()));
        remotePort.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!CHECK_REGEX.matcher(newValue).matches()) {
                remotePort.setText(REPLACE_REGEX.matcher(newValue).replaceAll(""));
            }
        });

        ComboBox<ProxyType> proxyType = new ComboBox<>(FXCollections.observableArrayList(ProxyType.values()));
        proxyType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            domain.setDisable(false);
            token.setDisable(false);
            remotePort.setDisable(false);
            if (Objects.equals(ProxyType.TCP, newValue)) {
                domain.setDisable(true);
                token.setDisable(true);
                domain.setText("");
                token.setText("");
            }
            if (Objects.equals(ProxyType.HTTP, newValue)) {
                remotePort.setDisable(true);
                remotePort.setText("");
            }
        });
        proxyType.setValue(remoteConfig.getProxyType());

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

        grid.add(new Text("访问账户"), 0, 4);
        grid.add(token, 1, 4);

        grid.add(new Text("暴露端口"), 0, 5);
        grid.add(remotePort, 1, 5);

        grid.add(new Text("备注"), 0, 6);
        grid.add(description, 1, 6);

        pair.getValue().getDialogPane().setContent(grid);

        ButtonType buttonType = pair.getValue().showAndWait().orElse(null);
        if (Objects.equals(buttonType, pair.getKey())) {
            remoteConfig.setProxyType(proxyType.getValue());
            remoteConfig.setLocalIp(localIp.getText());
            remoteConfig.setLocalPort(StringUtils.hasText(localPort.getText()) ? Integer.parseInt(localPort.getText()) : 0);
            remoteConfig.setDomain(domain.getText());
            remoteConfig.setToken(token.getText());
            remoteConfig.setRemotePort(StringUtils.hasText(remotePort.getText()) ? Integer.parseInt(remotePort.getText()) : 0);
            remoteConfig.setDescription(description.getText());
            return remoteConfig;
        }
        return null;
    }

    public static ClientConfig buildDialog(String confirm, String headerTex, ClientConfig clientConfig) {

        Pair<ButtonType, Dialog<ButtonType>> pair = buildDialog(confirm, headerTex);

        TextField serverIp = new TextField(clientConfig.getServerIp());
        TextField serverPort = new TextField(String.valueOf(clientConfig.getServerPort()));
        serverPort.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!CHECK_REGEX.matcher(newValue).matches()) {
                serverPort.setText(REPLACE_REGEX.matcher(newValue).replaceAll(""));
            }
        });
        TextField clientKey = new TextField(clientConfig.getClientKey());
        clientKey.setPrefWidth(300);

        GridPane grid = new GridPane();
        grid.setMinWidth(300);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Text("服务端地址"), 0, 0);
        grid.add(serverIp, 1, 0);

        grid.add(new Text("服务端端口"), 0, 1);
        grid.add(serverPort, 1, 1);

        grid.add(new Text("连接秘钥"), 0, 3);
        grid.add(clientKey, 1, 3);

        pair.getValue().getDialogPane().setContent(grid);

        ButtonType buttonType = pair.getValue().showAndWait().orElse(null);
        if (Objects.equals(pair.getKey(), buttonType)) {
            clientConfig.setServerIp(serverIp.getText());
            clientConfig.setServerPort(StringUtils.hasText(serverPort.getText()) ? Integer.parseInt(serverPort.getText()) : 0);
            clientConfig.setClientKey(clientKey.getText());
            return clientConfig;
        }
        return null;
    }
}
