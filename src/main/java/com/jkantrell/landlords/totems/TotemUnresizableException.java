package com.jkantrell.landlords.totems;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class TotemUnresizableException extends Exception{

    //ASSETS
    public enum Reason { FACE_COLLISION, SIZE_MAXED_OUT, SIZE_MAXED_IN, VOLUME_MAXED_OUT }

    //FIELDS
    private final Vector exceededNegative_;
    private final Vector exceededPositive_;
    private final Reason reason_;

    //CONSTRUCTORS
    TotemUnresizableException(double exceededNegative, double exceededPositive, Axis direction, Reason reason) {
        double[] magnitudesPositive = {0, 0, 0}, magnitudesNegative = {0, 0, 0};
        int locator = switch (direction) { case X -> 0; case Y -> 1; default -> 2; };
        magnitudesPositive[locator] = exceededPositive; magnitudesNegative[locator] = exceededNegative;
        this.exceededNegative_ = new Vector(magnitudesNegative[0], magnitudesNegative[1], magnitudesNegative[2]);
        this.exceededPositive_ = new Vector(magnitudesPositive[0], magnitudesPositive[1], magnitudesPositive[2]);
        this.reason_ = reason;
    }
    TotemUnresizableException(Vector exceededNegative, Vector exceededPositive, Reason reason) {
        this.exceededNegative_ = exceededNegative;
        this.exceededPositive_ = exceededPositive;
        this.reason_ = reason;
    }
    TotemUnresizableException(double exceededXNeg, double exceededYNeg, double exceededZNeg, double exceededXPos, double exceededYPos, double exceededZPos, Reason reason) {
        this(
                new Vector(exceededXNeg, exceededYNeg, exceededZNeg),
                new Vector(exceededXPos, exceededYPos, exceededZPos),
                reason
        );
    }
    TotemUnresizableException(double exceededMagnitude, BlockFace direction, Reason reason){
        if (!direction.isCartesian()) {
            throw new IllegalArgumentException("The provided BlockFace must be cartesian (NORTH, SOUTH, EAST, WEST, UP, DOWN).");
        }

        Vector magVector = direction.getDirection().multiply(exceededMagnitude);
        Vector zeroVector = new Vector(0,0,0);

        switch (direction) {
            case WEST, NORTH, DOWN ->
                    { this.exceededNegative_ = magVector; this.exceededPositive_ = zeroVector; }
            default ->
                    { this.exceededNegative_ = zeroVector; this.exceededPositive_ = magVector; }
        }
        this.reason_ = reason;
    }

    //GETTERS

    public Vector getExceededNegative() {
        return exceededNegative_;
    }
    public Vector getExceededPositive() {
        return exceededPositive_;
    }
    public Reason getReason() {
        return reason_;
    }
    public double getExceeded(BlockFace direction) {
        return switch (direction) {
            case WEST -> this.exceededNegative_.getBlockX();
            case NORTH -> this.exceededNegative_.getBlockZ();
            case DOWN -> this.exceededNegative_.getBlockY();
            case EAST -> this.exceededPositive_.getBlockX();
            case SOUTH -> this.exceededPositive_.getBlockZ();
            case UP -> this.exceededPositive_.getBlockY();
            default -> throw new IllegalArgumentException("The provided BlockFace must be cartesian (NORTH, SOUTH, EAST, WEST, UP, DOWN).");
        };
    }
    public double getExceeded(Axis direction) {
        Function<Vector, Double> extractor = switch (direction) {
            case X -> Vector::getX;
            case Y -> Vector::getY;
            case Z -> Vector::getZ;
        };

        return extractor.apply(this.exceededNegative_) + extractor.apply(this.exceededPositive_);
    }
}
