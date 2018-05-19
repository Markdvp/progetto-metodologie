package gapp.gui;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static gapp.gui.Variables.*;

/**
 * Un oggetto {@code MenuBox} rappresenta un menu di selezione grafico.
 */
public class MenuBox extends StackPane {

    /**
     * Il nodo principale della classe
     */
    private VBox mainLayout;

    /**
     * Crea un oggetto {@link MenuBox} per creare un menu di selezione
     * @param title il titolo del menu
     * @param items le voci del menu
     */
    public MenuBox(String title,MenuItems...items) {
        Rectangle background = new Rectangle(310,600);
        background.setFill(Color.GREY);
        background.setOpacity(0.2);

        DropShadow shadow = new DropShadow(7,5,0,Color.BLACK);
        shadow.setSpread(0.2);

        background.setEffect(shadow);

        Text gameTitle = new Text(title + "   ");
        gameTitle.setFont(MAIN_FONT);
        gameTitle.setFill(Color.LIGHTGRAY);

        Line endline = new Line();
        endline.setStartX(20);
        endline.setEndX(328);
        endline.setStroke(Color.LIGHTGRAY);
        endline.setOpacity(0.4);

        Line startline = new Line();
        startline.setStartX(300);
        startline.setEndX(300);
        startline.setEndY(600);
        startline.setStroke(Color.LIGHTGRAY);
        startline.setOpacity(0.4);

        mainLayout = new VBox();
        mainLayout.setSpacing(10);
        mainLayout.setAlignment(Pos.TOP_RIGHT);
        mainLayout.setPadding(new Insets(30,0,0,0));
        mainLayout.getChildren().addAll(gameTitle,endline);
        mainLayout.getChildren().addAll(items);

        setAlignment(Pos.TOP_RIGHT);
        getChildren().addAll(background,startline,mainLayout);
    }

    /**
     * Se questo metodo viene invocato ed il menu non è visibile lo rende visibile
     */
    void showMenu(){
        setVisible(true);
        TranslateTransition slideTransition = new TranslateTransition(Duration.seconds(0.5),this);
        slideTransition.setToX(0);
        slideTransition.play();
    }

    /**
     * Se questo metodo viene invocato ed il menu è visibile lo rende invisibile
     */
    void hideMenu(){
        TranslateTransition slideTransition = new TranslateTransition(Duration.seconds(0.5),this);
        slideTransition.setToX(-300);
        slideTransition.setOnFinished(event -> setVisible(false));
        slideTransition.play();
    }
}

