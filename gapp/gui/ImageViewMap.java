package gapp.gui;

import javafx.scene.image.ImageView;
import javafx.scene.shape.Shape;

import gapp.ulg.game.board.Pos;

/**
 * Un {@code ImageViewMap} è un oggetto che mantiene, data una {@link Pos},
 * la x e la y dell'angolo superiore sinistro della relativa {@link ImageView},
 * oltre all'eventuale {@link Shape} in caso un {@link gapp.ulg.game.util.PlayerGUI}
 * stia giocando.
 */
public class ImageViewMap {

    /**
     * Crea un oggetto {@link ImageViewMap}
     */
    public ImageViewMap (Pos first, int second, int third, ImageView fourth, ImageView fifth, Shape sixth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
        this.sixth = sixth;
    }

    /**
     * La {@link Pos} passata in input corrispondente al quadrato marrone della board
     * */
    private volatile Pos first;

    /**
     * La x dell'angolo superiore sinistro
     * */
    private volatile int second;

    /**
     * La y dell'angolo superiore sinistro
     * */
    private volatile int third;

    /**
     * La {@link ImageView} del quadrato marrone (o trasparente) presente in quella posizione
     * */
    private volatile ImageView fourth;

    /**
     * La {@link ImageView} del pezzo presente in quella posizione, altrimenti null
     * */
    private volatile ImageView fifth;

    /**
     * Nel caso di un {@link gapp.ulg.game.util.PlayerGUI}, rappresenta la Shape del quadrato verde, altrimenti null
     * */
    private volatile Shape sixth;

    /**
     * @return la {@link Pos} passata in input
     */
    Pos getFirst() {
        return first;
    }

    /**
     * @return La x dell'angolo superiore sinistro
     * */
    int getSecond() {
        return second;
    }

    /**
     * @return La y dell'angolo superiore sinistro
     * */
    int getThird() {
        return third;
    }

    /**
     * @return La {@link ImageView} del quadrato marrone (o trasparente) presente in quella posizione
     * */
    ImageView getFourth() {
        return fourth;
    }

    /**
     * @return La {@link ImageView} del pezzo presente in quella posizione, altrimenti null
     * */
    ImageView getFifth() {
        return fifth;
    }

    /**
     * Setta una nuova {@link ImageView} per il pezzo presente in quella posizione, altrimenti null
     * */
    void setFifth(ImageView fifth) {
        this.fifth = fifth;
    }

    /**
     * @return La {@link Shape} del quadrato marrone (o trasparente) presente in quella posizione
     * */
    Shape getSixth() {
        return sixth;
    }

    /**
     * Setta una nuova {@link Shape} nel caso stia giocando un {@link gapp.ulg.game.util.PlayerGUI}
     * */
    void setSixth(Shape sixth) {
        this.sixth = sixth;
    }

}
