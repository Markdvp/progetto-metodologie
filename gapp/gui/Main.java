package gapp.gui;

import javafx.scene.input.KeyCode;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static gapp.gui.Variables.*;

public class Main extends Application {

    public static void main(String... args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        menu = new GameMenu();
        Scene scene = new Scene(menu.getRoot(), MAIN_SCENE_WIDHT, MAIN_SCENE_HEIGHT);
        stage.setScene(scene);
        stage.setTitle(ULG_TITLE);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();

        menu.getGameMenu().setVisible(false);
        menu.getGameMenu().setTranslateX(-300);

        scene.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER) && menu.getRoot().getChildren().contains(menu.getGameMenu())) {

                if (menu.getGameMenu().isVisible()) {
                    menu.getGameMenu().hideMenu();
                    menu.getRoot().getChildren().add(textIntro);
                }

                else {
                    menu.getGameMenu().showMenu();
                    menu.getRoot().getChildren().remove(textIntro);
                }

            }

        });

    }

}
