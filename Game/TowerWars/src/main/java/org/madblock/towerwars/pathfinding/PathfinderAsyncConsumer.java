package org.madblock.towerwars.pathfinding;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.*;
import java.util.function.Supplier;

/**
 * Modified version of A*
 * The goal of this pathfinding algorithm is to stay in our lane while also finding the best possible route to the end goal region.
 * It is not to get the most efficient path to the end goal region, otherwise all paths would collide onto one lane in the end goal region.
 *
 * Behaviour:
 * Each node is assigned a score and a spacity value.
 *  Score is composed of the distance to the end goal and the amount of steps it took to get to this node
 *  Spacity is raised the higher a node is from a wall
 * It will investigate all of the adjacent nodes around itself and choose the node that has the same/higher spacity and has the lowest value.
 *  If there is a node that meets the spacity requirement, it will ignore any adjacent nodes that do not meet this requirement as to ensure we stay in our spacity lane.
 * If there is no node that meets the spacity requirements, it will choose the node with the lowest score.
 */
public class PathfinderAsyncConsumer implements Supplier<List<Vector2>> {

    private static final Vector2[] POSSIBLE_MOVE_ADJUSTMENTS = new Vector2[]{
            new Vector2(0, -1), // Up
            new Vector2(0, 1),  // Down
            new Vector2(1, 0),  // Right
            new Vector2(-1, 0)  // Left
    };

    private final Settings settings;

    private final Set<Node> activeNodes = new HashSet<>();  // Nodes we have seen but have yet to explore.
    private final Map<Vector2, Node> smallestScores = new HashMap<>();      // Record of smallest scores (most efficient) for each vector
    // to ensure that we don't just end up in a loop

    private int targetSpacity;

    public PathfinderAsyncConsumer(Settings settings) {
        this.settings = settings;
    }

    @Override
    public List<Vector2> get() {
        Vector2 initialPosition = new Vector2((int)this.settings.getInitialPosition().getX(), (int)this.settings.getInitialPosition().getZ());
        this.targetSpacity = this.getSpacity(initialPosition);

        Node currentNode = new Node(initialPosition, this.calculateDistanceToEndGoal(initialPosition), 0, this.targetSpacity);
        this.activeNodes.add(currentNode);
        this.smallestScores.put(currentNode.getPosition(), currentNode);

        while (this.activeNodes.size() > 0) {
            // Investigate the best node we have collected so far

            currentNode = this.activeNodes
                    .stream()
                    .sorted((nodeA, nodeB) -> (int)(nodeA.getScore() - nodeB.getScore()))
                    .findAny()
                    .get();
            if (currentNode.getDistanceFromEndGoal() == 0) {
                break;  // We have a path
            }

            this.activeNodes.remove(currentNode);
            this.activeNodes.addAll(this.getNodes(currentNode));
        }

        // Return path
        if (this.isVectorWithinEndGoal(currentNode.getPosition())) {
            LinkedList<Vector2> orders = new LinkedList<>();
            while (currentNode.getParentNode() != null) {
                orders.addFirst(currentNode.getPosition());
                currentNode = currentNode.getParentNode();

            }
            return orders;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get the possible nodes around a node.
     * If a chosen node has already been chosen before and can be reached quicker, it is removed or replaced.
     * If a node is found with the idealSpacity value, then all other nodes are disregarded and only nodes that meet that spacity are returned.
     * This is used so that enemies stay in their lane rather than hugging the walls
     * @param parentNode
     * @return
     */
    private Set<Node> getNodes(Node parentNode) {
        Vector2 position = parentNode.getPosition();
        Set<Node> nodes = new HashSet<>();
        Set<Node> matchingSpacityNodes = new HashSet<>();

        for (Vector2 adjustment : POSSIBLE_MOVE_ADJUSTMENTS) {
            Vector2 nodePosition = position.add(adjustment);

            // They cannot go back to their previous position
            if (parentNode.getParentNode() != null && parentNode.getParentNode().getPosition().equals(nodePosition)) {
                continue;
            }

            // Is it a valid move?
            if (this.settings.getBoundaries().isWithinThisRegion(new Vector3(nodePosition.getX(), this.settings.getBoundaries().getPosLesser().getY(), nodePosition.getZ())) && this.isBlockAir(nodePosition)) {
                Node newNode = new Node(parentNode, nodePosition, this.calculateDistanceToEndGoal(nodePosition), parentNode.getStepsTaken() + 1, this.getSpacity(nodePosition));

                // If another route found a more efficient way to get here, do not continue with our current route.
                if (this.smallestScores.containsKey(nodePosition)) {
                    if (this.smallestScores.get(nodePosition).getScore() > newNode.getScore()) {
                        // our current route is better. Get rid of the old route and replace it with our route.
                        this.activeNodes.remove(this.smallestScores.get(nodePosition));
                        this.smallestScores.put(nodePosition, newNode);
                        nodes.add(newNode);

                        // Prioritize nodes that have a higher spacity or our target spacity in order to stay on course and not enter other lanes
                        if (newNode.getSpacity() >= parentNode.getSpacity() || newNode.getSpacity() == this.targetSpacity) {
                            matchingSpacityNodes.add(newNode);
                        }
                    }

                } else {
                    this.smallestScores.put(nodePosition, newNode);
                    nodes.add(newNode);

                    // Prioritize nodes that have a higher spacity or our target spacity in order to stay on course and not enter other lanes
                    if (newNode.getSpacity() >= parentNode.getSpacity() || newNode.getSpacity() == this.targetSpacity) {
                        matchingSpacityNodes.add(newNode);
                    }
                }
            }
        }

        if (matchingSpacityNodes.size() > 0) {
            return matchingSpacityNodes;
        } else {
            return nodes;
        }
    }

    private int getSpacity(Vector2 position) {
        return this.settings.getSpacityMap()[(int)Math.abs(this.settings.getBoundaries().getPosLesser().getZ() - position.getZ())][(int)Math.abs(this.settings.getBoundaries().getPosLesser().getX() - position.getX())];
    }

    /**
     * Distance to nearest end point in end goal region given position.
     * @param position our current position
     * @return
     */
    private double calculateDistanceToEndGoal(Vector2 position) {
        double lowestDistance = Double.MAX_VALUE;
        for (int x = this.settings.getEndGoalRegion().getPosLesser().getX(); x <= this.settings.getEndGoalRegion().getPosGreater().getX(); x++) {
            for (int z = this.settings.getEndGoalRegion().getPosLesser().getZ(); z <= this.settings.getEndGoalRegion().getPosGreater().getZ(); z++) {

                double distance = Math.abs(x - position.getX()) + Math.abs(z - position.getZ());
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                }
            }
        }
        return lowestDistance;
    }

    private boolean isBlockAir(Vector2 position) {
        return this.settings.getLevel().getBlockIdAt((int)position.getX(), this.settings.getBoundaries().getPosLesser().getY(), (int)position.getZ()) == Block.AIR;
    }

    private boolean isVectorWithinEndGoal(Vector2 position) {
        return this.settings.getEndGoalRegion().isWithinThisRegion(new Vector3(position.getX(), this.settings.getEndGoalRegion().getPosLesser().getY(), position.getZ()));
    }


    public static class Settings {

        private final ChunkManager level;
        private final Vector3 initialPosition;
        private final MapRegion boundaries;
        private final MapRegion endGoalRegion;
        private final int[][] spacityMap;

        private Settings(
                ChunkManager level,
                Vector3 initialPosition,
                MapRegion boundaries,
                MapRegion endGoalRegion,
                int[][] spacityMap
        ) {
            this.level = level;
            this.initialPosition = initialPosition;
            this.boundaries = boundaries;
            this.endGoalRegion = endGoalRegion;
            this.spacityMap = spacityMap;
        }

        public ChunkManager getLevel() {
            return this.level;
        }

        public Vector3 getInitialPosition() {
            return this.initialPosition;
        }

        public MapRegion getBoundaries() {
            return this.boundaries;
        }

        public MapRegion getEndGoalRegion() {
            return this.endGoalRegion;
        }

        public int[][] getSpacityMap() {
            return this.spacityMap;
        }



        public static class Builder {

            private ChunkManager level;
            private Vector3 initialPosition;
            private MapRegion boundaries;
            private MapRegion endGoalRegion;
            private int[][] spacityMap;

            public Builder setLevel(ChunkManager level) {
                this.level = level;
                return this;
            }

            public Builder setInitialPosition(Vector3 initialPosition) {
                this.initialPosition = initialPosition;
                return this;
            }

            public Builder setBoundaries(MapRegion boundaries) {
                this.boundaries = boundaries;
                return this;
            }

            public Builder setEndGoalRegion(MapRegion endGoalRegion) {
                this.endGoalRegion = endGoalRegion;
                return this;
            }

            public Builder setSpacityMap(int[][] spacityMap) {
                this.spacityMap = spacityMap;
                return this;
            }

            public Settings build() {
                return new Settings(this.level, this.initialPosition, this.boundaries, this.endGoalRegion, this.spacityMap);
            }

        }

    }

}
