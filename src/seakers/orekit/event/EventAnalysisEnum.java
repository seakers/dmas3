/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

/**
 * The enums for the event analysis types
 * @author nhitomi
 */
public enum EventAnalysisEnum {
    
    /**
     * Analysis that detects when targets of interest are in the field of view of a satellite
     */
    FOV,
    /**
     * Analysis that detects when targets experience a specified sun angle 
     */
    GND_BODY_ANGLE,
    
    /**
     * Analysis of ground station accesses
     */
    ACCESS
}
