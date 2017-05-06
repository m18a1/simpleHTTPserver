package com.mwyatt001.simpleServer;
/*
 * @Author: Matt Wyatt
 * Naive attempt at a simple HTTP server that gets/sets ad campaigns from parameters in JSON requests
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONObject;

public class SimpleServer {
	static Map<String,Triple<String,Integer,Long>> ads = new HashMap<String,Triple<String,Integer,Long>>();
	//public SimpleServer() throws IOException{
	public static void main(String[] args) throws IOException{
		HttpServer server = HttpServer.create(new InetSocketAddress(8888), 0)	;
		server.createContext("/", new SimpleHandler());
		server.setExecutor(null);
		System.out.println("Server started");
		server.start();
	}
	
	static class SimpleHandler implements HttpHandler{

		@Override
		public void handle(HttpExchange t) throws IOException {
			System.out.println("Handler started");
			String path = t.getRequestURI().getPath();
			String[] pathSplit = path.split("/");
			int pSize = pathSplit.length;
			String method = t.getRequestMethod();
			System.out.println("method= " + method);
			String responseBodyStr = null;
			OutputStream os = t.getResponseBody();
			Triple vals = null;

			
			if (method.contains("POST")){
				BufferedReader streamReader = new BufferedReader(new InputStreamReader(t.getRequestBody(),"UTF-8"));
				StringBuilder reqStrBuilder = new StringBuilder();
				String s;
				while (( s =streamReader.readLine()) != null){
					reqStrBuilder.append(s);
				}
				JSONObject jsob = new JSONObject(reqStrBuilder.toString());
				System.out.println("reqStr= " + reqStrBuilder.toString());
				String part_id = jsob.getString("partner_id");
				String ad_con = jsob.getString("ad_content");
				int dur = jsob.getInt("duration");
				Triple<String,Integer,Long>trip = Triple.of(ad_con,dur,Calendar.getInstance().getTimeInMillis()/1000);
				
				//Weed out bad requests
				if (part_id.indexOf("/") > -1){
					responseBodyStr = "Invalid Partner ID";
					t.sendResponseHeaders(400, responseBodyStr.length());
					os.write(responseBodyStr.getBytes());
					os.close();
				}
				else if (ads.containsKey(part_id)) {
					vals = ads.get(part_id);
					long currTime = Calendar.getInstance().getTimeInMillis()/1000;
					Long m = Long.parseLong(vals.getMiddle().toString());
					Long r = Long.parseLong(vals.getRight().toString());
					long endTime = m + r;
					if (endTime>currTime){
						responseBodyStr = "Ad campaign exists for partner.";
						t.sendResponseHeaders(400, responseBodyStr.length());
						os.write(responseBodyStr.getBytes());
						os.close();
					}
					else{
						ads.put(part_id,trip);
						responseBodyStr = "Ad campaign added for partner " + part_id;
						t.sendResponseHeaders(200, responseBodyStr.length());
						os.write(responseBodyStr.getBytes());
						os.close();
					}
				}
				else{
					ads.put(part_id,trip);
					responseBodyStr = "Ad campaign added for partner " + part_id;
					t.sendResponseHeaders(200, responseBodyStr.length());
					os.write(responseBodyStr.getBytes());
					os.close();
				}
			}
			else if (method.contains("GET")){
				//determine if get all or get partner
				if (pSize > 2){ //contains partner id
					String part_id =pathSplit[2];
					//check if valid partner id
					if (ads.containsKey(part_id)){	
						vals = ads.get(part_id);
						System.out.println(part_id + " and vals " + vals.toString());
					}
					else{
						System.out.println("partner not found");
						responseBodyStr = "Partner not found.";
						t.sendResponseHeaders(404, responseBodyStr.length());
						os.write(responseBodyStr.getBytes());
						os.close();
					}
					//check if ad is expired
					long currTime = Calendar.getInstance().getTimeInMillis()/1000;
					Long m = Long.parseLong(vals.getMiddle().toString());
					Long r = Long.parseLong(vals.getRight().toString());
					long endTime = m + r;
					if (endTime <= currTime){
						System.out.println("ad expired");
						responseBodyStr = "No active campaigns for partner: " + part_id;
						t.sendResponseHeaders(404,responseBodyStr.length());
						os.write(responseBodyStr.getBytes());
						os.close();
					}
					else{
//						System.out.println("building response");
						responseBodyStr = "Partner ID: " + part_id + "\nAd Content: " + vals.getLeft().toString();
						t.sendResponseHeaders(200,responseBodyStr.length());
						os = t.getResponseBody();
						os. write(responseBodyStr.getBytes());
						os.close();
					}
				}
				else {
//					System.out.println("getting all ads");
					JSONArray jarr = new JSONArray();
					for (Entry<String, Triple<String, Integer, Long>> me : ads.entrySet()){
						vals = me.getValue();
						JSONObject jsob = new JSONObject();
						jsob.put("ad_content", vals.getLeft().toString());
						jsob.put("duration", vals.getMiddle());
						jsob.put("partner_id", me.getKey().toString());
						jarr.put(jsob);
					}
					t.sendResponseHeaders(200,jarr.toString().length() );
					os.write(jarr.toString().getBytes());
					os.close();
				}
			}
			else{
				System.out.println("command not recognized");
				responseBodyStr = "Command not recognized.";
				t.sendResponseHeaders(400, responseBodyStr.length());
				os.write(responseBodyStr.getBytes());
				os.close();
			}
		}	
	}
}
