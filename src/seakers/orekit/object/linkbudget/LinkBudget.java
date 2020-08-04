/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.linkbudget;

import java.io.Serializable;

/**
 *
 * @author paugarciabuzzi
 */
public class LinkBudget implements Serializable{

    private static final long serialVersionUID = 2405364072901796995L;
    
    private final double txPower;
    
    private final double txGain;
    
    private final double rxGain;
    
    private final double lambda;
    
    private final double boltzmannConstant = 1.38064852e-23;
    
    private final double noiseTemperature;
    
    private final double dataRate;
    
    private double distance;
    
    private final double minEbN0_dB;

    public LinkBudget(double txPower, double txGain, double rxGain, double lambda, double noiseTemperature, double dataRate, double distance) {
        this.txPower = txPower;
        this.txGain = txGain;
        this.rxGain = rxGain;
        this.lambda = lambda;
        this.noiseTemperature = noiseTemperature;
        this.dataRate = dataRate;
        this.distance=distance;
        this.minEbN0_dB=1.8; //assuming a QPSK scheme wit BER=10-5
    }
    
    public LinkBudget(double txPower, double txGain, double rxGain, double lambda, double noiseTemperature, double dataRate){
        this(txPower,txGain, rxGain, lambda, noiseTemperature, dataRate, 0);
    }
    
    
    public double Margin(){
        double EbN0=(this.txPower*this.txGain*this.rxGain*java.lang.Math.pow((this.lambda),2))/(java.lang.Math.pow(4*java.lang.Math.PI*this.distance,2)*this.boltzmannConstant*this.noiseTemperature*this.dataRate);
        double EbN0_dB=java.lang.Math.log10(EbN0);
        return EbN0_dB-this.minEbN0_dB;
    }
    
    public double MaxDistance(){
        double minEbN0=java.lang.Math.pow(10, this.minEbN0_dB/10);
        return java.lang.Math.sqrt((this.txPower*this.txGain*this.rxGain*java.lang.Math.pow((this.lambda),2))/(java.lang.Math.pow(4*java.lang.Math.PI,2)*this.boltzmannConstant*this.noiseTemperature*this.dataRate*minEbN0));
    }
   
    public double MaxDateRate(){
        double minEbN0=java.lang.Math.pow(10, this.minEbN0_dB/10);
        return (this.txPower*this.txGain*this.rxGain*java.lang.Math.pow((this.lambda),2))/(java.lang.Math.pow(4*java.lang.Math.PI*this.distance,2)*this.boltzmannConstant*this.noiseTemperature*minEbN0);
    }
    
    public void setDistance(double distance){
        this.distance=distance;
    }
    

    
    
    
}
