
import com.cometway.ak.AgentRequest;
import com.cometway.net.HTTPLoader;
import com.cometway.props.Props;
import com.cometway.text.ITextBuffer;
import com.cometway.text.StringTextBuffer;
import com.cometway.text.TextFinder;
import com.cometway.text.TextPointer;
import com.cometway.text.TextRange;
import com.cometway.xml.XMLRequestAgent;
import java.io.*;
import java.net.*;
import java.util.*;


public class WeatherXMLRequestAgent extends XMLRequestAgent
{
	public void initProps()
	{
		setDefault("service_name", "/weather.agent");
		setDefault("submit_url", "http://printer.wunderground.com/cgi-bin/findweather/getForecast?query=");
        }


	public void handleRequest(AgentRequest request)
	{
		request.setContentType("text/plain");

		try
		{
			String  zip = request.getString("zip");

			println("Retrieving weather data for " + zip);

			String  html = getWeatherPage(zip);
//debug(html.substring(0,512));

			if ((html == null) || (html.length() == 0) || (html.indexOf("Error: Not Found") != -1))
			{
				error("Weather data for " + zip + " could not be found.");

				return;
			}

			println("Parsing weather data...");

			StringTextBuffer data = new StringTextBuffer(html);
			Props p = new Props();

			getField(data, "Observed at", p, "location");
			getField(data, "Temperature", p, "temperature");
			getField(data, "Humidity", p, "humidity");
			getField(data, "Dewpoint", p, "dewpoint");
			getField(data, "Pressure", p, "pressure");
			getField(data, "<td>Conditions", p, "conditions");
//p.dump();

			// Write the weather properties as XML

			writeXMLHeader(request);
			writeProps(request, p, "weather");
		}
		catch (Exception e)
		{
			error("Could not continue", e);
		}
	}


	/**
	 * This method submits a HTTP request to the weather service and retrieves
	 * a formatted HTML page containing the weather information for zip.
	 */

	String getWeatherPage(String zip)
	{
		String html;

		try
		{
			HTTPLoader loader = new HTTPLoader();
			loader.setRequestTimeout(120000);
                        loader.setUserAgent("Mozilla/4.0 (compatible; MSIE 5.12; Mac_PowerPC)");

			html = loader.getURL(getString("submit_url") + zip);
		}
		catch (Exception e)
		{
			error("Could not load weather page", e);

			html = null;
		}

		return (html);
	}


	void getField(ITextBuffer b, String label, Props p, String key)
	{
		TextRange r = getField(b, 0, label, "<b>", '<');

		if (r != null)
		{
			String s = r.toString().trim();
			int i = s.indexOf("&#176");

			if (i != -1)
			{
				s = s.substring(0, i);
			}

			p.setProperty(key, s);
		}
	}


        TextRange getField(ITextBuffer b, int fromIndex, String label, String beforeText, char endChar)
        {
                TextFinder f = new TextFinder(label);
                TextRange r = b.findText(fromIndex, f);

                if (r != null)
                {
                        f = new TextFinder(beforeText);
                        r = b.findText(r.getEnd(), f);

                        if (r != null)
                        {
                                TextPointer start = r.getStartPointer();
                                TextPointer end = r.getEndPointer();

                                start.setPosition(end);
                                end.findNext(endChar);
                        }
                }
                return (r);
        }
}


