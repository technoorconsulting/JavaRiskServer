/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gft.api.example;

import gft.api.Candle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import technoor.datarequest.ProtoTradeApi;
import technoor.datarequest.ProtoTradeApi.CandleProto;
import technoor.datarequest.ProtoTradeApi.Response;

/**
 *
 * @author salman
 */
public class ServiceUpdate implements technoor.service.IServiceUpdater {
    private BufferedWriter bufferedWriter;
    private ServerSocket sSocket;
    StringBuilder str;
    Socket s; // =ss.accept();
    private int instrumentId;
    private int numOfDays;
    private int timePeriod;

    //	@ getter
    @Override
    public int getInstrumentId() {
        return instrumentId;
    }

    /**
     *
     * @return
     */
    @Override
    public int getNumOfDays() {
        return 5;
    }

    private void log(String s) {
        System.out.println(s);
    }

    public ServiceUpdate() throws IOException {
        sSocket = new ServerSocket(8801);
    }

    /*
     * waits for a socket connection and returns the number of points requested.
     */
    @Override
    public int waitForConnection() throws IOException {
        s = sSocket.accept();
        System.out.println("Client Accepted2");
        int numOfPeriods = -1;
        String read = "";
        try {
            ProtoTradeApi.RequestUpdate ru = ProtoTradeApi.RequestUpdate.parseDelimitedFrom(s.getInputStream());
            log("ID got" + ru.getId());
            instrumentId = ru.getId();
            log("numOfPeriods" + ru.getNumOfPeriods());
            numOfPeriods = ru.getNumOfPeriods();
            setNumOfDays(ru.getNumOfDays());
            setTimePeriod(ru.getTimeMin());
        } catch (Exception e) {
            // for backward compatibility try to read the stream.
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                read = br.readLine();
                numOfPeriods = Integer.parseInt(read);
                log("Number read" + read);
                read = br.readLine();
                log("Number read" + read);
                instrumentId = Integer.parseInt(read);
            } catch (NumberFormatException ne) {
                System.err.println("Exception:" + read);
                instrumentId = 3;
                ne.printStackTrace();
            }
        }
        //System.out.println(br.readLine());
        return numOfPeriods;
    }

    public Response createResponse(Candle[] a) {
        Response response = Response.getDefaultInstance();
        Response.Builder responseBuilder;
        responseBuilder = Response.newBuilder();
        CandleProto candle = CandleProto.getDefaultInstance();
        CandleProto.Builder candleBuilder = candle.newBuilderForType();
        int i;
        for (i = 0; i < a.length; i++) {
            // assume it is unix timestamp so converting to excel readable number in terms of days
            // adding the uni timestart of Jan 1 1970
            candleBuilder.setTimestamp(a[i].getStartTimestamp());
            candleBuilder.setHigh(a[i].getHigh());
            candleBuilder.setLow(a[i].getLow());
            candleBuilder.setOpen(a[i].getOpen());
            candleBuilder.setClose(a[i].getClose());
            responseBuilder.addDataCandle(candleBuilder.build());
        }
        return responseBuilder.build();
    }

    @Override
    public void writeArrayToOutput(String title, Candle[] a, Candle[] ref, String fname) {
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(fname));
            str = new StringBuilder();
            log("writing to file");
            int i = 0;
            for (int j = 0; j < ref.length; j++) {
                log("" + ref[j] + ",");
                for (; i < a.length; ++i) {
                    if (a[i].getStartTimestamp() == ref[j].getStartTimestamp()) {
                        i = j;
                        str.append("").append(a[i].getStartTimestamp()).append(",").append(a[i].getHigh()).append(",").append(a[i].getLow()).append(",").append(a[i].getOpen()).append(",").append(a[i].getClose()).append(",").append(ref[j].getHigh()).append(",").append(ref[j].getLow()).append("\r\n");
                        this.bufferedWriter.write(str.toString());
                        break;
                    }
                }
            }
            if (s != null && !s.isClosed() && s.isConnected()) {
                Response response = createResponse(a);
                response.writeDelimitedTo(s.getOutputStream());
                //	PrintWriter wr = new PrintWriter(new OutputStreamWriter(
                //			s.getOutputStream()), true);
                //	wr.println(str.toString());
            }
            bufferedWriter.flush();
        } catch (IOException e) {
            log("Io exception");
            log(e.getStackTrace().toString());
            e.printStackTrace();
        } catch (Exception e) {
            log("General Exception");
            log(e.getStackTrace().toString());
            e.printStackTrace();
        }
    }

    /**
     * @param numOfDays the numOfDays to set
     */
    public void setNumOfDays(int numOfDays) {
        this.numOfDays = numOfDays;
    }

    /**
     * @return the timePeriod
     */
    public int getTimePeriod() {
        return timePeriod;
    }

    /**
     * @param timePeriod the timePeriod to set
     */
    public void setTimePeriod(int timePeriod) {
        this.timePeriod = timePeriod;
    }
    
}
