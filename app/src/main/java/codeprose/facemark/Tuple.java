package codeprose.facemark;

public class Tuple {
    public final float X, Y;
    public Tuple(float x, float y){
        X = x;
        Y = y;
    }

    @Override
    public String toString() {
        return String.format("(%f, %f)", X, Y);
    }
}
