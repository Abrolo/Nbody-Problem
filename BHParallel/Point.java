

public class Point{
    public double x, y;
    public int id;
    public Point(double x, double y){
        this.x = x;
        this.y = y;

    }
    
    public Point(double x, double y, int id){
        this.x = x;
        this.y = y;
        this.id = id;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public void add(double deltaX, double deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }
}