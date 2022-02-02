

import java.util.Arrays;

public class Tree{
    private int numChildren = 0;
    private Tree[] childTrees; //size 4 - ok... ok?
    private Point topLeft;
    private Point bottomRight;
    private Body body; // This can be either a clumped pseudobody OR if we only have one body inside us, then this is that body (should work for eq. checks)

    final double THETA = 0.5;


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
        s =bottomRight.x-topLeft.x;
        //System.out.println("s = " + s + " d=" + d + " THETA=" + ParallelBody.THETA);
        //System.out.println(s/d < ParallelBody.THETA);
        return (s/d < ParallelBody.THETA);
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
