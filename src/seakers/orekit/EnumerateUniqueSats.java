/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.constellations.EnumerateWalkerConstellations;
import seakers.orekit.constellations.Walker;
import seakers.orekit.constellations.WalkerParameters;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

/**
 *
 * @author Prachi
 */
public class EnumerateUniqueSats {
    public static void main(String[] args)  throws OrekitException, IOException {
    EnumerateWalkerConstellations en = new EnumerateWalkerConstellations();
        
        double[] alt = new double[5];
        double[] inc = new double[8];
        int[] sats = new int[18];
        
        for(int i_alt = 4; i_alt<=8;i_alt++) {
            alt[i_alt-4]=i_alt*100+6378000;
        }
        
        for(int i_inc = 0; i_inc<=7;i_inc++) {
            inc[i_inc]=((i_inc+3)*10);
        }
        
        for(int i_nsats = 1; i_nsats<=18;i_nsats++) {
            sats[i_nsats-1]=i_nsats;
        }
             
        ArrayList<Instrument> payload=new ArrayList<>();
        Frame inertialFrame=FramesFactory.getEME2000();
        AbsoluteDate date=new AbsoluteDate();
        ArrayList<WalkerParameters> constell = EnumerateWalkerConstellations.fullFactWalker(alt,inc,sats);
        HashMap<String,Satellite> map=new HashMap<>();
        for (WalkerParameters params:constell){
            Walker w = new Walker("", payload, params.getA(), params.getI(), 
                    params.getT(), params.getP(), params.getF(), inertialFrame, date, 0);
        for(Satellite sat:w.getSatellites()){
                map.put(String.valueOf(sat.hashCode()), sat);
            }
        }
        int uniqueSats=map.size();
        System.out.printf("%d",uniqueSats);
}
}