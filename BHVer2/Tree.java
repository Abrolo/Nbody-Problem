

import java.util.Arrays;
import java.util.concurrent.FutureTask;
class Tree {
    private int numChildren = 0;
    public Tree[] childTrees; //size 4 - ok... ok?
    private Point topLeft;
    private Point bottomRight;
    private Body _body; // This can be either a clumped pseudobody OR if we only have one body inside us, then this is that body (should work for eq. checks)

    public Tree(Point topLeft, Point bottomRight){
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public void assignChildren(Body[] children) {
        //System.out.println("assignChildren");
        numChildren = children.length;
        if (children.length > 1) {
            divide(children);
        } else if (children.length == 1) {
            _body = children[0];
        }        
    }

    public Body getBody() {
        if (_body == null) {
            double centerMassX = 0.0;
            double centerMassY = 0.0;
            double totalMass = 0.0;
            for (Tree childTree : childTrees) {
                if (childTree.numChildren == 0) continue;
                centerMassX += childTree.getBody().x*childTree.getBody().mass;
                centerMassY += childTree.getBody().y*childTree.getBody().mass;
                totalMass += childTree.getBody().mass;
            }
            centerMassX /= totalMass;
            centerMassY /= totalMass;
            _body = new Body(centerMassX, centerMassY, totalMass);
        }
        
        return _body;
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

        FutureTask<Void>[] subTasks = (FutureTask<Void>[]) new FutureTask[4];
        for (Tree childTree : childTrees) {
            var tempTask = ParallelBody.getNewTreeTask(childTree, children);
            ParallelBody.newExecutor.addTask(tempTask);
        }
        for (var subTask : subTasks) {
            while (!subTask.isDone()) {
                ParallelBody.newExecutor.assist(false);
            }
        }
        getBody(); // to calculate it
    }

    public boolean isFar(double d){
        double s;
        s = bottomRight.x - topLeft.x;
        return (s/d < ParallelBody.THETA);
    }

    public void calculateForce(Body target, Point resultingForces){
        if (numChildren == 0 || getBody() == target) return;

        double distance = getBody().computeDistance(target);
        if (isFar(distance) || numChildren == 1) {
            getBody().computeForceTo(target, resultingForces);
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

