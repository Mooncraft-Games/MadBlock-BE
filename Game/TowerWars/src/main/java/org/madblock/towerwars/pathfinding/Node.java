package org.madblock.towerwars.pathfinding;

import org.madblock.towerwars.utils.Vector2i;

public class Node {

    private final Node parentNode;
    private final Vector2i position;
    private final double distanceFromEndGoal;
    private final int stepsTaken;
    private final int spacity;  // How close you are to the wall. Higher values = farther away from a wall

    public Node(Node parentNode, Vector2i position, double distanceFromEndGoal, int stepsTaken, int spacity) {
        this.parentNode = parentNode;
        this.position = position;
        this.distanceFromEndGoal = distanceFromEndGoal;
        this.stepsTaken = stepsTaken;
        this.spacity = spacity;
    }

    public Node(Vector2i position, double distanceFromEndGoal, int stepsTaken, int spacity) {
        this.parentNode = null;
        this.position = position;
        this.distanceFromEndGoal = distanceFromEndGoal;
        this.stepsTaken = stepsTaken;
        this.spacity = spacity;
    }

    public Node getParentNode() {
        return this.parentNode;
    }

    /**
     * The score is calculated by the amount of moves it took to get to this node and the distance to the end goal from this node
     * @return a score. (lower = better)
     */
    public double getScore() {
        return this.distanceFromEndGoal + this.stepsTaken;
    }

    public double getDistanceFromEndGoal() {
        return this.distanceFromEndGoal;
    }

    public int getSpacity() {
        return this.spacity;
    }

    public int getStepsTaken() {
        return this.stepsTaken;
    }

    public Vector2i getPosition() {
        return this.position;
    }

}
