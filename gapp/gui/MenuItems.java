package gapp.gui;

import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import static gapp.gui.Variables.*;

/**
 * Un oggetto {@code MenuItems} rappresenta una voce selezionabile in
 * un {@link MenuBox}
 */
public class MenuItems extends StackPane {

    /**
     * Crea un oggetto {@link MenuItems}
     * @param name il titolo della voce del menu
     */
    public MenuItems(String name) {

        Rectangle background = new Rectangle(300,24);

        LinearGradient backgroundColorGradient = new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
                new Stop(0,Color.BLACK),new Stop(0.2,Color.DARKGRAY));

        background.setFill(backgroundColorGradient);
        background.setVisible(false);
        background.setEffect(new DropShadow(5,0,5,Color.BLACK));

        Text nameText = new Text(name + "         ");
        nameText.setFill(Color.LIGHTGREY);
        nameText.setFont(MAIN_FONT);

        setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(background,nameText);

        setOnMouseEntered(event -> {
            background.setVisible(true);
            nameText.setFill(Color.WHITE);
        });

        setOnMouseExited(event -> {
            background.setVisible(false);
            nameText.setFill(Color.LIGHTGREY);
        });

        setOnMousePressed(event -> {
            background.setFill(Color.WHITE);
            nameText.setFill(Color.BLACK);
        });

        setOnMouseReleased(event -> {
            background.setFill(backgroundColorGradient);
            nameText.setFill(Color.WHITE);
        });

    }

}
