
import com.cometway.ak.*;
import com.cometway.httpd.*;
import com.cometway.props.Props;
import com.cometway.util.*;


public class FormDemoPage extends RequestAgent
{
	public void initProps()
	{
		setDefault("service_name", "/formdemo.agent");
	}


	public void handleRequest(AgentRequest request)
	{
		Props p = request.getProps();

		p.setDefault("title", "Comet Way");
		p.setDefault("heading_text", "Comet Way Agents");
		p.setDefault("body_text", "Comet Way's agent based programming model is perfect for creating embedded applications that are lightweight, modular, and robust.");
		p.setDefault("bg_color", "FFFFFF");
		p.setDefault("body_align", "LEFT");
		p.setDefault("font_color", "FF0000");
		p.setDefault("font_face", "Helvetica");
		p.setDefault("font_size", "3");

		String title = p.getString("title");
		String bg_color = p.getString("bg_color");
		String font_color = p.getString("font_color");
		String font_face = p.getString("font_face");
		String font_size = p.getString("font_size");
		String heading_text = p.getString("heading_text");
		String body_text = p.getString("body_text");
		String body_align = p.getString("body_align");

		request.println("<HTML>\n<HEAD>");
		request.println("<TITLE>" + title + "</TITLE>");
		request.println("</HEAD>");

		request.println("<BODY BGCOLOR='" + bg_color + "'>");
		request.println("<FONT COLOR='" + font_color + "' SIZE='" + font_size + "' FACE='" + font_face + "'>");
		request.println("<H1>" + heading_text + "</H1>");
		request.println("<P ALIGN='" + body_align + "'>" + body_text + "</P>");
		request.println("</FONT>");

		request.println("<HR>");

		request.println("<FORM METHOD='GET' ACTION='" + getString("service_name") + "'>");
		request.println("<TABLE>");
		request.println("<TR><TH COLSPAN='2'>This page was rendered by an agent using these parameters:</TH></TR>");
		request.println("<TR><TD COLSPAN='2'>&nbsp;</TD></TR>");
		request.println("<TR><TD ALIGN='RIGHT'>Title:</TD><TD><INPUT NAME='title' VALUE='" + title + "'></TD></TR>");
		request.println("<TR><TD ALIGN='RIGHT'>Background Color:</TD><TD><INPUT NAME='bg_color' VALUE='" + bg_color + "'></TD></TR>");
		request.println("<TR><TD ALIGN='RIGHT'>Font Color:</TD><TD><INPUT NAME='font_color' VALUE='" + font_color + "'></TD></TR>");

		request.println("<TR><TD ALIGN='RIGHT'>Font Face:</TD><TD><SELECT NAME='font_face'>");
		request.println("<OPTION" + (font_face.equals("Arial") ? " SELECTED" : "") + ">Arial</OPTION>");
		request.println("<OPTION" + (font_face.equals("Courier New") ? " SELECTED" : "") + ">Courier New</OPTION>");
		request.println("<OPTION" + (font_face.equals("Helvetica") ? " SELECTED" : "") + ">Helvetica</OPTION>");
		request.println("<OPTION" + (font_face.equals("Times") ? " SELECTED" : "") + ">Times</OPTION>");
		request.println("<OPTION" + (font_face.equals("Verdana") ? " SELECTED" : "") + ">Verdana</OPTION>");
		request.println("</SELECT></TD></TR>");

		request.println("<TR><TD ALIGN='RIGHT'>Font Size:</TD><TD><SELECT NAME='font_size'>");
		request.println("<OPTION" + (font_size.equals("1") ? " SELECTED" : "") + ">1</OPTION>");
		request.println("<OPTION" + (font_size.equals("2") ? " SELECTED" : "") + ">2</OPTION>");
		request.println("<OPTION" + (font_size.equals("3") ? " SELECTED" : "") + ">3</OPTION>");
		request.println("<OPTION" + (font_size.equals("4") ? " SELECTED" : "") + ">4</OPTION>");
		request.println("<OPTION" + (font_size.equals("5") ? " SELECTED" : "") + ">5</OPTION>");
		request.println("</SELECT></TD></TR>");

		request.println("<TR><TD ALIGN='RIGHT'>Heading Text:</TD><TD><INPUT NAME='heading_text' VALUE='" + heading_text + "' SIZE='60'></TD></TR>");
		request.println("<TR><TD ALIGN='RIGHT'>Body Text:</TD><TD><TEXTAREA NAME='body_text' COLS='60' ROWS='4'>" + body_text + "</TEXTAREA></TD></TR>");
		request.println("<TR><TD ALIGN='RIGHT'>Body Align:</TD><TD>");
		request.println("<INPUT NAME='body_align' TYPE='RADIO' VALUE='LEFT'" + (body_align.equals("LEFT") ? " CHECKED" : "") + ">Left");
		request.println("<INPUT NAME='body_align' TYPE='RADIO' VALUE='CENTER'" + (body_align.equals("CENTER") ? " CHECKED" : "") + ">Center");
		request.println("<INPUT NAME='body_align' TYPE='RADIO' VALUE='RIGHT'" + (body_align.equals("RIGHT") ? " CHECKED" : "") + ">Right");
		request.println("</TD></TR>");

		request.println("<TR><TD COLSPAN='2'>&nbsp;</TD></TR>");
		request.println("<TR><TD ALIGN='CENTER' COLSPAN='2'><INPUT type=submit value='Submit Form'></TD></TR>");
		request.println("</TABLE>");
		request.println("</FORM>");
		request.println("<HR>");
		request.println("<P ALIGN='CENTER'>Powered by <A HREF='http://www.cometway.com'>Comet Way</A> Agents</P>");

		request.println("</BODY>\n</HTML>");
	}
}



