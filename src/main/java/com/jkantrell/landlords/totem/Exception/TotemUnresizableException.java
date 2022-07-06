package com.jkantrell.landlords.totem.Exception;

import com.jkantrell.landlords.totem.Totem;
import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class TotemUnresizableException extends Exception{

    //FIELDS
    private final Totem totem_;
    private final Vector exceededNegative_;
    private final Vector exceededPositive_;
    private final LinkedList<UnresizableReason> reasons_ = new LinkedList<>();
    private boolean resized_;

    //CONSTRUCTORS
    public TotemUnresizableException(Totem totem, double exceededNegative, double exceededPositive, Axis direction, @Nullable UnresizableReason reason) {
        this.totem_ = totem;
        double[] magnitudesPositive = {0, 0, 0}, magnitudesNegative = {0, 0, 0};
        int locator = switch (direction) { case X -> 0; case Y -> 1; default -> 2; };
        magnitudesPositive[locator] = exceededPositive; magnitudesNegative[locator] = exceededNegative;
        this.exceededNegative_ = new Vector(magnitudesNegative[0], magnitudesNegative[1], magnitudesNegative[2]);
        this.exceededPositive_ = new Vector(magnitudesPositive[0], magnitudesPositive[1], magnitudesPositive[2]);
        this.addReason(reason);
    }
    public TotemUnresizableException(Totem totem, Vector exceededNegative, Vector exceededPositive, @Nullable UnresizableReason reason) {
        this.totem_ = totem;
        this.exceededNegative_ = exceededNegative;
        this.exceededPositive_ = exceededPositive;
        this.addReason(reason);
    }
    public TotemUnresizableException(Totem totem, double exceededXNeg, double exceededYNeg, double exceededZNeg, double exceededXPos, double exceededYPos, double exceededZPos, @Nullable UnresizableReason reason) {
        this(
                totem,
                new Vector(exceededXNeg, exceededYNeg, exceededZNeg),
                new Vector(exceededXPos, exceededYPos, exceededZPos),
                reason
        );
    }
    public TotemUnresizableException(Totem totem, double exceededMagnitude, BlockFace direction, @Nullable UnresizableReason reason){
        if (!direction.isCartesian()) {
            throw new IllegalArgumentException("The provided BlockFace must be cartesian (NORTH, SOUTH, EAST, WEST, UP, DOWN).");
        }

        Vector magVector = direction.getDirection().multiply(exceededMagnitude);
        Vector zeroVector = new Vector(0,0,0);

        this.totem_ = totem;
        switch (direction) {
            case WEST, NORTH, DOWN ->
                    { this.exceededNegative_ = magVector; this.exceededPositive_ = zeroVector; }
            default ->
                    { this.exceededNegative_ = zeroVector; this.exceededPositive_ = magVector; }
        }
        this.addReason(reason);
    }
    public TotemUnresizableException(Totem totem, double exceededMagnitude, @Nonnull OneDirectionalUnresizableReason reason) {
        this(totem, exceededMagnitude, reason.getDirection(), reason);
    }

    //GETTERS
    public Vector getExceededNegative() {
        return exceededNegative_;
    }
    public Vector getExceededPositive() {
        return exceededPositive_;
    }
    public double getTotalExceeded() {
        Vector[] vectors = {this.exceededNegative_, this.exceededPositive_};
        double r = 0;
        for (int i = 0; i < 2; i++) {
            r += vectors[i].getX() + vectors[i].getY() + vectors[i].getZ();
        }
        return r;
    }
    public List<UnresizableReason> getReasons() {
        return new ArrayList<>(this.reasons_);
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
    public Totem getTotem() {
        return this.totem_;
    }
    public boolean wasResized() {
        return this.resized_;
    }

    //SETTERS
    public void announceResizing() {
        this.resized_ = true;
    }

    //METHODS
    public void addReason(UnresizableReason reason) {
        if (reason == null) { return; }
        reason.setTotem(this.getTotem());
        this.reasons_.add(reason);
    }
    public void addReasons(Collection<UnresizableReason> reasons) {
        for (UnresizableReason reason : reasons) { this.addReason(reason); }
    }
    public void addReasons(UnresizableReason... reasons) {
        this.addReasons(List.of(reasons));
    }
    public void addExceeded(double exceededNegative, double exceededPositive, Axis direction) {
        double[] magnitudesPositive = {0, 0, 0}, magnitudesNegative = {0, 0, 0};
        int locator = switch (direction) { case X -> 0; case Y -> 1; default -> 2; };
        magnitudesPositive[locator] = exceededPositive; magnitudesNegative[locator] = exceededNegative;
        this.exceededNegative_.add(new Vector(magnitudesNegative[0], magnitudesNegative[1], magnitudesNegative[2]));
        this.exceededPositive_.add(new Vector(magnitudesPositive[0], magnitudesPositive[1], magnitudesPositive[2]));
    }
    public void addExceeded(Vector exceededNegative, Vector exceededPositive) {
        this.exceededNegative_.add(exceededNegative);
        this.exceededPositive_.add(exceededPositive);
    }
    public void addExceeded(double exceededXNeg, double exceededYNeg, double exceededZNeg, double exceededXPos, double exceededYPos, double exceededZPos, @Nullable UnresizableReason reason) {
        this.addExceeded(
                new Vector(exceededXNeg, exceededYNeg, exceededZNeg),
                new Vector(exceededXPos, exceededYPos, exceededZPos)
        );
    }
    public void addExceeded(double exceededMagnitude, BlockFace direction){
        if (!direction.isCartesian()) {
            throw new IllegalArgumentException("The provided BlockFace must be cartesian (NORTH, SOUTH, EAST, WEST, UP, DOWN).");
        }
        Vector magVector = direction.getDirection().multiply(exceededMagnitude);
        Vector zeroVector = new Vector(0,0,0);

        switch (direction) {
            case WEST, NORTH, DOWN ->
                    { this.exceededNegative_.add(magVector); this.exceededPositive_.add(zeroVector); }
            default ->
                    { this.exceededNegative_.add(zeroVector); this.exceededPositive_.add(magVector); }
        }
    }
}
