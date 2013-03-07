package technoor.service;

import gft.api.Candle;
import java.io.IOException;


import technoor.service.RequestOptions;



public interface IServiceUpdater extends RequestOptions{
	public void writeArrayToOutput(String title, Candle[] a, Candle[] ref,
			String fname);
	public int waitForConnection() throws IOException;

};

;
