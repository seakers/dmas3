/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * A collection of satellites
 * @author nozomihitomi
 */
public class Constellation implements OrekitObject, Serializable{
    private static final long serialVersionUID = -9173393663699339529L;
    
    private final HashSet<Satellite> satellites;
    private final String name;

    public Constellation(String name, Collection<Satellite> satellites) {
        this.satellites = new HashSet<>(satellites);
        this.name = name;
    }
    
    /**
     * Method to check if a given satellite is part of the constellation
     * @param satellite
     * @return 
     */
    public boolean hasSatellite(Satellite satellite){
        return satellites.contains(satellite);
    }

    public Collection<Satellite> getSatellites() {
        return satellites;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Constellation{" + "name=" + name + ", number of satellites=" + satellites.size() + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.satellites);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Constellation)) {
            return false;
        }
        final Constellation other = (Constellation) obj;
        if (!Objects.equals(this.satellites, other.satellites)) {
            return false;
        }
        return true;
    }
    
    
    
}
