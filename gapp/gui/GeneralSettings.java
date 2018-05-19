package gapp.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static gapp.gui.Variables.*;

/**
 * Un {@code GeneralSetting} rappresenta la pagina di scelta dei settaggi generali
 * di un match e della GUI ancor prima di aver selezionato un gioco, ossia parametri
 * indipendenti dal gioco selezionato.
 * Si accede a questa pagina dal {@link GameMenu#subGameMenu}
 */
public class GeneralSettings {

    private VBox mainLayout;

    /**
     * Crea un {@link GeneralSettings} per scegliere i settaggi generali di un match o della GUI.
     */
    public GeneralSettings(){
        mainLayout = new VBox();

        VBox vBox = new VBox();

        vBox.setPadding(new Insets(0,0,0,10));

        HBox hBox = new HBox();

        hBox.setSpacing(10);
        vBox.setSpacing(10);

        Text maxBtitle = new Text("Generic time limits of match selection");
        maxBtitle.setFill(Color.RED);
        Text maxB = new Text("MAXBLOCKTIME : ");

        maxBtitle.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 15));
        maxB.setFont(Font.font(MAIN_FONT.getName(), MAIN_FONT.getSize()));


        maxB.setFill(Color.LIGHTGRAY);
        TextField maxBvalue = new TextField();

        Text maxBinfo = new Text(" Info : ( Default value - 1 )");
        maxBinfo.setFill(Color.LIGHTGRAY);
        maxBinfo.setFont(Font.font(MAIN_FONT.getName(), MAIN_FONT.getSize()));


        hBox.getChildren().addAll(maxB,maxBvalue,maxBinfo);
        vBox.getChildren().addAll(maxBtitle,hBox);



        Text fontTitle = new Text("Speed with which actions are drawn");
        fontTitle.setFill(Color.RED);
        fontTitle.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 15));


        VBox vBox1 = new VBox();
        vBox1.setPadding(new Insets(0,0,0,10));

        HBox hBox1 = new HBox();

        hBox1.setSpacing(10);
        vBox1.setSpacing(10);

        Text font = new Text("ACTIONSPEED : ");
        font.setFill(Color.LIGHTGRAY);
        font.setFont(Font.font(MAIN_FONT.getName(), MAIN_FONT.getSize()));


        TextField drawSpeedvalue = new TextField();

        Text fontInfo = new Text(" Info : ( Default value 500 )");
        fontInfo.setFill(Color.LIGHTGRAY);
        fontInfo.setFont(Font.font(MAIN_FONT.getName(), MAIN_FONT.getSize()));


        hBox1.getChildren().addAll(font,drawSpeedvalue,fontInfo);
        vBox.getChildren().addAll(fontTitle,hBox1);

        HBox buttonsBox = new HBox();

        Button apply = new Button("Apply");
        Button back = new Button("Back");

        back.setFont(MAIN_FONT);
        apply.setFont(MAIN_FONT);

        buttonsBox.getChildren().addAll(apply,back);
        buttonsBox.setSpacing(20);
        buttonsBox.setTranslateX(450);
        buttonsBox.setTranslateY(250);

        Text title = new Text("GENERAL SETTINGS");
        title.setTranslateX(300);
        title.setFont(Font.font(MAIN_FONT.getName(),40));
        title.setFill(Color.RED);

        mainLayout.getChildren().addAll(title,vBox,vBox1,buttonsBox);
        mainLayout.setSpacing(20);
        mainLayout.setPadding(new Insets(15,0,0,0));

        apply.setOnAction(event -> {

            if (!drawSpeedvalue.getText().equals(""))
                DRAW_ACTION_SPEED = Long.parseLong(drawSpeedvalue.getText());


            if (!maxBvalue.getText().equals(""))
                MAXBLOCKTIME = Integer.parseInt(maxBvalue.getText());

            menu.getRoot().getChildren().remove(mainLayout);
            menu.getRoot().getChildren().add(menu.getSubGameMenu());
        });

        back.setOnAction(event -> {
            menu.getRoot().getChildren().remove(mainLayout);
            menu.getRoot().getChildren().add(menu.getSubGameMenu());
        });

    }

    VBox getMainLayout() {
        return mainLayout;
    }

}
