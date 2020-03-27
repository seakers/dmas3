/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object;

import java.io.Serializable;
import java.util.Objects;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;

/**
 * The sensor or instrument to install aboard a satellite or at a ground
 * station. The sensor is defined with some field of view. There are two
 * constructors for simple conical field of view or a rectangular field of view
 *
 * @author nozomihitomi
 */
public class Instrument implements OrekitObject, Serializable {

    private static final long serialVersionUID = -3579827074220214433L;

    private final String name;
    
    private final FieldOfViewDefinition fov;
    
    private final double mass;
    
    private final double averagePower;
    
    /**
     * This constructor is for creating a custom shape field of view.
     *
     * @param name the name of the instrument
     * @param fov the field of view of the instrument
     * @param mass the mass of the instrument
     * @param averagePower the average power required by the instrument
     * 
     */
    public Instrument(String name, FieldOfViewDefinition fov, double mass, double averagePower) {
        this.name = name;
        this.fov = fov;
        this.mass = mass;
        this.averagePower = averagePower;
    }

    /**
     * Returns the field of view object
     *
     * @return
     */
    public FieldOfViewDefinition getFOV() {
        return fov;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Instrument{" + "Name=" + name + " " + fov.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.fov);
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.mass) ^ (Double.doubleToLongBits(this.mass) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.averagePower) ^ (Double.doubleToLongBits(this.averagePower) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Instrument other = (Instrument) obj;
        if (!Objects.equals(this.fov, other.fov)) {
            return false;
        }
        if (Double.doubleToLongBits(this.mass) != Double.doubleToLongBits(other.mass)) {
            return false;
        }
        if (Double.doubleToLongBits(this.averagePower) != Double.doubleToLongBits(other.averagePower)) {
            return false;
        }
        return true;
    }


    

    }
