package codeprose.facemark;

public class Tuple {
    private float X, Y;
    public Tuple(float x, float y){
        X = x;
        Y = y;
    }

    public float getX() {
        return X;
    }

    public float getY() {
        return Y;
    }

    @Override
    public String toString() {
        return String.format("(%f, %f)", X, Y);
    }
}
