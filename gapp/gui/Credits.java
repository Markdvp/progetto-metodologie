package gapp.gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;


import static gapp.gui.Variables.*;

/**
 * Pagina di ringraziamento per chi ha lavorato duramente a questo progetto #ENJOY
 */
public class Credits extends Pane {

    volatile boolean hasFinished = false;

    public Credits(){

        Text title = new Text("A.M.D presents");

        title.setTranslateX((MAIN_ROOT_WIDHT / 2) - 230);
        title.setTranslateY((MAIN_ROOT_HEIGHT / 2) + 20);
        title.setFont(new Font(MAIN_FONT.getName(),100));
        title.setFill(SCREEN_TITLE_DEFAULT_COLOR);

        Text descriptions = new Text("U.L.G");
        Text descriptions2 = new Text("Universal Library for Games");

        descriptions.setVisible(false);
        descriptions.setTranslateX((MAIN_ROOT_WIDHT / 2));
        descriptions.setTranslateY((MAIN_ROOT_HEIGHT / 2) - 155);
        descriptions.setFont(new Font(MAIN_FONT.getName(),100));
        descriptions.setFill(SCREEN_TITLE_DEFAULT_COLOR);

        descriptions2.setVisible(false);
        descriptions2.setTranslateX((MAIN_ROOT_WIDHT / 2) - 250);
        descriptions2.setTranslateY((MAIN_ROOT_HEIGHT / 2) - 40);
        descriptions2.setFont(new Font(MAIN_FONT.getName(),60));
        descriptions2.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

        Text descriptions3 = new Text("Product by :");
        Text descriptions4 = new Text("Andrea - Marco - Daniele");

        descriptions3.setVisible(false);
        descriptions3.setTranslateX((MAIN_ROOT_WIDHT / 2) + 10);
        descriptions3.setTranslateY((MAIN_ROOT_HEIGHT / 2) + 55);
        descriptions3.setFont(new Font(MAIN_FONT.getName(),40));
        descriptions3.setFill(SCREEN_TITLE_DEFAULT_COLOR);


        descriptions4.setVisible(false);
        descriptions4.setTranslateX((MAIN_ROOT_WIDHT / 2) - 110);
        descriptions4.setTranslateY((MAIN_ROOT_HEIGHT / 2) + 155);
        descriptions4.setFont(new Font(MAIN_FONT.getName(), 40));
        descriptions4.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);


        FadeTransition transition = new FadeTransition(Duration.seconds(2.0),title);
        transition.setFromValue(1);
        transition.setToValue(0);
        transition.play();

        getChildren().addAll(title,descriptions,descriptions2,descriptions3,descriptions4);

        transition.setOnFinished(event -> {
            descriptions.setVisible(true);
            descriptions2.setVisible(true);
            descriptions3.setVisible(true);
            descriptions4.setVisible(true);

            hasFinished = true;
        });

        Thread t = new Thread(() -> {

            while (!hasFinished) {}

            try {Thread.sleep(4000);} catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                menu.getRoot().getChildren().remove(this);
                menu.getRoot().getChildren().add(menu.getGameMenu());
            });
        });

        t.setDaemon(true);
        t.start();
    }
}
