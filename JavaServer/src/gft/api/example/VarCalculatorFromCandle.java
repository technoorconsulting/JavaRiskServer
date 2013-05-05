/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gft.api.example;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import technoor.service.IRiskStatistic;
import technoor.datarequest.ProtoTradeApi.CandleProto;

/**
 *
 * @author salman
 */
public class VarCalculatorFromCandle implements IRiskStatistic {
     public class RunningAverage {

        private BigInteger myPrice1;
        private BigInteger myPrice2;
        private long count;
        private long expSize;
        private BigDecimal [] myPrice1std;
        ArrayList<SortedSet<Long>> listOfSets;
        

        
        public RunningAverage(long s) {
            myPrice1 = BigInteger.valueOf(0);
            myPrice2 = BigInteger.valueOf(0);
            myPrice1std = new BigDecimal[]{BigDecimal.valueOf(0),BigDecimal.valueOf(0)};
            expSize = s;
            listOfSets = new ArrayList<>();
            listOfSets.add(new TreeSet<Long>());
            listOfSets.add(new TreeSet<Long>());        
        }

        void addStat(long x, long y) {
            myPrice1 = myPrice1.add(BigInteger.valueOf(x));
            myPrice2 = myPrice2.add(BigInteger.valueOf(y));
            double tx = (double)myPrice1.longValue()/1000;
            double ty = (double)y/1000;        
            myPrice1std[0].add(BigDecimal.valueOf(tx*tx/expSize));
            myPrice1std[1].add(BigDecimal.valueOf(ty*ty/expSize));
            ++count;
            
            listOfSets.get(0).add(x);
            listOfSets.get(1).add(y);
        }

        public double getAverage1() {
            BigDecimal avgTime = BigDecimal.valueOf(myPrice1.longValue());
            avgTime = avgTime.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
            return avgTime.doubleValue();
        }

        public double getAverage2() {
            BigDecimal avgCol = BigDecimal.valueOf(myPrice2.longValue());
            avgCol = avgCol.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP);
            return avgCol.doubleValue();
        }
        
        public double getStd(int i){
            
            return Math.sqrt(myPrice1std[i].doubleValue());
        }
        public double getHistoricVar(int i){
            
            return Math.sqrt((double)listOfSets.get(i).last()+0.5*getStd(i));
        }
    }
     RunningAverage runAvg;
    
    
    @Override
    public CandleProto [] calculateStat(CandleProto[] p){
        runAvg = new RunningAverage(p.length);
        
        int mostRecent = p[p.length-1].getTimestamp()>p[0].getTimestamp()?p.length-1:0;
        int i = 1;
        if(mostRecent==0) i=2;
        ArrayList<CandleProto> A = new ArrayList<CandleProto>();
        CandleProto.Builder candleStat = CandleProto.newBuilder();
        for(;i<p.length;++i){            
            candleStat.setTimestamp(p[i].getTimestamp());
            candleStat.setHigh(p[i].getHigh()/p[i-1].getHigh()*p[mostRecent].getHigh());
            candleStat.setClose(p[i].getClose()/p[i-1].getClose()*p[mostRecent].getClose());
            candleStat.setLow(p[i].getLow()/p[i-1].getLow()*p[mostRecent].getLow());
            candleStat.setOpen(p[i].getOpen()/p[i-1].getOpen()*p[mostRecent].getOpen()); 
            if(i==p.length-1){
                candleStat.setExtremelow((float)runAvg.getHistoricVar(1));
                candleStat.setExtremehigh((float) runAvg.getHistoricVar(0));
            }
            
            runAvg.addStat((long)(candleStat.getHigh()*1000), (long)(candleStat.getLow()*1000)); 
            A.add(candleStat.build()); 
        }


        System.out.println("VAR high"+candleStat.getExtremehigh()+"low"+candleStat.getExtremelow());
        return A.toArray(new CandleProto[p.length-1]);
    }
}
