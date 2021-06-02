package org.madblock.towerwars.pathfinding;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.*;
import java.util.function.Supplier;

public class PathfinderAsyncConsumer implements Supplier<List<Vector2>> {

    private static final Vector2[] POSSIBLE_MOVE_ADJUSTMENTS = new Vector2[]{
        new Vector2(0, -1), // Up
        new Vector2(0, 1),  // Down
        new Vector2(1, 0),  // Right
        new Vector2(-1, 0)  // Left
    };

    private final Settings settings;

    private final Set<Node> activeNodes = new HashSet<>();  // Nodes to investigate
    private final Map<Vector2, Node> smallestScores = new HashMap<>();      // Record of smallest scores for each vector
                                                                            // to ensure that we don't just end up in a loop

    public PathfinderAsyncConsumer(Settings settings) {
        this.settings = settings;
    }

    @Override
    public List<Vector2> get() {
        Vector2 initialPosition = new Vector2((int)this.settings.getInitialPosition().getX(), (int)this.settings.getInitialPosition().getZ());
        int targetSpacity = this.getSpacity(initialPosition);
        Node currentNode = new Node(initialPosition, this.calculateDistanceToEndGoal(initialPosition), 0, targetSpacity);
        this.activeNodes.add(currentNode);
        this.smallestScores.put(currentNode.getPosition(), currentNode);

        while (this.activeNodes.size() > 0) {
            // Investigate the smallest node
            currentNode = this.activeNodes
                    .stream()
                    .sorted((nodeA, nodeB) -> (int)((nodeA.getScore() + Math.abs(this.getSpacity(nodeA.getPosition()) - targetSpacity)) - (nodeB.getScore() + Math.abs(this.getSpacity(nodeB.getPosition()) - targetSpacity))))
                    .findAny()
                    .get();
            this.activeNodes.remove(currentNode);
            this.activeNodes.addAll(this.getNodes(currentNode));
            if (currentNode.getDistanceFromEndGoal() == 0) {
                break;
            }
        }

        // Return orders

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
     * @param parentNode
     * @return
     */
    private Set<Node> getNodes(Node parentNode) {
        Vector2 position = parentNode.getPosition();
        Set<Node> nodes = new HashSet<>();

        for (Vector2 adjustment : POSSIBLE_MOVE_ADJUSTMENTS) {
            Vector2 nodePosition = position.add(adjustment);

            // They cannot go back to their previous position
            if (parentNode.getParentNode() != null && parentNode.getParentNode().getPosition().equals(nodePosition)) {
                continue;
            }

            // Is it a valid move?
            if (this.settings.getBoundaries().isWithinThisRegion(new Vector3(nodePosition.getX(), this.settings.getInitialPosition().getY(), nodePosition.getZ())) && this.isBlockAir(nodePosition)) {
                Node newNode = new Node(parentNode, nodePosition, this.calculateDistanceToEndGoal(nodePosition), parentNode.getStepsTaken() + 1, this.getSpacity(nodePosition));

                // If another route found a more efficient way to get here, do not continue with our current route.
                if (this.smallestScores.containsKey(nodePosition)) {
                    if (this.smallestScores.get(nodePosition).getScore() > newNode.getScore()) {
                        // our current route is better. Get rid of the old route and replace it with our route.
                        this.activeNodes.remove(this.smallestScores.get(nodePosition));
                        this.smallestScores.put(nodePosition, newNode);
                        nodes.add(newNode);
                    }
                } else {
                    nodes.add(newNode);
                    this.smallestScores.put(nodePosition, newNode);
                }
            }
        }

        return nodes;
    }

    private int getSpacity(Vector2 position) {
        return this.settings.getSpacityMap()[Math.abs(this.settings.getBoundaries().getPosLesser().getZ() - position.getZ())][Math.abs(this.settings.getBoundaries().getPosLesser().getX() - position.getX())];
    }

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
        return this.settings.getLevel().getBlockIdAt(position.getX(), (int)this.settings.getInitialPosition().getY(), position.getZ()) == Block.AIR;
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
