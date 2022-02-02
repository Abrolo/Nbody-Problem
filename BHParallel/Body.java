

public class Body extends Point {
    public double mass;
    final double G = 6.67e-11;
    public Body(double x, double y, double mass){
        super(x, y);
        this.mass = mass;
    }
    
    public void computeForceTo(Body other, Point result) {
        Point direction = new Point(0,0);
        double magnitude;
        double distance = computeDistance(other);
        distance = Math.max(distance, 0.1);
        magnitude = (G*mass*other.mass) / Math.pow(distance,2);

        direction.x = other.x-x;
        direction.y = other.y-y;
        
        result.x += magnitude*direction.x/distance;
        result.y += magnitude*direction.y/distance;

    }

    public double computeDistance(Body other){
        double distance =Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
        //System.out.println(String.format("Distance between (%f,%f) and (%f,%f): %f", x, y, other.x, other.y, distance));
        return distance;
    }
}
