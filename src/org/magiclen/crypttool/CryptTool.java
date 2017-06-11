/*
 *
 * Copyright 2015-2017 magiclen.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magiclen.crypttool;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.magiclen.magiccrypt.MagicCrypt;
import org.magiclen.magiccrypt.lib.Crypt;
import org.magiclen.magicdialog.Dialogs;

/**
 * Crypt Tool.
 *
 * @author Magic Len
 */
public class CryptTool extends Application {

    // -----Class Constant-----
    private static final int SCREEN_WIDTH, SCREEN_HEIGHT;

    // -----Initial Static-----
    static {
        final Screen mainScreen = Screen.getPrimary();
        final Rectangle2D screenRectangle = mainScreen.getBounds();
        SCREEN_WIDTH = (int) screenRectangle.getWidth();
        SCREEN_HEIGHT = (int) screenRectangle.getHeight();
    }

    // -----Object Constant-----
    /**
     * The default value of width.
     */
    private final int WIDTH = 680;
    /**
     * The default value of Height.
     */
    private final int HEIGHT = 540;
    /**
     * The default distance of controls.
     */
    private final int GAP = 3;
    /**
     * The default padding of the main frame.
     */
    private final int PADDING_GAP = 10;
    /**
     * The default size of text.
     */
    private final float FONT_SIZE = 18f;
    /**
     * The default height of progress bar.
     */
    private final float PROGRESS_HEIGHT = 50;

    private final Border encryptBorder = new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));
    private final Border decryptBorder = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));

    // -----Object Variable-----
    /**
     * The font of text.
     */
    private Font font;
    /**
     * The margin of controls.
     */
    private Insets insets;
    /**
     * The padding of the main frame.
     */
    private Insets padding;
    /**
     * The stage of this application.
     */
    private Stage MAIN_STAGE;
    /**
     * The scene of this application.
     */
    private Scene MAIN_SCENE;
    /**
     * The root panel of controls.
     */
    private BorderPane MAIN_ROOT;
    private TextArea taTextSource, taTextDestination;
    private TextField tfTextKey, tfTextIV, tfFileKey, tfFileIV, tfFileSource, tfFileDestination;
    private Button bCopy, bStartOrStop;
    private RadioButton rbTextAuto, rbTextEnc, rbTextDec, rbText64, rbText128, rbText192, rbText256, rbFileAuto, rbFileEnc, rbFileDec, rbFile64, rbFile128, rbFile192, rbFile256;
    private ToggleGroup tgTextMethod, tgTextKeyLength, tgFileMethod, tgFileKeyLength;
    private VBox vbText, vbFile;
    private HBox hbTextMethod, hbTextBits, hbFileMethod, hbFileBits;
    private Label lTextMethod, lTextKey, lFileMethod, lFileKey, lFileSource, lFileDestination, lAuthor;
    private TabPane tpMain;
    private Tab tText, tFile;
    private ProgressBar pbProgress;
    private FileChooser fcChooser;

    private boolean runningFile = false, stoppingFile = false;

    // -----Object Method-----
    /**
     * Lock or unclock controls.
     *
     * @param disable whether to disable controls
     */
    private void lockOrUnlock(final boolean disable) {
        tfFileSource.setDisable(disable);
        tfFileDestination.setDisable(disable);
        tfFileKey.setDisable(disable);
        tfFileIV.setDisable(disable);
        rbFileAuto.setDisable(disable);
        rbFileEnc.setDisable(disable);
        rbFileDec.setDisable(disable);
        rbFile64.setDisable(disable);
        rbFile128.setDisable(disable);
        rbFile192.setDisable(disable);
        rbFile256.setDisable(disable);
        bStartOrStop.setText(disable ? "Stop" : "Start");
    }

    /**
     * Encrypt or decrypt a file.
     */
    private void handleFile() {
        if (runningFile) {
            stoppingFile = true;
            return;
        }
        runningFile = true;
        stoppingFile = false;
        lockOrUnlock(true);
        pbProgress.setProgress(-1);

        final String sourcePath = tfFileSource.getText().trim();
        final String destinationPath = tfFileDestination.getText().trim();

        final String key = tfFileKey.getText().trim();
        final String iv = tfFileIV.getText();

        final Integer rbTextbits = (Integer) tgFileKeyLength.getSelectedToggle().getUserData();

        taTextSource.setBorder(null);
        taTextDestination.setBorder(null);

        try {
            if (sourcePath.length() == 0) {
                Dialogs.create().type(Dialogs.Type.INFORMATION).title("HINT").message("You need to input the path of source file.").showAndWait();
                throw new Exception();
            }
            if (destinationPath.length() == 0) {
                Dialogs.create().type(Dialogs.Type.INFORMATION).title("HINT").message("You need to input the path of destination file.").showAndWait();
                throw new Exception();
            }

            final File source = new File(sourcePath).getAbsoluteFile();
            if (!source.exists() || source.isDirectory()) {
                Dialogs.create().type(Dialogs.Type.INFORMATION).title("HINT").message("You need to input the path of source file correctly.").showAndWait();
                throw new Exception();
            }

            final File destination = new File(destinationPath).getAbsoluteFile();
            if (destination.exists()) {
                if (destination.isDirectory()) {
                    Dialogs.create().type(Dialogs.Type.INFORMATION).title("HINT").message("You need to input the path of destination file correctly.").showAndWait();
                    throw new Exception();
                } else if (destination.equals(source)) {
                    Dialogs.create().type(Dialogs.Type.WARNING).title("WARNING").message("The destination and source files must be different.").showAndWait();
                    throw new Exception();
                } else {
                    final ButtonType rtn = Dialogs.create().type(Dialogs.Type.QUESTION).title("QUESTION").header("Destination file exists.").message("Do you want to overwrite the original file?").showAndWait();
                    if (rtn != ButtonType.OK) {
                        throw new Exception();
                    }
                }
            }

            final MagicCrypt mc = new MagicCrypt(key, rbTextbits, iv.length() == 0 ? null : iv.trim());
            final Crypt.CryptListener listener = new Crypt.CryptListener() {

                @Override
                public void onStarted(final long totalBytes) {
                    Platform.runLater(() -> {
                        pbProgress.setProgress(-1);
                    });
                }

                @Override
                public boolean onRunning(final long currentBytes, final long totalBytes) {
                    if (totalBytes <= 0) {
                        Platform.runLater(() -> {
                            pbProgress.setProgress(-1);
                        });
                    } else {
                        Platform.runLater(() -> {
                            pbProgress.setProgress(currentBytes * 1f / totalBytes);
                        });
                    }
                    return !stoppingFile;
                }

                @Override
                public void onFinished(final long finishedBytes, final long totalBytes) {
                    if (stoppingFile) {
                        destination.delete();
                    } else {
                        Platform.runLater(() -> {
                            pbProgress.setProgress(1);
                        });
                    }
                }
            };
            new Thread(() -> {
                int encIndex = 2;
                try {
                    switch ((String) tgFileMethod.getSelectedToggle().getUserData()) {
                        case "Auto":
                            try {
                                mc.decrypt(source, destination, listener);
                                encIndex = 0;
                            } catch (final Exception ex) {
                                mc.encrypt(source, destination, listener);
                                encIndex = 1;
                            }
                            break;
                        case "Encrypt":
                            mc.encrypt(source, destination, listener);
                            encIndex = 1;
                            break;
                        case "Decrypt":
                            mc.decrypt(source, destination, listener);
                            encIndex = 0;
                            break;
                    }
                } catch (final Exception ex) {
                    final String msg = ex.getMessage();
                    Dialogs.create().type(Dialogs.Type.WARNING).title("WARNING").message(msg).showAndWait();
                }

                final int fixedEncIndex = encIndex;
                Platform.runLater(() -> {
                    switch (fixedEncIndex) {
                        case 0:
                            tfFileSource.setBorder(encryptBorder);
                            tfFileDestination.setBorder(decryptBorder);
                            break;
                        case 1:
                            tfFileSource.setBorder(decryptBorder);
                            tfFileDestination.setBorder(encryptBorder);
                            break;
                    }

                    if (stoppingFile) {
                        Dialogs.create().type(Dialogs.Type.INFORMATION).title("HINT").message("Stopped.").showAndWait();
                    } else {
                        Dialogs.create().type(Dialogs.Type.INFORMATION).title("HINT").message("Finished.").showAndWait();
                    }
                    lockOrUnlock(false);
                    runningFile = false;
                });
            }).start();
        } catch (final Exception ex) {
            lockOrUnlock(false);
            runningFile = false;
            pbProgress.setProgress(0);
        }
    }

    /**
     * Encrypt or decrypt text.
     */
    private void handleText() {
        final String text = taTextSource.getText().trim();
        final String key = tfTextKey.getText().trim();
        final String iv = tfTextIV.getText();

        final Integer rbTextbits = (Integer) tgTextKeyLength.getSelectedToggle().getUserData();

        int encIndex = 2;
        final MagicCrypt mc = new MagicCrypt(key, rbTextbits, iv.length() == 0 ? null : iv.trim());
        try {
            switch ((String) tgTextMethod.getSelectedToggle().getUserData()) {
                case "Auto":
                    try {
                        taTextDestination.setText(mc.decrypt(text));
                        encIndex = 0;
                    } catch (final Exception ex) {
                        taTextDestination.setText(mc.encrypt(text));
                        encIndex = 1;
                    }
                    break;
                case "Encrypt":
                    taTextDestination.setText(mc.encrypt(text));
                    encIndex = 1;
                    break;
                case "Decrypt":
                    taTextDestination.setText(mc.decrypt(text));
                    encIndex = 0;
                    break;
            }
        } catch (final Exception ex) {
            final String msg = ex.getMessage();
            if (msg.contains("Illegal key size")) {
                Dialogs.create().type(Dialogs.Type.WARNING).title("WARNING").message("You may need Java Cryptography Extension(JCE) to encrypt or decrypt.").showAndWait();
            } else {
                Dialogs.create().type(Dialogs.Type.WARNING).title("WARNING").message(msg).showAndWait();
            }
        }

        taTextSource.setBorder(null);
        taTextDestination.setBorder(null);
        switch (encIndex) {
            case 0:
                taTextSource.setBorder(encryptBorder);
                taTextDestination.setBorder(decryptBorder);
                break;
            case 1:
                taTextSource.setBorder(decryptBorder);
                taTextDestination.setBorder(encryptBorder);
                break;
        }
    }

    /**
     * Choose a file to load.
     */
    private void chooseInputFile() {
        fcChooser.setTitle("Choose a file to encrypt or decrypt.");
        final File file = fcChooser.showOpenDialog(MAIN_STAGE);
        if (file != null) {
            fcChooser.setInitialDirectory(file.getParentFile());
            tfFileSource.setText(file.getAbsolutePath());
        }
    }

    /**
     * Choose a file to save.
     */
    private void chooseOutputFile() {
        fcChooser.setTitle("Choose a file to save.");
        final File file = fcChooser.showSaveDialog(MAIN_STAGE);
        if (file != null) {
            fcChooser.setInitialDirectory(file.getParentFile());
            tfFileDestination.setText(file.getAbsolutePath());
        }
    }

    /**
     * Add events.
     */
    private void addActions() {
        bCopy.setOnAction((e) -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(taTextDestination.getText());
            clipboard.setContent(content);
        });

        taTextSource.textProperty().addListener((e) -> {
            handleText();
        });

        tfTextKey.textProperty().addListener((e) -> {
            handleText();
        });

        tfTextIV.textProperty().addListener((e) -> {
            handleText();
        });

        tgTextMethod.selectedToggleProperty().addListener((e) -> {
            handleText();
        });

        tgTextKeyLength.selectedToggleProperty().addListener((e) -> {
            handleText();
        });

        tfFileSource.setOnMouseClicked(e -> {
            if (e.getClickCount() == 3) {
                chooseInputFile();
            }
        });
        tfFileDestination.setOnMouseClicked(e -> {
            if (e.getClickCount() == 3) {
                chooseOutputFile();
            }
        });

        tfFileSource.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                chooseInputFile();
            }
        });
        tfFileDestination.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                chooseOutputFile();
            }
        });

        bStartOrStop.setOnAction(e -> {
            handleFile();
        });

        lAuthor.setOnMouseClicked((e) -> {
            final URI uri = URI.create("https://magiclen.org/");
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(uri);
                } catch (final IOException ex) {
                }
            }).start();
        });
    }

    /**
     * Construct the primary stage.
     *
     * @param primaryStage JavaFX will input a stage instance here
     */
    @Override
    public void start(final Stage primaryStage) {
        font = new Font(FONT_SIZE);
        insets = new Insets(GAP, GAP, GAP, GAP);
        padding = new Insets(PADDING_GAP, PADDING_GAP, PADDING_GAP, PADDING_GAP);

        fcChooser = new FileChooser();

        taTextSource = new TextArea();
        taTextDestination = new TextArea();

        taTextSource.setFont(font);
        taTextDestination.setFont(font);

        taTextDestination.setEditable(false);

        lTextMethod = new Label("Method: ");
        lTextKey = new Label("Key Strength: ");
        lFileMethod = new Label("Method: ");
        lFileKey = new Label("Key Strength: ");
        lFileSource = new Label("Source File: ");
        lFileDestination = new Label("Destination File: ");
        lAuthor = new Label("Powered by magiclen.org");

        lTextMethod.setFont(font);
        lTextKey.setFont(font);
        lFileMethod.setFont(font);
        lFileKey.setFont(font);
        lFileSource.setFont(font);
        lFileDestination.setFont(font);
        lAuthor.setFont(font);

        lAuthor.setAlignment(Pos.BASELINE_RIGHT);
        lAuthor.setMaxWidth(Integer.MAX_VALUE);

        rbTextAuto = new RadioButton("Auto");
        rbTextEnc = new RadioButton("Encrypt");
        rbTextDec = new RadioButton("Decrypt");
        rbText64 = new RadioButton("64 bits");
        rbText128 = new RadioButton("128 bits");
        rbText192 = new RadioButton("192 bits");
        rbText256 = new RadioButton("256 bits");
        rbFileAuto = new RadioButton("Auto");
        rbFileEnc = new RadioButton("Encrypt");
        rbFileDec = new RadioButton("Decrypt");
        rbFile64 = new RadioButton("64 bits");
        rbFile128 = new RadioButton("128 bits");
        rbFile192 = new RadioButton("192 bits");
        rbFile256 = new RadioButton("256 bits");

        rbTextAuto.setFont(font);
        rbTextEnc.setFont(font);
        rbTextDec.setFont(font);
        rbText64.setFont(font);
        rbText128.setFont(font);
        rbText192.setFont(font);
        rbText256.setFont(font);
        rbFileAuto.setFont(font);
        rbFileEnc.setFont(font);
        rbFileDec.setFont(font);
        rbFile64.setFont(font);
        rbFile128.setFont(font);
        rbFile192.setFont(font);
        rbFile256.setFont(font);

        rbTextAuto.setUserData("Auto");
        rbTextEnc.setUserData("Encrypt");
        rbTextDec.setUserData("Decrypt");
        rbText64.setUserData(64);
        rbText128.setUserData(128);
        rbText192.setUserData(192);
        rbText256.setUserData(256);
        rbFileAuto.setUserData("Auto");
        rbFileEnc.setUserData("Encrypt");
        rbFileDec.setUserData("Decrypt");
        rbFile64.setUserData(64);
        rbFile128.setUserData(128);
        rbFile192.setUserData(192);
        rbFile256.setUserData(256);

        tgTextMethod = new ToggleGroup();
        tgTextKeyLength = new ToggleGroup();
        tgFileMethod = new ToggleGroup();
        tgFileKeyLength = new ToggleGroup();

        rbTextAuto.setToggleGroup(tgTextMethod);
        rbTextEnc.setToggleGroup(tgTextMethod);
        rbTextDec.setToggleGroup(tgTextMethod);
        rbText64.setToggleGroup(tgTextKeyLength);
        rbText128.setToggleGroup(tgTextKeyLength);
        rbText192.setToggleGroup(tgTextKeyLength);
        rbText256.setToggleGroup(tgTextKeyLength);
        rbFileAuto.setToggleGroup(tgFileMethod);
        rbFileEnc.setToggleGroup(tgFileMethod);
        rbFileDec.setToggleGroup(tgFileMethod);
        rbFile64.setToggleGroup(tgFileKeyLength);
        rbFile128.setToggleGroup(tgFileKeyLength);
        rbFile192.setToggleGroup(tgFileKeyLength);
        rbFile256.setToggleGroup(tgFileKeyLength);

        rbTextAuto.setMaxWidth(Integer.MAX_VALUE);
        rbTextEnc.setMaxWidth(Integer.MAX_VALUE);
        rbTextDec.setMaxWidth(Integer.MAX_VALUE);
        rbText64.setMaxWidth(Integer.MAX_VALUE);
        rbText128.setMaxWidth(Integer.MAX_VALUE);
        rbText192.setMaxWidth(Integer.MAX_VALUE);
        rbText256.setMaxWidth(Integer.MAX_VALUE);
        rbFileAuto.setMaxWidth(Integer.MAX_VALUE);
        rbFileEnc.setMaxWidth(Integer.MAX_VALUE);
        rbFileDec.setMaxWidth(Integer.MAX_VALUE);
        rbFile64.setMaxWidth(Integer.MAX_VALUE);
        rbFile128.setMaxWidth(Integer.MAX_VALUE);
        rbFile192.setMaxWidth(Integer.MAX_VALUE);
        rbFile256.setMaxWidth(Integer.MAX_VALUE);

        rbTextEnc.setSelected(true);
        rbText128.setSelected(true);
        rbFileEnc.setSelected(true);
        rbFile128.setSelected(true);

        tfTextKey = new TextField();
        tfTextIV = new TextField();
        tfFileKey = new TextField();
        tfFileIV = new TextField();
        tfFileSource = new TextField();
        tfFileDestination = new TextField();

        tfTextKey.setFont(font);
        tfTextIV.setFont(font);
        tfFileKey.setFont(font);
        tfFileIV.setFont(font);
        tfFileSource.setFont(font);
        tfFileDestination.setFont(font);

        tfTextKey.setPromptText("Key");
        tfTextIV.setPromptText("IV(0)");
        tfFileKey.setPromptText("Key");
        tfFileIV.setPromptText("IV(0)");
        tfFileSource.setPromptText("Click here 3 times or press enter to choose a file to load.");
        tfFileDestination.setPromptText("Click here 3 times or press enter to choose a file to save.");

        bCopy = new Button("Copy");
        bStartOrStop = new Button("Start");

        bCopy.setFont(font);
        bStartOrStop.setFont(font);

        bCopy.setMaxWidth(Integer.MAX_VALUE);
        bStartOrStop.setMaxSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

        pbProgress = new ProgressBar(0);
        pbProgress.setMaxSize(Double.MAX_VALUE, PROGRESS_HEIGHT);

        final Tooltip tipAuthor = new Tooltip("Magic Len");
        tipAuthor.setFont(font);
        Tooltip.install(lAuthor, tipAuthor);

        HBox.setHgrow(rbTextAuto, Priority.ALWAYS);
        HBox.setHgrow(rbTextEnc, Priority.ALWAYS);
        HBox.setHgrow(rbTextDec, Priority.ALWAYS);
        HBox.setHgrow(rbText64, Priority.ALWAYS);
        HBox.setHgrow(rbText128, Priority.ALWAYS);
        HBox.setHgrow(rbText192, Priority.ALWAYS);
        HBox.setHgrow(rbText256, Priority.ALWAYS);
        HBox.setHgrow(rbFileAuto, Priority.ALWAYS);
        HBox.setHgrow(rbFileEnc, Priority.ALWAYS);
        HBox.setHgrow(rbFileDec, Priority.ALWAYS);
        HBox.setHgrow(rbFile64, Priority.ALWAYS);
        HBox.setHgrow(rbFile128, Priority.ALWAYS);
        HBox.setHgrow(rbFile192, Priority.ALWAYS);
        HBox.setHgrow(rbFile256, Priority.ALWAYS);

        hbTextMethod = new HBox();
        hbTextBits = new HBox();
        hbFileMethod = new HBox();
        hbFileBits = new HBox();

        hbTextMethod.setMaxWidth(Integer.MAX_VALUE);
        hbTextBits.setMaxWidth(Integer.MAX_VALUE);
        hbFileMethod.setMaxWidth(Integer.MAX_VALUE);
        hbFileBits.setMaxWidth(Integer.MAX_VALUE);

        hbTextMethod.getChildren().addAll(lTextMethod, rbTextEnc, rbTextDec, rbTextAuto);
        hbTextBits.getChildren().addAll(lTextKey, rbText64, rbText128, rbText192, rbText256);
        hbFileMethod.getChildren().addAll(lFileMethod, rbFileEnc, rbFileDec, rbFileAuto);
        hbFileBits.getChildren().addAll(lFileKey, rbFile64, rbFile128, rbFile192, rbFile256);

        VBox.setVgrow(taTextSource, Priority.ALWAYS);
        VBox.setVgrow(taTextDestination, Priority.ALWAYS);
        VBox.setVgrow(bCopy, Priority.SOMETIMES);
        VBox.setVgrow(bStartOrStop, Priority.ALWAYS);
        VBox.setVgrow(pbProgress, Priority.ALWAYS);

        VBox.setMargin(taTextSource, insets);
        VBox.setMargin(taTextDestination, insets);
        VBox.setMargin(bCopy, insets);
        VBox.setMargin(hbTextMethod, insets);
        VBox.setMargin(hbTextBits, insets);
        VBox.setMargin(tfTextKey, insets);
        VBox.setMargin(tfTextIV, insets);
        VBox.setMargin(lFileSource, insets);
        VBox.setMargin(tfFileSource, insets);
        VBox.setMargin(lFileDestination, insets);
        VBox.setMargin(tfFileDestination, insets);
        VBox.setMargin(bStartOrStop, insets);
        VBox.setMargin(hbFileMethod, insets);
        VBox.setMargin(hbFileBits, insets);
        VBox.setMargin(tfFileKey, insets);
        VBox.setMargin(tfFileIV, insets);
        VBox.setMargin(pbProgress, insets);

        vbText = new VBox();
        vbFile = new VBox();

        vbText.setPadding(padding);
        vbFile.setPadding(padding);

        vbText.getChildren().addAll(taTextSource, taTextDestination, bCopy, hbTextMethod, hbTextBits, tfTextKey, tfTextIV);
        vbFile.getChildren().addAll(lFileSource, tfFileSource, lFileDestination, tfFileDestination, bStartOrStop, pbProgress, hbFileMethod, hbFileBits, tfFileKey, tfFileIV);

        tText = new Tab();
        tFile = new Tab();

        final String tabStyle = String.format("-fx-font-size: %.0fpx;", FONT_SIZE);
        tText.setStyle(tabStyle);
        tFile.setStyle(tabStyle);

        tText.setClosable(false);
        tFile.setClosable(false);

        tText.setContent(vbText);
        tFile.setContent(vbFile);

        tText.setText("Text");
        tFile.setText("File");

        tpMain = new TabPane();
        tpMain.getTabs().addAll(tText, tFile);

        BorderPane.setMargin(lAuthor, padding);

        MAIN_ROOT = new BorderPane(tpMain);
        MAIN_ROOT.setBottom(lAuthor);

        MAIN_SCENE = new Scene(MAIN_ROOT, WIDTH, HEIGHT);

        primaryStage.setResizable(true);
        primaryStage.setTitle("Crypt Tool");
        primaryStage.setScene(MAIN_SCENE);
        primaryStage.setX((SCREEN_WIDTH - WIDTH) / 2);
        primaryStage.setY((SCREEN_HEIGHT - HEIGHT) / 2);

        MAIN_STAGE = primaryStage;

        primaryStage.show();

        addActions();
    }

    /**
     * The initiation of this program.
     *
     * @param args not used
     */
    public static void main(final String[] args) {
        launch(args);
    }

}
