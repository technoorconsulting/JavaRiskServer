package gft.api.example;

import technoor.datarequest.ProtoTradeApi;
import gft.api.Candle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import technoor.datarequest.ProtoTradeApi.CandleProto;
import technoor.datarequest.ProtoTradeApi.Response;

interface RequestOptions {

    public int getInstrumentId();

    public int getNumOfDays();

    /**
     * @param numOfDays the numOfDays to set
     */
    public void setNumOfDays(int numOfDays);

    /**
     * @return the timePeriod
     */
    public int getTimePeriod();

    /**
     * @param timePeriod the timePeriod to set
     */
    public void setTimePeriod(int timePeriod);
}


public interface IServiceUpdates extends RequestOptions{
	public void writeToFileArray(String title, Candle[] a, Candle[] ref,
			String fname);
	public int waitForConnection() throws IOException;

};

class ServiceUpdater implements IServiceUpdates {

	private BufferedWriter bufferedWriter;
	private ServerSocket sSocket;
	StringBuilder str;
	Socket s;// =ss.accept();
	private int instrumentId;
        private int numOfDays;
        private int timePeriod;

//	@ getter
	public int getInstrumentId()
	{
		return instrumentId;
	}

	public int getNumOfDays()
	{
		return 5;
	}
	private void log(String s) 
	{
		System.out.println(s);
	}

	public ServiceUpdater() throws IOException {
		sSocket = new ServerSocket(8801);

	}
/*
 * waits for a socket connection and returns the number of points requested.
 */
	public int waitForConnection() throws IOException {
		s = sSocket.accept();
		System.out.println("Client Accepted2");

		
		int numOfPeriods=-1;
		String read=("");
            try {
                ProtoTradeApi.RequestUpdate ru = ProtoTradeApi.RequestUpdate.parseDelimitedFrom(s.getInputStream());
                log("ID got" + ru.getId());
                instrumentId = ru.getId();
                log("numOfPeriods" + ru.getNumOfPeriods());
                numOfPeriods = ru.getNumOfPeriods();
                setNumOfDays(ru.getNumOfDays());
                setTimePeriod(ru.getTimeMin());
                
            } catch (Exception e) {
                e.printStackTrace();
                // for backward compatibility try to read the stream.
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            s.getInputStream()));

                    read = br.readLine();
                    numOfPeriods = Integer.parseInt(read);
                    log("Number read" + read);
                    read = br.readLine();
                    log("Number read" + read);
                    instrumentId = Integer.parseInt(read);


                }
		catch(NumberFormatException ne){
			System.err.println("Exception:"+ read);
			instrumentId=3;
                        ne.printStackTrace();
		}
               }
		//System.out.println(br.readLine());
		
		
		return numOfPeriods;

	}
        public Response createResponse(gft.api.Candle[] a){
        
            Response response = Response.getDefaultInstance();
            Response.Builder responseBuilder;
            responseBuilder = response.newBuilder();
            CandleProto candle = CandleProto.getDefaultInstance();
            CandleProto.Builder candleBuilder = candle.newBuilderForType();
            
            int i;
            for (i = 0; i < a.length; i++) {
                // assume it is unix timestamp so converting to excel readable number in terms of days
                // adding the uni timestart of Jan 1 1970
                candleBuilder.setTimestamp((a[i].getStartTimestamp()));
                candleBuilder.setHigh(a[i].getHigh());
                candleBuilder.setLow(a[i].getLow());
                candleBuilder.setOpen(a[i].getOpen());
                candleBuilder.setClose(a[i].getClose());

                responseBuilder.addDataCandle(candleBuilder.build());
            }

            return responseBuilder.build();     
            
        } 

    @Override
	public void writeToFileArray(String title, Candle[] a, Candle[] ref,
			String fname) {
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
						str.append("" + a[i].getStartTimestamp()
								+ "," + a[i].getHigh() + "," + a[i].getLow()
								+ "," + a[i].getOpen() + "," + a[i].getClose()
								+ "," + ref[j].getHigh() + ","
								+ ref[j].getLow() + "\r\n");
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

};
