
import java.util.Arrays;

import java.util.Random;


public class barnesSeq {
    Point[] v;
    Point[] f;
    double[] m;
    final int DT = 1;
    int time = 0;
    int finishTime;
    Body[] bodies;
    int n;
    Tree root;
    static double THETA;

    final int X_GRID = 10;
    final int Y_GRID = 10;
    double XMIN = Integer.MAX_VALUE;
    double XMAX = Integer.MIN_VALUE;
    double YMIN = Integer.MAX_VALUE;
    double YMAX = Integer.MIN_VALUE;
    // final AtomicInteger XMIN = new AtomicInteger(Integer.MAX_VALUE);
    // final AtomicInteger XMAX = new AtomicInteger(Integer.MIN_VALUE);
    // final AtomicInteger YMIN = new AtomicInteger(Integer.MAX_VALUE);
    // final AtomicInteger YMAX = new AtomicInteger(Integer.MIN_VALUE);

    public barnesSeq(int n, int steps, int th){
        this.n = n;
        THETA = ((double)th)/10;
        v = new Point[n];
        //f = new Point[numWorkers][n];
        f = new Point[n];
        m = new double[n];
        finishTime = steps*DT;
        bodies = new Body[n];




        // Do these two in each step:

        init(n, 0);
        runSims5();
    }

    public void runSims5(){
        long t1 = System.nanoTime();
        for (time = 0; time < finishTime; time += DT) {
            root = new Tree(new Point(XMIN, YMAX), new Point(XMAX, YMIN)); // WHen updating bodies position, keep track of max globally (global atomic something)
            root.assignChildren(bodies);
            calculateForces();
            moveBodies();
        }
        long t2 = System.nanoTime();
        System.out.println("Completed in " + (t2 - t1) / 1E6 + "ms");
    }

    public void calculateForces(){
        for (int bodyID = 0; bodyID < n; bodyID++) {
            root.calculateForce(bodies[bodyID], f[bodyID]);
        }
    }
    
    public void moveBodies() {
        Point deltav, deltap;
        for (int i = 0; i < n; i++) {
            deltav = new Point(f[i].x / m[i] * DT, f[i].y / m[i] * DT);
            deltap = new Point((v[i].x + deltav.x/2) * DT, 
                                    (v[i].y + deltav.y/2) * DT);
            v[i].x = v[i].x + deltav.x;
            v[i].y = v[i].y + deltav.y;
            bodies[i].x = bodies[i].x + deltap.x;
            bodies[i].y = bodies[i].y + deltap.y;
            //System.out.println("Body " + i + " moved (" + deltap.x + "," + deltap.y + ")");
            f[i].x = 0;
            f[i].y = 0;
        }
    }


    public static void main(String[] args) throws InterruptedException{
        int n = Integer.parseInt(args[0]);
        int steps = Integer.parseInt(args[1]);
        int th = Integer.parseInt(args[2]);
        new barnesSeq(n, steps, th);
        
    } 

    public void setMinMax(double x, double y) {
        XMIN = Math.min(XMIN, x);
        XMAX = Math.max(XMAX, x);
        YMIN = Math.min(YMIN, y);
        YMAX = Math.max(YMAX, y);
        /*
        int xmin = (int) Math.floor(x);
        int xmax = (int) Math.ceil(x);
        int ymin = (int) Math.floor(y);
        int ymax = (int) Math.ceil(y);
        //if (xmin < XMIN.get())
            XMIN.accumulateAndGet(xmin, Math::min);
        //if (xmax > XMAX.get())
            XMAX.accumulateAndGet(xmax, Math::max);
        //if (ymin < YMIN.get())
            YMIN.accumulateAndGet(ymin, Math::min);
        //if (ymax > YMAX.get())
            YMAX.accumulateAndGet(ymax, Math::max);
        */
    }
    
    public void init(int n, int numWorkers){
        Random rand = new Random();
        double x, y;
        for(int i = 0; i<n; i++){
            //randomly spaces out bodies in a grid
            x = rand.nextDouble() * X_GRID;
            y = rand.nextDouble() * Y_GRID;
            setMinMax(x, y);
            bodies[i] = new Body(x, y, i);

            v[i] = new Point(0, 0);

            f[i] = new Point(0, 0);


            m[i] = rand.nextDouble()*10e+6;

        }
    }   
}


class Tree{
    private int numChildren = 0;
    private Tree[] childTrees; //size 4 - ok... ok?
    private Point topLeft;
    private Point bottomRight;
    private Body body; // This can be either a clumped pseudobody OR if we only have one body inside us, then this is that body (should work for eq. checks)


    public Tree(Point topLeft, Point bottomRight){
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public void assignChildren(Body[] children) {
        numChildren = children.length;
        if (children.length > 1) {
            divide(children);
            double centerMassX = 0.0;
            double centerMassY = 0.0;
            double totalMass = 0.0;
            for (Tree childTree : childTrees) {
                if (childTree.numChildren == 0) continue;
                centerMassX += childTree.body.x*childTree.body.mass;
                centerMassY += childTree.body.y*childTree.body.mass;
                totalMass += childTree.body.mass;
            }
            centerMassX /= totalMass;
            centerMassY /= totalMass;
            body = new Body(centerMassX, centerMassY, totalMass);            
        } else if (children.length == 1) {
            body = children[0];
        }
        
                
    }

    private void divide(Body[] children) {
        var deltaX = (bottomRight.x - topLeft.x);
        var deltaY = (bottomRight.y - topLeft.y);
        var midX = topLeft.x + deltaX / 2;
        var midY = topLeft.y + deltaY / 2;
        Point midPoint = new Point(midX, midY);

        childTrees = new Tree[]{
            new Tree(topLeft, midPoint), // Top left
            new Tree(new Point(midX, topLeft.y), new Point(bottomRight.x, midY)), // Top right
            new Tree(new Point(topLeft.x, midY), new Point(midX, bottomRight.y)), // Bottom left
            new Tree(midPoint, bottomRight) // Bottom right
        };
        
        for (Tree childTree : childTrees) {
            Body[] temp = Arrays.stream(children).filter(child -> childTree.isInBox(child)).toArray(Body[]::new);
            childTree.assignChildren(temp);
        }
    }

    public boolean isFar(double d){
        double s;
        s = bottomRight.x - topLeft.x;
        return (s/d < barnesSeq.THETA);
    }

    public void calculateForce(Body target, Point resultingForces){
        if (numChildren == 0 || body == target) return;

        double distance = body.computeDistance(target);
        if (isFar(distance) || numChildren == 1) {
            body.computeForceTo(target, resultingForces);
        } else { // We are close and we have more than one child
            for (Tree subTree : childTrees) {
                subTree.calculateForce(target, resultingForces);
            }
        }
    }

    public boolean isInBox(Body p) {
        return p.x >= topLeft.x && p.x < bottomRight.x && p.y <= topLeft.y && p.y > bottomRight.y;
    }
}

class Body extends Point {
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