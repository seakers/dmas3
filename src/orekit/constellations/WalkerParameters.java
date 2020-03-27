/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.constellations;

/**
 *
 * @author paugarciabuzzi
 */
public class WalkerParameters {
    
    double a;
    double i;
    int t;
    int p;
    int f;

    public WalkerParameters(double a, double i, int t, int p, int f) {
        this.a = a;
        this.i = i;
        this.t = t;
        this.p = p;
        this.f = f;
    }

    public double getA() {
        return a;
    }

    public double getI() {
        return i;
    }

    public int getT() {
        return t;
    }

    public int getP() {
        return p;
    }

    public int getF() {
        return f;
    }
    
    
    
}
